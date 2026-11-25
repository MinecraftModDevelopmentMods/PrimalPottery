package net.modcrafters.minecraft.primalpottery.pottery;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum PotteryType {
    LUMP("Just a lump of clay", ChatFormatting.DARK_RED),
    LUMP_NO_BOTTOM("A pipe?", ChatFormatting.DARK_RED),
    LUMP_THIN("Too thin!", ChatFormatting.DARK_RED),
    POT("A pot!", ChatFormatting.DARK_AQUA),
    JAR("A jar!", ChatFormatting.DARK_AQUA),
    JUG("A jug!", ChatFormatting.DARK_AQUA);

    private final String label;
    private final ChatFormatting color;

    PotteryType(String label, ChatFormatting color) {
        this.label = label;
        this.color = color;
    }

    public String label() {
        return this.label;
    }

    public ChatFormatting color() {
        return this.color;
    }

    public boolean isVessel() {
        return this == POT || this == JAR || this == JUG;
    }

    public Component tooltipName() {
        PotteryType type = this.isVessel() ? this : LUMP;
        return Component.translatable("pottery.primalpottery.type." + type.name().toLowerCase(Locale.ROOT));
    }
}
