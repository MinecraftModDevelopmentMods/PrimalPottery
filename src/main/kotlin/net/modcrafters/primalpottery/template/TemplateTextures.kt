package net.modcrafters.primalpottery.template

import net.minecraft.util.ResourceLocation
import net.modcrafters.primalpottery.MOD_ID
import net.ndrei.teslacorelib.gui.IGuiTexture

enum class TemplateTextures(path: String): IGuiTexture {
    MAIN("textures/gui/main.png");

    override val resource = ResourceLocation(MOD_ID, path)
}
