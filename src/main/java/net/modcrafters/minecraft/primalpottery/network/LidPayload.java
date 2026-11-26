package net.modcrafters.minecraft.primalpottery.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.modcrafters.minecraft.primalpottery.Ids;

/** A lid put on or taken off the pottery item in one hand. */
public record LidPayload(boolean mainHand, boolean closed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LidPayload> TYPE = new CustomPacketPayload.Type<>(Ids.of("lid"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LidPayload> STREAM_CODEC = StreamCodec.of(
            LidPayload::write,
            LidPayload::read
    );

    private static void write(RegistryFriendlyByteBuf buf, LidPayload payload) {
        buf.writeBoolean(payload.mainHand);
        buf.writeBoolean(payload.closed);
    }

    private static LidPayload read(RegistryFriendlyByteBuf buf) {
        return new LidPayload(buf.readBoolean(), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
