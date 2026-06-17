package MengySmod.vhm.event;

import MengySmod.vhm.treadmill.TreadmillBlockEntity;
import MengySmod.vhm.treadmill.TreadmillMount;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class TreadmillEvents {
    private TreadmillEvents() {}

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (entity.level().isClientSide) {
            return;
        }

        if (entity instanceof Player player) {
            TreadmillBlockEntity mounted = TreadmillMount.getMountedTreadmill(player);
            if (mounted != null) {
                if (player.isShiftKeyDown()) {
                    mounted.dismount(player);
                } else {
                    mounted.handleMountedPlayer(player);
                }
                return;
            }
            if (TreadmillMount.isMounted(player)) {
                TreadmillMount.dismount(player);
            }
            return;
        }

        if (entity instanceof Villager villager) {
            TreadmillMount.tickBreadBoost(villager);
            TreadmillMount.tickReleaseCooldown(villager);
            if (TreadmillMount.getReleaseCooldownTicks(villager) > 0) {
                return;
            }
            BlockPos mountedPos = TreadmillMount.getMountedPos(villager);
            if (mountedPos != null) {
                TreadmillBlockEntity mounted = TreadmillBlockEntity.at(entity.level(), mountedPos);
                if (mounted != null) {
                    if (mounted.hasMountedPlayer()) {
                        TreadmillMount.release(villager);
                        return;
                    }
                    mounted.handleBoundVillager(villager);
                    return;
                }
                TreadmillMount.dismount(villager);
            }

            TreadmillBlockEntity treadmill = findNearbyTreadmill(villager);
            if (treadmill != null && !treadmill.hasBoundVillager() && !treadmill.hasMountedPlayer()) {
                TreadmillMount.mount(villager, treadmill.getBlockPos());
                treadmill.handleBoundVillager(villager);
            }
        }
    }

    private static TreadmillBlockEntity findNearbyTreadmill(Villager villager) {
        BlockPos origin = villager.blockPosition();
        TreadmillBlockEntity closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos candidatePos = origin.offset(x, y, z);
                    TreadmillBlockEntity candidate = TreadmillBlockEntity.at(villager.level(), candidatePos);
                    if (candidate == null || !candidate.isEntityNearBelt(villager, TreadmillBlockEntity.VILLAGER_AUTO_MOUNT_RADIUS)) {
                        continue;
                    }

                    double distance = candidate.getBlockPos().distSqr(villager.blockPosition());
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closest = candidate;
                    }
                }
            }
        }

        return closest;
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getTarget() instanceof Villager villager)) {
            return;
        }
        TreadmillBlockEntity treadmill = TreadmillBlockEntity.at(event.getLevel(), villager.blockPosition());
        if (treadmill == null) {
            treadmill = TreadmillBlockEntity.at(event.getLevel(), villager.blockPosition().below());
        }
        if (treadmill == null) {
            return;
        }
        if (event.getItemStack().is(Items.BREAD)) {
            if (treadmill.supportsEntity(villager)) {   // 是否站在跑步机上
                // 面包增益只记录到村民自己身上，这样下机或重进后都会保留。
                TreadmillMount.grantBreadBoost(villager, 12000);   // 1s = 20 ticks
            }
            return;
        }
        if (treadmill.isMountedVillager(villager)) {
            treadmill.ejectBoundVillager();
            event.setCancellationResult(InteractionResult.sidedSuccess(false));
            event.setCanceled(true);
        }
    }
}
