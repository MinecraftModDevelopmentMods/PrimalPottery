package net.modcrafters.minecraft.primalpottery.network;

import java.util.ArrayList;
import java.util.List;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.modcrafters.minecraft.primalpottery.Ids;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryDisc;

/** The sculpted profile, sent to the server so the template item keeps it. */
public record TemplatePayload(boolean mainHand, List<PotteryDisc> discs) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TemplatePayload> TYPE = new CustomPacketPayload.Type<>(Ids.of("template"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TemplatePayload> STREAM_CODEC = StreamCodec.of(
            TemplatePayload::write,
            TemplatePayload::read
    );

    private static void write(RegistryFriendlyByteBuf buf, TemplatePayload payload) {
        buf.writeBoolean(payload.mainHand);
        buf.writeVarInt(payload.discs.size());
        for (PotteryDisc disc : payload.discs) {
            buf.writeDouble(disc.height());
            buf.writeDouble(disc.radius());
            buf.writeDouble(disc.innerRadius());
        }
    }

    private static TemplatePayload read(RegistryFriendlyByteBuf buf) {
        boolean mainHand = buf.readBoolean();
        int count = buf.readVarInt();
        if (count < 0 || count > 64) {
            throw new DecoderException("template");
        }
        List<PotteryDisc> discs = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            discs.add(new PotteryDisc(buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }
        return new TemplatePayload(mainHand, discs);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
