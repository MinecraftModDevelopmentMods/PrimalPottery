package net.modcrafters.primalpottery.pottery

import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.world.World
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.modcrafters.primalpottery.to_tcl.readObject
import net.ndrei.teslacorelib.annotations.AutoRegisterItem
import net.ndrei.teslacorelib.utils.getHeldItem

@AutoRegisterItem
@Mod.EventBusSubscriber
object PotteryItem: ItemBlock(PotteryBlock) {
    init {
        this.registryName = PotteryBlock.registryName
    }

    override fun canPlaceBlockOnSide(worldIn: World?, pos: BlockPos?, side: EnumFacing?, player: EntityPlayer?, stack: ItemStack?) =
        (side == EnumFacing.UP)

    override fun rayTrace(worldIn: World?, playerIn: EntityPlayer?, useLiquids: Boolean): RayTraceResult {
        return super.rayTrace(worldIn, playerIn, useLiquids)
    }

    override fun placeBlockAt(stack: ItemStack, player: EntityPlayer, world: World, pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, newState: IBlockState): Boolean {
        if (super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState)) {
            if (!world.isRemote) {
                val tile= world.getTileEntity(pos) as? PotteryEntity
                if (tile != null) {
                    val pottery = stack.tagCompound.readObject("pottery", { Pottery() })
                    if (pottery != null) {
                        tile.addPottery(pottery, hitX - pos.x, hitZ - pos.z)
                    }
                }
            }
            return true
        }
        return false
    }

    @JvmStatic
    @SubscribeEvent
    fun onBlockHighlightEvent(ev: DrawBlockHighlightEvent) {
        if((ev.target != null) && (ev.target.typeOfHit == RayTraceResult.Type.BLOCK)) {
            val stack = ev.player.getHeldItem()
            if (!stack.isEmpty && (stack.item === PotteryItem) && (ev.target.sideHit == EnumFacing.UP)) {
                val x= if ((ev.target.hitVec.x - ev.target.blockPos.x) < 0.5) 0.0 else 0.5
                val z = if ((ev.target.hitVec.z - ev.target.blockPos.z) < 0.5) 0.0 else 0.5

                GlStateManager.enableBlend()
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO)
                GlStateManager.glLineWidth(2.0f)
                GlStateManager.disableTexture2D()
                GlStateManager.depthMask(false)

                RenderGlobal.drawBoundingBox(
                    ev.target.blockPos.x - ev.player.posX + x + PotteryPart.OFFSET / 32.0,
                    ev.target.blockPos.y - ev.player.posY + 1.0,
                    ev.target.blockPos.z - ev.player.posZ + z + PotteryPart.OFFSET / 32.0,
                    ev.target.blockPos.x - ev.player.posX + x + .5 - PotteryPart.OFFSET / 16.0,
                    ev.target.blockPos.y - ev.player.posY + 1.0 + PotteryPart.SIZE / 32.0,
                    ev.target.blockPos.z - ev.player.posZ + z + .5 - PotteryPart.OFFSET / 16.0
                , 0.0f, 0.0f, 0.0f, 0.42f)

                GlStateManager.depthMask(true)
                GlStateManager.enableTexture2D()
                GlStateManager.disableBlend()

                ev.isCanceled = true
            }
        }
    }
}