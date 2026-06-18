package MengySmod.vhm.network;

import MengySmod.vhm.Vhm;
import MengySmod.vhm.treadmill.TreadmillBlockEntity;
import MengySmod.vhm.treadmill.TreadmillMount;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class VhmNetwork {
    private static final String VERSION = "1";

    private VhmNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(VERSION)
            .playToServer(PlayerTreadmillStatePacket.TYPE, PlayerTreadmillStatePacket.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> payload.handle((ServerPlayer) context.player())))
            .playToClient(PlayerTreadmillSyncPacket.TYPE, PlayerTreadmillSyncPacket.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> payload.handle()));
    }

    public static void syncPlayerTreadmillState(ServerPlayer player, boolean mounted, BlockPos pos) {
        PacketDistributor.sendToPlayer(player, new PlayerTreadmillSyncPacket(mounted, pos));
    }

    public static final class PlayerTreadmillStatePacket implements CustomPacketPayload {
        public static final Type<PlayerTreadmillStatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Vhm.MODID, "player_treadmill_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerTreadmillStatePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            PlayerTreadmillStatePacket::pos,
            ByteBufCodecs.BOOL,
            PlayerTreadmillStatePacket::movingForward,
            ByteBufCodecs.BOOL,
            PlayerTreadmillStatePacket::sprinting,
            PlayerTreadmillStatePacket::new
        );

        private final BlockPos pos;
        private final boolean movingForward;
        private final boolean sprinting;

        public PlayerTreadmillStatePacket(BlockPos pos, boolean movingForward, boolean sprinting) {
            this.pos = pos;
            this.movingForward = movingForward;
            this.sprinting = sprinting;
        }

        public BlockPos pos() {
            return pos;
        }

        public boolean movingForward() {
            return movingForward;
        }

        public boolean sprinting() {
            return sprinting;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private void handle(ServerPlayer player) {
            if (!TreadmillMount.isMounted(player)) {
                return;
            }
            BlockPos mountedPos = TreadmillMount.getMountedPos(player);
            if (mountedPos == null || !mountedPos.equals(pos)) {
                return;
            }
            if (player.level().getBlockEntity(mountedPos) instanceof TreadmillBlockEntity treadmill) {
                treadmill.setPlayerMotionState(player, movingForward, sprinting);
            }
        }
    }

    public static final class PlayerTreadmillSyncPacket implements CustomPacketPayload {
        public static final Type<PlayerTreadmillSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Vhm.MODID, "player_treadmill_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerTreadmillSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            PlayerTreadmillSyncPacket::mounted,
            BlockPos.STREAM_CODEC,
            PlayerTreadmillSyncPacket::pos,
            PlayerTreadmillSyncPacket::new
        );

        private final boolean mounted;
        private final BlockPos pos;

        public PlayerTreadmillSyncPacket(boolean mounted, BlockPos pos) {
            this.mounted = mounted;
            this.pos = pos;
        }

        public boolean mounted() {
            return mounted;
        }

        public BlockPos pos() {
            return pos;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private void handle() {
            if (mounted) {
                TreadmillMount.setClientMountedPos(pos);
            } else {
                TreadmillMount.clearClientMountedPos();
            }
        }
    }
}
