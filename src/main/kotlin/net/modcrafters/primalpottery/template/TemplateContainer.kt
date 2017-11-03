package net.modcrafters.primalpottery.template

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container

class TemplateContainer : Container() {
    override fun canInteractWith(playerIn: EntityPlayer?) = true
}