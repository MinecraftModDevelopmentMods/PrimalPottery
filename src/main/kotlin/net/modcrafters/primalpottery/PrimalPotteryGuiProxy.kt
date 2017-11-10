package net.modcrafters.primalpottery

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.IGuiHandler
import net.modcrafters.primalpottery.template.TemplateContainer
import net.modcrafters.primalpottery.template.TemplateContainerGui

object PrimalPotteryGuiProxy: IGuiHandler {
    override fun getClientGuiElement(ID: Int, player: EntityPlayer?, world: World?, x: Int, y: Int, z: Int): Any? = when (ID) {
        MOD_GUI_TEMPLATE -> TemplateContainerGui(TemplateContainer())
        else -> null
    }

    override fun getServerGuiElement(ID: Int, player: EntityPlayer?, world: World?, x: Int, y: Int, z: Int): Any? = when (ID) {
        MOD_GUI_TEMPLATE -> TemplateContainer()
        else -> null
    }
}
