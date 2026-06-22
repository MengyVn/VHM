package MengySmod.vhm.treadmill;

import MengySmod.vhm.VhmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VillagerFoodFollowHelper {
    private static final double FOLLOW_RANGE = 8.0D;
    private static final double FOLLOW_RANGE_SQR = FOLLOW_RANGE * FOLLOW_RANGE;
    private static final double FOLLOW_SPEED = 0.9D;
    private static final double TARGET_SWITCH_BUFFER_SQR = 4.0D;
    private static final int PATH_REFRESH_INTERVAL = 6;
    private static final Map<UUID, FollowState> STATES = new HashMap<>();

    private VillagerFoodFollowHelper() {}

    public static boolean tick(Villager villager) {
        if (villager.level().isClientSide) {
            return false;
        }
        if (TreadmillMount.getMountedPos(villager) != null || isNearTreadmill(villager)) {
            stopFollowing(villager);
            return false;
        }

        FollowState state = STATES.computeIfAbsent(villager.getUUID(), ignored -> new FollowState());
        TargetCandidate best = findClosestAttractedPlayer(villager);
        Player target = resolveTarget(villager, state, best);
        if (target == null) {
            stopFollowing(villager);
            return false;
        }

        villager.setNoAi(false);
        villager.setSprinting(true);
        villager.getLookControl().setLookAt(target, 10f, 10f);

        long gameTime = villager.level().getGameTime();
        if (!target.getUUID().equals(state.lastTargetId) || gameTime - state.lastPathTick >= PATH_REFRESH_INTERVAL) {
            villager.getNavigation().moveTo(target, FOLLOW_SPEED);
            state.lastPathTick = gameTime;
            state.lastTargetId = target.getUUID();
        }
        state.targetId = target.getUUID();
        state.lastSeenTick = gameTime;
        return true;
    }

    private static TargetCandidate findClosestAttractedPlayer(Villager villager) {
        AABB scanBox = villager.getBoundingBox().inflate(FOLLOW_RANGE);
        TargetCandidate closest = null;
        double closestDistance = FOLLOW_RANGE_SQR;

        for (ServerPlayer player : villager.level().getEntitiesOfClass(ServerPlayer.class, scanBox, VillagerFoodFollowHelper::isHoldingAttractiveFood)) {
            double distance = villager.distanceToSqr(player);
            if (distance > FOLLOW_RANGE_SQR) {
                continue;
            }
            if (closest == null || distance < closestDistance) {
                closest = new TargetCandidate(player, distance);
                closestDistance = distance;
            }
        }

        return closest;
    }

    private static Player resolveTarget(Villager villager, FollowState state, TargetCandidate best) {
        Level level = villager.level();
        Player locked = state.targetId == null ? null : level.getPlayerByUUID(state.targetId);
        if (locked == null || !isValidTarget(locked) || villager.distanceToSqr(locked) > FOLLOW_RANGE_SQR) {
            return best == null ? null : best.player();
        }

        if (best == null) {
            return locked;
        }
        if (best.player() == locked) {
            return locked;
        }

        double lockedDistance = villager.distanceToSqr(locked);
        if (best.distanceSqr() + TARGET_SWITCH_BUFFER_SQR < lockedDistance) {
            return best.player();
        }
        return locked;
    }

    private static boolean isValidTarget(Player player) {
        return player instanceof ServerPlayer && player.isAlive() && !player.isSpectator() && isHoldingAttractiveFood(player);
    }

    public static boolean isHoldingAttractiveFood(Player player) {
        return isAttractiveFood(player.getMainHandItem()) || isAttractiveFood(player.getOffhandItem());
    }

    private static boolean isNearTreadmill(Villager villager) {
        BlockPos origin = villager.blockPosition();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos candidatePos = origin.offset(x, y, z);
                    TreadmillBlockEntity treadmill = TreadmillBlockEntity.at(villager.level(), candidatePos);
                    if (treadmill != null && treadmill.isEntityNearBelt(villager, TreadmillBlockEntity.VILLAGER_AUTO_MOUNT_RADIUS)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void stopFollowing(Villager villager) {
        STATES.remove(villager.getUUID());
        villager.setSprinting(false);
        villager.getNavigation().stop();
    }

    private static boolean isAttractiveFood(ItemStack stack) {
        return stack.is(VhmItems.SPRITE_SIP.get()) || stack.is(VhmItems.CHOCO_LIZ.get());
    }

    private record TargetCandidate(Player player, double distanceSqr) {}

    private static final class FollowState {
        private UUID targetId;
        private UUID lastTargetId;
        private long lastSeenTick;
        private long lastPathTick;
    }
}
