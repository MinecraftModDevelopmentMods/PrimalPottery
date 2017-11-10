package net.modcrafters.primalpottery.pottery

import net.minecraft.util.text.TextFormatting

enum class PotteryType(val label: String, val color: TextFormatting) {
    LUMP("Just a lump of clay", TextFormatting.DARK_RED),
    LUMP_NO_BOTTOM("A pipe?", TextFormatting.DARK_RED),
    LUMP_THIN("Too thin!", TextFormatting.DARK_RED),
    POT("A pot!", TextFormatting.DARK_AQUA),
    BOWL("A bowl!", TextFormatting.DARK_AQUA)
}
