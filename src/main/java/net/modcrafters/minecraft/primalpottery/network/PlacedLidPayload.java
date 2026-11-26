package net.modcrafters.minecraft.primalpottery.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.modcrafters.minecraft.primalpottery.Ids;

/** A lid put on or taken off one pot in a placed pottery block. */
public record PlacedLidPayload(BlockPos pos, int slot, boolean closed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlacedLidPayload> TYPE = new CustomPacketPayload.Type<>(Ids.of("placed_lid"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlacedLidPayload> STREAM_CODEC = StreamCodec.of(
            PlacedLidPayload::write,
            PlacedLidPayload::read
    );

    private static void write(RegistryFriendlyByteBuf buf, PlacedLidPayload payload) {
        buf.writeBlockPos(payload.pos);
        buf.writeVarInt(payload.slot);
        buf.writeBoolean(payload.closed);
    }

    private static PlacedLidPayload read(RegistryFriendlyByteBuf buf) {
        return new PlacedLidPayload(buf.readBlockPos(), buf.readVarInt(), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
