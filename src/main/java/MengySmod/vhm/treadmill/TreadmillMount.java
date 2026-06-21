package MengySmod.vhm.treadmill;

import MengySmod.vhm.VhmEffects;
import MengySmod.vhm.VhmSounds;
import MengySmod.vhm.network.VhmNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;

public final class TreadmillMount {
    private static final String TAG = "VhmTreadmillPos";
    public static final String BREAD_BOOST_TAG = "VhmTreadmillBreadBoost";
    public static final String DRINK_BOOST_TAG = "VhmTreadmillDrinkBoost";
    public static final String SNACK_BOOST_TAG = "VhmTreadmillSnackBoost";
    private static final String RELEASE_COOLDOWN_TAG = "VhmTreadmillReleaseCooldown";
    private static BlockPos clientMountedPos;

    private TreadmillMount() {}

    public static void mount(Player player, BlockPos pos) {
        player.getPersistentData().putLong(TAG, pos.asLong());
        if (player instanceof ServerPlayer serverPlayer) {
            VhmNetwork.syncPlayerTreadmillState(serverPlayer, true, pos);
        }
    }

    public static void mount(LivingEntity entity, BlockPos pos) {
        entity.getPersistentData().putLong(TAG, pos.asLong());
        if (!entity.level().isClientSide() && entity instanceof Villager villager) {
            villager.level().playSound(null, villager.blockPosition(), VhmSounds.TREADMILL_VILLAGER_MOUNT.get(), SoundSource.NEUTRAL, 0.9f, 1.0f);
        }
    }

    public static void dismount(Player player) {
        player.getPersistentData().remove(TAG);
        if (player instanceof ServerPlayer serverPlayer) {
            VhmNetwork.syncPlayerTreadmillState(serverPlayer, false, BlockPos.ZERO);
        }
    }

    public static void dismount(LivingEntity entity) {
        entity.getPersistentData().remove(TAG);
    }

    public static void grantBreadBoost(Villager villager, int ticks) {
        addBoostTicks(villager, BREAD_BOOST_TAG, ticks);
        syncBoostEffects(villager);
    }

    public static int getBreadBoostTicks(Villager villager) {
        return getBoostTicks(villager, BREAD_BOOST_TAG);
    }

    public static void setBreadBoostTicks(Villager villager, int ticks) {
        setBoostTicks(villager, BREAD_BOOST_TAG, ticks);
        syncBoostEffects(villager);
    }

    public static void tickBreadBoost(Villager villager) {
        tickBoosts(villager);
    }

    public static void grantDrinkBoost(Villager villager, int ticks) {
        addBoostTicks(villager, DRINK_BOOST_TAG, ticks);
        syncBoostEffects(villager);
    }

    public static int getDrinkBoostTicks(Villager villager) {
        return getBoostTicks(villager, DRINK_BOOST_TAG);
    }

    public static void setDrinkBoostTicks(Villager villager, int ticks) {
        setBoostTicks(villager, DRINK_BOOST_TAG, ticks);
        syncBoostEffects(villager);
    }

    public static void tickDrinkBoost(Villager villager) {
        tickBoosts(villager);
    }

    public static void grantSnackBoost(Villager villager, int ticks) {
        addBoostTicks(villager, SNACK_BOOST_TAG, ticks);
        syncBoostEffects(villager);
    }

    public static int getSnackBoostTicks(Villager villager) {
        return getBoostTicks(villager, SNACK_BOOST_TAG);
    }

    public static void setSnackBoostTicks(Villager villager, int ticks) {
        setBoostTicks(villager, SNACK_BOOST_TAG, ticks);
        syncBoostEffects(villager);
    }

    public static void tickSnackBoost(Villager villager) {
        tickBoosts(villager);
    }

    public static void clearAccelerationBoosts(Villager villager) {
        setBreadBoostTicks(villager, 0);
        setDrinkBoostTicks(villager, 0);
        setSnackBoostTicks(villager, 0);
    }

    public static boolean isTrackedBoostEffect(MobEffect effect) {
        return effect == VhmEffects.TREADMILL_BREAD_BOOST.get()
            || effect == VhmEffects.TREADMILL_DRINK_BOOST.get()
            || effect == VhmEffects.TREADMILL_SNACK_BOOST.get();
    }

    public static void grantReleaseCooldown(Villager villager, int ticks) {
        CompoundTag data = villager.getPersistentData();
        data.putInt(RELEASE_COOLDOWN_TAG, Math.max(getReleaseCooldownTicks(villager), ticks));
    }

    public static int getReleaseCooldownTicks(Villager villager) {
        CompoundTag data = villager.getPersistentData();
        return data.contains(RELEASE_COOLDOWN_TAG) ? data.getInt(RELEASE_COOLDOWN_TAG) : 0;
    }

