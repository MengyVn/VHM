package MengySmod.vhm.event;

import MengySmod.vhm.VhmItems;
import MengySmod.vhm.VhmSounds;
import MengySmod.vhm.treadmill.TreadmillBlockEntity;
import MengySmod.vhm.treadmill.TreadmillMount;
import MengySmod.vhm.treadmill.VillagerFoodFollowHelper;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class TreadmillEvents {
    private static final int AUTO_BREAD_BOOST_CAP = 12000;
    private static final int AUTO_DRINK_BOOST_CAP = 18000;
    private static final int AUTO_SNACK_BOOST_CAP = 18000;

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
            TreadmillMount.tickBoosts(villager);
            TreadmillMount.tickReleaseCooldown(villager);
            BlockPos mountedPos = TreadmillMount.getMountedPos(villager);
            if (mountedPos != null) {
                TreadmillBlockEntity mounted = TreadmillBlockEntity.at(entity.level(), mountedPos);
                if (mounted != null) {
                    if (mounted.hasMountedPlayer()) {
                        TreadmillMount.release(villager);
                    } else {
                        mounted.handleBoundVillager(villager);
                        return;
                    }
                }
                TreadmillMount.dismount(villager);
            }

            TreadmillBlockEntity treadmill = findNearbyTreadmill(villager);
            if (treadmill != null) {
                if (treadmill.isMountedVillager(villager)) {
                    treadmill.handleBoundVillager(villager);
                    return;
                }

                if ((treadmill.hasBoundVillager() || treadmill.hasMountedPlayer()) && treadmill.isEntityOnBelt(villager)) {
                    treadmill.repelForeignVillager(villager);
                    return;
                }

                if (TreadmillMount.getReleaseCooldownTicks(villager) <= 0 && !treadmill.hasBoundVillager() && !treadmill.hasMountedPlayer()) {
                    TreadmillMount.mount(villager, treadmill.getBlockPos());
                    treadmill.handleBoundVillager(villager);
                    return;
                }
            }

            if (VillagerFoodFollowHelper.tick(villager)) {
                return;
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
        boolean automated = player instanceof DeployerFakePlayer;
        if (automated && event.getItemStack().isEmpty()) {
            cancel(event);
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
                if (automated) {
                    if (TreadmillMount.getBreadBoostTicks(villager) >= AUTO_BREAD_BOOST_CAP) {
                        cancel(event);
                        return;
                    }
                    TreadmillMount.grantBreadBoost(villager, 12000);
                    consume(player, event.getItemStack());
                    cancel(event);
                } else {
                    // 面包增益只记录到村民自己身上，这样下机或重进后都会保留。
                    TreadmillMount.grantBreadBoost(villager, 12000);   // 1s = 20 ticks
                }
            }
            return;
        }
        if (event.getItemStack().is(VhmItems.SPRITE_SIP.get())) {
            if (treadmill.supportsEntity(villager)) {
                if (automated) {
                    if (TreadmillMount.getDrinkBoostTicks(villager) >= AUTO_DRINK_BOOST_CAP) {
                        cancel(event);
                        return;
                    }
                    TreadmillMount.grantDrinkBoost(villager, 18000);
                } else {
                    TreadmillMount.grantDrinkBoost(villager, 18000);
                }
                event.getLevel().playSound(null, villager.blockPosition(), VhmSounds.TREADMILL_VILLAGER_FEED.get(), SoundSource.NEUTRAL, 0.9f, 1.05f);
                consume(player, event.getItemStack());
                cancel(event);
            }
            return;
        }
        if (event.getItemStack().is(VhmItems.CHOCO_LIZ.get())) {
            if (treadmill.supportsEntity(villager)) {
                if (automated) {
                    if (TreadmillMount.getSnackBoostTicks(villager) >= AUTO_SNACK_BOOST_CAP) {
                        cancel(event);
                        return;
                    }
                    TreadmillMount.grantSnackBoost(villager, 18000);
                } else {
                    TreadmillMount.grantSnackBoost(villager, 18000);
                }
                event.getLevel().playSound(null, villager.blockPosition(), VhmSounds.TREADMILL_VILLAGER_FEED.get(), SoundSource.NEUTRAL, 0.9f, 0.95f);
                consume(player, event.getItemStack());
                cancel(event);
            }
            return;
        }
        if (event.getItemStack().is(VhmItems.CLEANSING_BRUSH.get())) {
            TreadmillMount.clearAccelerationBoosts(villager);
            for (MobEffectInstance effect : villager.getActiveEffects()) {
                villager.removeEffect(effect.getEffect());
            }
            consume(player, event.getItemStack());
            cancel(event);
            return;
        }
        if (treadmill.isMountedVillager(villager)) {
            if (automated) {
                event.setCancellationResult(InteractionResult.sidedSuccess(false));
                event.setCanceled(true);
                return;
            }
            treadmill.ejectBoundVillager();
            event.setCancellationResult(InteractionResult.sidedSuccess(false));
            event.setCanceled(true);
        }
    }

    private static void consume(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private static void cancel(PlayerInteractEvent.EntityInteract event) {
        event.setCancellationResult(InteractionResult.sidedSuccess(false));
        event.setCanceled(true);
    }
}
