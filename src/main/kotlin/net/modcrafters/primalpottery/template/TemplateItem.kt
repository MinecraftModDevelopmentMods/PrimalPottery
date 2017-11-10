package net.modcrafters.primalpottery.template

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.modcrafters.primalpottery.MOD_GUI_TEMPLATE
import net.modcrafters.primalpottery.MOD_ID
import net.modcrafters.primalpottery.PrimalPotteryMod
import net.ndrei.teslacorelib.annotations.AutoRegisterItem
import net.ndrei.teslacorelib.items.RegisteredItem

@AutoRegisterItem
object TemplateItem : RegisteredItem(MOD_ID, PrimalPotteryMod.creativeTab, "pottery_template") {
//    override fun onItemUse(player: EntityPlayer, worldIn: World, pos: BlockPos, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): EnumActionResult {
//        if (!worldIn.isRemote) {
//            player.openGui(PrimalPotteryMod, MOD_GUI_TEMPLATE, worldIn, pos.x, pos.y, pos.z)
//        }
//        return EnumActionResult.SUCCESS
//    }

    override fun onItemUseFirst(player: EntityPlayer, world: World, pos: BlockPos?, side: EnumFacing?, hitX: Float, hitY: Float, hitZ: Float, hand: EnumHand): EnumActionResult {
        if (world.isRemote) {
            player.openGui(PrimalPotteryMod, MOD_GUI_TEMPLATE, world, 0, 0, 0) // pos.x, pos.y, pos.z)
        }
        return EnumActionResult.SUCCESS
    }
}