    public static void setReleaseCooldownTicks(Villager villager, int ticks) {
        CompoundTag data = villager.getPersistentData();
        if (ticks > 0) {
            data.putInt(RELEASE_COOLDOWN_TAG, ticks);
        } else {
            data.remove(RELEASE_COOLDOWN_TAG);
        }
    }

    public static void tickReleaseCooldown(Villager villager) {
        int ticks = getReleaseCooldownTicks(villager);
        if (ticks > 0) {
            setReleaseCooldownTicks(villager, ticks - 1);
        }
    }

    public static void tickBoosts(Villager villager) {
        int breadTicks = getBreadBoostTicks(villager);
        int drinkTicks = getDrinkBoostTicks(villager);
        int snackTicks = getSnackBoostTicks(villager);

        if (drinkTicks > 0 || snackTicks > 0) {
            if (drinkTicks > 0 && snackTicks > 0) {
                if (drinkTicks <= snackTicks) {
                    drinkTicks--;
                } else {
                    snackTicks--;
                }
            } else if (drinkTicks > 0) {
                drinkTicks--;
            } else {
                snackTicks--;
            }
        } else if (breadTicks > 0) {
            breadTicks--;
        }

        setBoostTicks(villager, BREAD_BOOST_TAG, breadTicks);
        setBoostTicks(villager, DRINK_BOOST_TAG, drinkTicks);
        setBoostTicks(villager, SNACK_BOOST_TAG, snackTicks);
        syncBoostEffects(villager);
    }

    private static void addBoostTicks(Villager villager, String tag, int ticks) {
        if (ticks <= 0) {
            return;
        }
        setBoostTicks(villager, tag, getBoostTicks(villager, tag) + ticks);
    }

    private static int getBoostTicks(Villager villager, String tag) {
        CompoundTag data = villager.getPersistentData();
        return data.contains(tag) ? data.getInt(tag) : 0;
    }

    private static void setBoostTicks(Villager villager, String tag, int ticks) {
        CompoundTag data = villager.getPersistentData();
        if (ticks > 0) {
            data.putInt(tag, ticks);
        } else {
            data.remove(tag);
        }
    }

    private static void syncBoostEffects(Villager villager) {
        syncEffect(villager, VhmEffects.TREADMILL_BREAD_BOOST, getBreadBoostTicks(villager));
        syncEffect(villager, VhmEffects.TREADMILL_DRINK_BOOST, getDrinkBoostTicks(villager));
        syncEffect(villager, VhmEffects.TREADMILL_SNACK_BOOST, getSnackBoostTicks(villager));
    }

    private static void syncEffect(Villager villager, Holder<MobEffect> effect, int ticks) {
        if (ticks > 0) {
            villager.addEffect(new MobEffectInstance(effect, ticks, 0, false, true, true));
        } else {
            villager.removeEffect(effect);
        }
    }

    public static boolean isMounted(Player player) {
        if (player.level().isClientSide()) {
            return clientMountedPos != null;
        }
        return player.getPersistentData().contains(TAG);
    }

    public static boolean isMounted(LivingEntity entity) {
        if (entity.level().isClientSide() && entity instanceof Player) {
            return clientMountedPos != null;
        }
        return entity.getPersistentData().contains(TAG);
    }

    public static BlockPos getMountedPos(Player player) {
        if (player.level().isClientSide()) {
            return clientMountedPos;
        }
        return getMountedPos((LivingEntity) player);
    }

    public static BlockPos getMountedPos(LivingEntity entity) {
        if (entity.level().isClientSide() && entity instanceof Player) {
            return clientMountedPos;
        }
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(TAG)) {
            return null;
        }
        return BlockPos.of(data.getLong(TAG));
    }

    public static TreadmillBlockEntity getMountedTreadmill(Player player) {
        BlockPos pos = getMountedPos(player);
        if (pos == null) {
            return null;
        }
        Level level = player.level();
        if (level.getBlockEntity(pos) instanceof TreadmillBlockEntity treadmill) {
            return treadmill;
        }
        return null;
    }

    public static void release(LivingEntity entity) {
        entity.getPersistentData().remove(TAG);
        entity.setNoGravity(false);
        entity.noPhysics = false;

        if (entity instanceof ServerPlayer serverPlayer) {
            VhmNetwork.syncPlayerTreadmillState(serverPlayer, false, BlockPos.ZERO);
        }

        if (entity instanceof Player player) {
            player.setForcedPose(null);
            player.setJumping(false);
            player.setSwimming(false);
            player.setSprinting(false);
            player.refreshDimensions();
        } else if (entity instanceof Villager villager) {
            villager.setNoAi(false);
            grantReleaseCooldown(villager, 10);
        }
    }

    public static void setClientMountedPos(BlockPos pos) {
        clientMountedPos = pos;
    }

    public static void clearClientMountedPos() {
        clientMountedPos = null;
    }

    public static BlockPos getClientMountedPos() {
        return clientMountedPos;
    }
}
