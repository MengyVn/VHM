package MengySmod.vhm.client;

import MengySmod.vhm.Vhm;
import MengySmod.vhm.network.VhmNetwork;
import MengySmod.vhm.treadmill.TreadmillMount;
import MengySmod.vhm.treadmill.TreadmillBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Vhm.MODID, value = Dist.CLIENT)
public class ClientAnimHandler {
    private ClientAnimHandler() {}

    @SubscribeEvent
    public static void onClientEntityTick(EntityTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Player player) {
            clampPlayerView(player);
            BlockPos mountedPos = TreadmillMount.getMountedPos(player);
            if (mountedPos != null) {
                Minecraft minecraft = Minecraft.getInstance();
                // 玩家站在跑步机上时，手动推进 walkAnimation，补上脚部摆动逻辑。
                updateMountedPlayerWalkAnimation(player, minecraft.options.keyUp.isDown());
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new VhmNetwork.PlayerTreadmillStatePacket(
                        mountedPos,
                        minecraft.options.keyUp.isDown(),
                        player.isSprinting()
                    )
                );
            }
            return;
        }

        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }

        TreadmillBlockEntity treadmill = findTreadmill(villager.level(), villager);
        if (treadmill == null || !treadmill.isEntityOnBelt(villager)) {
            return;
        }
        // TODO：玩家站上机器根据实施输入渲染腿部动画 [已完成]
        float beltMultiplier = treadmill.getBeltSpeedMultiplier();
        float baseRpm = TreadmillBlockEntity.BASE_RPM;
        float effectiveRpm = baseRpm * beltMultiplier;

        float swingAmount = Math.min(effectiveRpm * 0.08f, 1.4f);
        float stepSpeed = effectiveRpm * 0.04f;
        villager.walkAnimation.setSpeed(swingAmount);
        villager.walkAnimation.update(stepSpeed, 0.8f);
    }

    private static void updateMountedPlayerWalkAnimation(Player player, boolean movingForward) {
        if (!movingForward) {
            player.walkAnimation.setSpeed(0f);
            player.walkAnimation.update(0f, 0.8f);
            return;
        }

        float stageMultiplier = player.isSprinting() ? 4f : 1f;
        float effectiveRpm = TreadmillBlockEntity.BASE_RPM * stageMultiplier;

        float swingAmount = Math.min(effectiveRpm * 0.08f, 1.4f);
        float stepSpeed = effectiveRpm * 0.04f;
        player.walkAnimation.setSpeed(swingAmount);
        player.walkAnimation.update(stepSpeed, 0.8f);
    }

    private static void clampPlayerView(Player player) {
        if (TreadmillMount.getMountedPos(player) == null) {
            player.noPhysics = false;
            player.setNoGravity(false);
            player.setForcedPose(null);
            player.setSwimming(false);
            player.setJumping(false);
            player.setYBodyRot(player.getYRot());
            return;
        }

        TreadmillBlockEntity treadmill = TreadmillMount.getMountedTreadmill(player);
        if (treadmill == null) {
            return;
        }

        float facingYaw = TreadmillBlockEntity.modelYRotation(
            treadmill.getBlockState().getValue(MengySmod.vhm.treadmill.TreadmillBlock.HORIZONTAL_FACING)
        );
        float playerFacingYaw = facingYaw + 180f;
        float clampedYaw = playerFacingYaw + Mth.clamp(Mth.wrapDegrees(player.getYRot() - playerFacingYaw), -90f, 90f);
        player.setYRot(clampedYaw);
        player.setYHeadRot(clampedYaw);
        player.setYBodyRot(playerFacingYaw);
    }

    private static TreadmillBlockEntity findTreadmill(Level level, LivingEntity entity) {
        TreadmillBlockEntity treadmill = TreadmillBlockEntity.at(level, entity.blockPosition());
        if (treadmill == null) {
            treadmill = TreadmillBlockEntity.at(level, entity.blockPosition().below());
        }
        return treadmill;
    }
}
