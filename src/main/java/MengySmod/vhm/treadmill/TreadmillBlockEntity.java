package MengySmod.vhm.treadmill;

import MengySmod.vhm.Vhm;
import MengySmod.vhm.VhmBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.List;

import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

public class TreadmillBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final float MAX_STRESS_OUTPUT = 4096f;
    public static final float BASE_RPM = 8f;
    public static final float BASE_CAPACITY = 64f;

    private static final double BELT_MIN = 2 / 16.0;
    private static final double BELT_MAX = 14 / 16.0;
    private static final double BELT_TOP = 4 / 16.0;
    private static final double STAND_ON_BELT = BELT_TOP + 0.02;
    public static final double VILLAGER_AUTO_MOUNT_RADIUS = 1.2;
    public static final double VILLAGER_RELEASE_DISTANCE = 1.8;

    private float stressMultiplier = 1f;
    private float beltSpeedMultiplier = 0f;
    private boolean manualMode;
    private float stressCap = MAX_STRESS_OUTPUT;
    private boolean runnerPresent;
    private boolean playerMovingForward;
    private boolean playerSprinting;

    public TreadmillBlockEntity(BlockPos pos, BlockState state) {
        super(VhmBlockEntities.TREADMILL.get(), pos, state);
    }

    public static float modelYRotation(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180;
            case EAST -> 90;
            case WEST -> 270;
            default -> 0;
        };
    }

    public Axis getRotationAxis() {
        return getBlockState().getValue(TreadmillBlock.HORIZONTAL_FACING).getClockWise().getAxis();
    }

    public float getLocalGeneratedSpeed() {
        try {
            if (!runnerPresent || level == null) {
                return 0;
            }
            if (countCascadeUnits() > TreadmillNetworkHelper.MAX_CASCADE) {
                LOGGER.debug("[Treadmill BE {}] Cascade limit exceeded", worldPosition);
                return 0;
            }
            Direction facing = getBlockState().getValue(TreadmillBlock.HORIZONTAL_FACING);
            float speed = convertToDirection(BASE_RPM, facing);
            LOGGER.trace("[Treadmill BE {}] Generated speed: {}", worldPosition, speed);
            return speed;
        } catch (Exception e) {
            LOGGER.error("[Treadmill BE {}] Error in getLocalGeneratedSpeed", worldPosition, e);
            return 0;
        }
    }

    public float getBeltSpeedMultiplier() {
        return beltSpeedMultiplier;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        float theoreticalSpeed = Math.abs(getTheoreticalSpeed());
        if (Mth.equal(theoreticalSpeed, 0)) {
            return false;
        }

        CreateLang.translate("gui.goggles.generator_stats")
            .forGoggles(tooltip);
        CreateLang.translate("tooltip.capacityProvided")
            .style(ChatFormatting.GRAY)
            .forGoggles(tooltip);

        float stressTotal = theoreticalSpeed * BASE_CAPACITY * beltSpeedMultiplier;
        CreateLang.number(stressTotal)
            .translate("generic.unit.stress")
            .style(ChatFormatting.AQUA)
            .space()
            .add(CreateLang.translate("gui.goggles.at_current_speed")
                .style(ChatFormatting.DARK_GRAY))
            .forGoggles(tooltip, 1);

        return true;
    }

    public float getLocalStressCapacity() {
        try {
            if (!runnerPresent) {
                return 0;
            }
            return BASE_CAPACITY * stressMultiplier;
        } catch (Exception e) {
            LOGGER.error("[Treadmill BE {}] Error in getLocalStressCapacity", worldPosition, e);
            return 0;
        }
    }

    @Override
    public float getGeneratedSpeed() {
        try {
            if (level == null || level.isClientSide) {
                return super.getGeneratedSpeed();
            }
            return getLocalGeneratedSpeed();
        } catch (Exception e) {
            LOGGER.error("[Treadmill BE {}] Error in getGeneratedSpeed", worldPosition, e);
            return 0;
        }
    }

    @Override
    public float calculateAddedStressCapacity() {
        try {
            if (level == null || level.isClientSide) {
                return lastCapacityProvided;
            }
            if (!runnerPresent || getLocalGeneratedSpeed() == 0) {
                lastCapacityProvided = 0;
                return 0;
            }
            var cascade = TreadmillNetworkHelper.collectCascade(level, worldPosition, getRotationAxis());
            float networkRpm = TreadmillNetworkHelper.networkMaxRpm(cascade);
            if (networkRpm == 0) {
                lastCapacityProvided = 0;
                return 0;
            }
            float localCap = getLocalStressCapacity();
            float rawTotal = TreadmillNetworkHelper.networkTotalCapacity(cascade, networkRpm);
            float cappedTotal = TreadmillNetworkHelper.applyManualCap(cascade, rawTotal);
            if (rawTotal > 0 && cappedTotal < rawTotal) {
                localCap *= cappedTotal / rawTotal;
            }
            lastCapacityProvided = localCap;
            LOGGER.trace("[Treadmill BE {}] Stress capacity: local={}, rawTotal={}, cappedTotal={}", 
                worldPosition, localCap, rawTotal, cappedTotal);
            return localCap;
        } catch (Exception e) {
            LOGGER.error("[Treadmill BE {}] Error in calculateAddedStressCapacity", worldPosition, e);
            lastCapacityProvided = 0;
            return 0;
        }
    }

    @Override
    public void tick() {
        try {
            super.tick();
            if (level == null || level.isClientSide) {
                return;
            }
            refreshRunner();
        } catch (Exception e) {
            LOGGER.error("[Treadmill BE {}] Error in tick", worldPosition, e);
        }
    }

    public boolean tryMount(Player player) {
        if (level == null || level.isClientSide) {
            return false;
        }
        if (TreadmillMount.isMounted(player)) {
            return false;
        }
        ejectBoundVillagers();
        TreadmillMount.mount(player, worldPosition);
        lockOnBelt(player);
        refreshRunner();
        setChanged();
        return true;
    }

    public void dismount(Player player) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!isMountedHere(player)) {
            return;
        }
        TreadmillMount.dismount(player);
        Direction facing = getBlockState().getValue(TreadmillBlock.HORIZONTAL_FACING);
        Vec3 exit = beltCenter().add(facing.getStepX() * 0.65, 0.05, facing.getStepZ() * 0.65);
        player.teleportTo(exit.x, exit.y, exit.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.noPhysics = false;
        player.setNoGravity(false);
        player.setSprinting(false);
        player.setJumping(false);
        player.setSwimming(false);
        player.setForcedPose(null);
        player.refreshDimensions();
        refreshRunner();
        setChanged();
    }

    public boolean isMountedHere(Player player) {
        return worldPosition.equals(TreadmillMount.getMountedPos(player));
    }

    public void handleMountedPlayer(Player player) {
        if (!isMountedHere(player)) {
            return;
        }
        lockOnBelt(player);
    }

    public void setPlayerMotionState(Player player, boolean movingForward, boolean sprinting) {
        if (!isMountedHere(player)) {
            return;
        }
        playerMovingForward = movingForward;
        playerSprinting = sprinting;
        refreshRunner();
    }

    public void handleEntity(LivingEntity entity) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (entity instanceof Player player) {
            if (isMountedHere(player)) {
                handleMountedPlayer(player);
            }
            return;
        }
        if (hasMountedPlayer()) {
            return;
        }
        if (entity instanceof Villager villager && isEntityNearBelt(villager, VILLAGER_AUTO_MOUNT_RADIUS)) {
            if (hasBoundVillager() && !isMountedVillager(villager)) {
                return;
            }
            TreadmillMount.mount(villager, worldPosition);
            lockOnBelt(villager);
        }
    }

    public void handleBoundVillager(Villager villager) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (hasMountedPlayer()) {
            TreadmillMount.release(villager);
            return;
        }
        if (!worldPosition.equals(TreadmillMount.getMountedPos(villager))) {
            TreadmillMount.release(villager);
            return;
        }
        if (isEntityOnBelt(villager) || isEntityNearBelt(villager, VILLAGER_AUTO_MOUNT_RADIUS)) {
            lockOnBelt(villager);
        }
    }

    public boolean hasBoundVillager() {
        if (level == null) {
            return false;
        }
        AABB scanBox = beltBounds().inflate(1.25, 1.0, 1.25);
        return !level.getEntitiesOfClass(Villager.class, scanBox, villager -> worldPosition.equals(TreadmillMount.getMountedPos(villager))).isEmpty();
    }

    public boolean hasMountedPlayer() {
        if (level == null) {
            return false;
        }
        return level.players().stream().anyMatch(this::isMountedHere);
    }

    public void ejectBoundVillagers() {
        if (level == null) {
            return;
        }
        while (ejectBoundVillager()) {
            // Keep clearing until no mounted villager remains.
        }
    }

    public boolean ejectBoundVillager() {
        if (level == null) {
            return false;
        }
        Direction facing = getBlockState().getValue(TreadmillBlock.HORIZONTAL_FACING);
        Direction releaseDirection = facing.getOpposite();
        Vec3 exit = beltCenter().add(
            releaseDirection.getStepX() * VILLAGER_RELEASE_DISTANCE,
            0.05,
            releaseDirection.getStepZ() * VILLAGER_RELEASE_DISTANCE
        );
        AABB scanBox = beltBounds().inflate(1.25, 1.0, 1.25);
        for (Villager villager : level.getEntitiesOfClass(Villager.class, scanBox, this::isMountedVillager)) {
            TreadmillMount.release(villager);
            villager.teleportTo(exit.x, exit.y, exit.z);
            villager.setDeltaMovement(Vec3.ZERO);
            villager.hurtMarked = true;
            villager.setYRot(releaseDirection.toYRot());
            villager.setYHeadRot(releaseDirection.toYRot());
            return true;
        }
        return false;
    }

    public boolean isMountedVillager(Villager villager) {
        return worldPosition.equals(TreadmillMount.getMountedPos(villager));
    }

    private void refreshRunner() {
        // 修复：状态变化时会 setChanged() 并向客户端发 block update，确保 beltSpeedMultiplier 真正刷新到客户端
        if (level == null) {
            return;
        }

        boolean hadRunner = runnerPresent;
        float previousBeltSpeedMultiplier = beltSpeedMultiplier;
        float previousStressMultiplier = stressMultiplier;
        runnerPresent = false;
        beltSpeedMultiplier = 0f;
        stressMultiplier = 1f;

        for (Player player : level.players()) {
            if (isMountedHere(player) && playerMovingForward) {
                runnerPresent = true;
                float stageMultiplier = playerSprinting ? 4f : 1f;
                beltSpeedMultiplier = Math.max(beltSpeedMultiplier, stageMultiplier);
                stressMultiplier = Math.max(stressMultiplier, stageMultiplier);
                break;
            }
        }

        if (!runnerPresent) {
            AABB scanBox = beltBounds().inflate(0.05, 0.25, 0.05);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, scanBox, LivingEntity::isAlive)) {
                if (!isEntityOnBelt(entity)) {
                    continue;
                }
                if (entity instanceof Villager villager) {
                    runnerPresent = true;
                    boolean breadBoosted = TreadmillMount.getBreadBoostTicks(villager) > 0;
                    boolean scared = isVillagerScared(villager);
                    // 村民上机后默认就应当作为跑者参与发电，否则会出现“村民被固定住但跑步机不转”的情况
                    // beltSpeedMultiplier = 1f -> 0f 连锁反应，导致村民不会默认参与发电
                    float stageMultiplier = breadBoosted && scared ? 8f : (breadBoosted || scared ? 4f : 1f);
                    beltSpeedMultiplier = stageMultiplier;
                    stressMultiplier = stageMultiplier;
                    break;
                }
            }
        }

        if (hadRunner != runnerPresent || previousBeltSpeedMultiplier != beltSpeedMultiplier || previousStressMultiplier != stressMultiplier) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            updateGeneratedRotation();
        }
    }
    // 根据实体的位置变化量（deltaMovement）自动计算腿部摆动
    private void lockOnBelt(LivingEntity entity) {
        Direction facing = getBlockState().getValue(TreadmillBlock.HORIZONTAL_FACING);
        Direction entityFacing = entity instanceof Player ? facing.getOpposite() : facing;
        Vec3 center = beltCenter();
        double standY = worldPosition.getY() + STAND_ON_BELT;
        // FIXME：每 tick 传送 + 速度清零 → 原版认为玩家没有移动 → 腿部没有摆动动画
        entity.teleportTo(center.x, standY, center.z);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setYRot(entityFacing.toYRot());
        entity.setYHeadRot(entityFacing.toYRot());
        entity.setXRot(0);
        entity.fallDistance = 0;
        entity.hurtMarked = true;
        entity.setOnGround(true);
        entity.setNoGravity(true);
        if (entity instanceof Player player) {
            player.setJumping(false);
            player.setSwimming(false);
            player.setForcedPose(Pose.STANDING);
            player.setNoGravity(true);
            player.noPhysics = false;
        } else if (entity instanceof Villager villager) {
            villager.setNoAi(true);
            villager.noPhysics = true;
            villager.setNoGravity(true);
        }
    }

    public boolean isEntityNearBelt(LivingEntity entity, double horizontalRadius) {
        Vec3 beltCenter = beltCenter();
        Vec3 entityCenter = entity.getBoundingBox().getCenter();
        double dx = entityCenter.x - beltCenter.x;
        double dz = entityCenter.z - beltCenter.z;
        if (dx * dx + dz * dz > horizontalRadius * horizontalRadius) {
            return false;
        }

        AABB belt = beltBounds();
        AABB entityBox = entity.getBoundingBox();
        return entityBox.maxY > belt.minY - 0.6 && entityBox.minY < belt.maxY + 0.6;
    }

    private Vec3 beltCenter() {
        AABB belt = beltBounds();
        return new Vec3(belt.getCenter().x, belt.minY, belt.getCenter().z);
    }

    public static TreadmillBlockEntity at(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TreadmillBlockEntity treadmill) {
            return treadmill;
        }
        return null;
    }

    public void grantBreadBoost(int ticks) {
        // 兼容旧调用：实际增益现在写入村民自身数据，避免下机或退出后丢失
        if (level == null) {
            return;
        }
        AABB scanBox = beltBounds().inflate(1.25, 1.0, 1.25);
        for (Villager villager : level.getEntitiesOfClass(Villager.class, scanBox, this::isMountedVillager)) {
            TreadmillMount.grantBreadBoost(villager, ticks);
        }
    }

    public boolean supportsEntity(LivingEntity entity) {
        return isEntityOnBelt(entity);
    }

    public boolean isManualMode() {
        return manualMode;
    }

    public float getStressCap() {
        return stressCap;
    }

    public void toggleManualMode() {
        manualMode = !manualMode;
        updateGeneratedRotation();
        setChanged();
    }

    public void adjustStressCap(float delta) {
        stressCap = Math.max(64f, Math.min(MAX_STRESS_OUTPUT * TreadmillNetworkHelper.MAX_CASCADE, stressCap + delta));
        updateGeneratedRotation();
        setChanged();
    }

    public AABB beltBounds() {
        Direction facing = getBlockState().getValue(TreadmillBlock.HORIZONTAL_FACING);
        double[][] corners = {
            {BELT_MIN, BELT_MIN},
            {BELT_MIN, 1.0},
            {BELT_MAX, BELT_MIN},
            {BELT_MAX, 1.0}
        };
        double minX = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (double[] corner : corners) {
            double[] world = localToWorldXZ(corner[0], corner[1], facing);
            minX = Math.min(minX, world[0]);
            minZ = Math.min(minZ, world[1]);
            maxX = Math.max(maxX, world[0]);
            maxZ = Math.max(maxZ, world[1]);
        }
        return new AABB(
            worldPosition.getX() + minX,
            worldPosition.getY(),
            worldPosition.getZ() + minZ,
            worldPosition.getX() + maxX,
            worldPosition.getY() + BELT_TOP + 0.05,
            worldPosition.getZ() + maxZ
        );
    }

    public boolean isEntityOnBelt(LivingEntity entity) {
        if (entity instanceof Player player && isMountedHere(player)) {
            return isAboveBelt(entity.getBoundingBox());
        }
        AABB entityBox = entity.getBoundingBox();
        return isAboveBelt(entityBox) && entityBox.minY <= worldPosition.getY() + BELT_TOP + 0.35;
    }

    private boolean isAboveBelt(AABB entityBox) {
        AABB belt = beltBounds();
        return entityBox.maxX > belt.minX
            && entityBox.minX < belt.maxX
            && entityBox.maxZ > belt.minZ
            && entityBox.minZ < belt.maxZ;
    }

    private static double[] localToWorldXZ(double localX, double localZ, Direction facing) {
        return switch (facing) {
            case SOUTH -> new double[] {1.0 - localX, 1.0 - localZ};
            case EAST -> new double[] {localZ, 1.0 - localX};
            case WEST -> new double[] {1.0 - localZ, localX};
            default -> new double[] {localX, localZ};
        };
    }

    private int countCascadeUnits() {
        return TreadmillNetworkHelper.countCascade(level, worldPosition, getRotationAxis());
    }

    private static boolean isVillagerScared(Villager villager) {
        AABB scareBox = villager.getBoundingBox().inflate(8);
        return !villager.level().getEntitiesOfClass(LivingEntity.class, scareBox,
            e -> e instanceof Enemy && e.isAlive() && villager.hasLineOfSight(e)).isEmpty();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("ManualMode", manualMode);
        tag.putFloat("StressCap", stressCap);
        tag.putBoolean("RunnerPresent", runnerPresent);
        tag.putBoolean("PlayerMovingForward", playerMovingForward);
        tag.putBoolean("PlayerSprinting", playerSprinting);
        tag.putFloat("BeltSpeedMultiplier", beltSpeedMultiplier);
        tag.putFloat("StressMultiplier", stressMultiplier);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        manualMode = tag.getBoolean("ManualMode");
        stressCap = tag.contains("StressCap") ? tag.getFloat("StressCap") : MAX_STRESS_OUTPUT;
        runnerPresent = tag.getBoolean("RunnerPresent");
        playerMovingForward = tag.getBoolean("PlayerMovingForward");
        playerSprinting = tag.getBoolean("PlayerSprinting");
        if (tag.contains("BeltSpeedMultiplier")) {
            beltSpeedMultiplier = tag.getFloat("BeltSpeedMultiplier");
        }
        if (tag.contains("StressMultiplier")) {
            stressMultiplier = tag.getFloat("StressMultiplier");
        }
    }
}
