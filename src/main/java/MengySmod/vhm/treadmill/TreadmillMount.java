package MengySmod.vhm.treadmill;

import MengySmod.vhm.network.VhmNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class TreadmillMount {
    private static final String TAG = "VhmTreadmillPos";
    private static final String BREAD_BOOST_TAG = "VhmTreadmillBreadBoost";
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
        CompoundTag data = villager.getPersistentData();
        // 面包增益要记录在村民自己身上，这样下机或退出重进后都不会丢
        data.putInt(BREAD_BOOST_TAG, Math.max(getBreadBoostTicks(villager), ticks));
    }

    public static int getBreadBoostTicks(Villager villager) {
        CompoundTag data = villager.getPersistentData();
        return data.contains(BREAD_BOOST_TAG) ? data.getInt(BREAD_BOOST_TAG) : 0;
    }

    public static void setBreadBoostTicks(Villager villager, int ticks) {
        CompoundTag data = villager.getPersistentData();
        if (ticks > 0) {
            data.putInt(BREAD_BOOST_TAG, ticks);
        } else {
            data.remove(BREAD_BOOST_TAG);
        }
    }

    public static void tickBreadBoost(Villager villager) {
        int ticks = getBreadBoostTicks(villager);
        if (ticks > 0) {
            setBreadBoostTicks(villager, ticks - 1);
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
