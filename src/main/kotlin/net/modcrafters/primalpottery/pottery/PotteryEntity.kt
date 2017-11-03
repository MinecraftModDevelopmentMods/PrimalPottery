package net.modcrafters.primalpottery.pottery

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import net.modcrafters.primalpottery.PrimalPotteryMod
import net.modcrafters.primalpottery.to_tcl.readList
import net.modcrafters.primalpottery.to_tcl.writeList
import net.ndrei.teslacorelib.blocks.multipart.IBlockPart
import net.ndrei.teslacorelib.blocks.multipart.IBlockPartHitBox
import net.ndrei.teslacorelib.blocks.multipart.IBlockPartProvider
import net.ndrei.teslacorelib.inventory.SyncProviderLevel
import net.ndrei.teslacorelib.tileentities.SyncTileEntity
import java.util.function.Consumer
import java.util.function.Supplier

class PotteryEntity : SyncTileEntity(), IBlockPartProvider {
    private val pottery = listOf(
        PotteryPart(EnumFacing.AxisDirection.NEGATIVE, EnumFacing.AxisDirection.NEGATIVE),
        PotteryPart(EnumFacing.AxisDirection.POSITIVE, EnumFacing.AxisDirection.NEGATIVE),
        PotteryPart(EnumFacing.AxisDirection.POSITIVE, EnumFacing.AxisDirection.POSITIVE),
        PotteryPart(EnumFacing.AxisDirection.NEGATIVE, EnumFacing.AxisDirection.POSITIVE)
    )

    init {
        PrimalPotteryMod.logger.info("POTTERY ENTITY CREATED!")

        this.registerSyncTagPart("pottery_parts",
            Consumer { it.readList("pp", this.pottery) },
            Supplier { NBTTagCompound().also { it.writeList("pp", this.pottery) } },
            SyncProviderLevel.TICK)
    }

    override fun getParts() = this.pottery

    override fun onPartActivated(player: EntityPlayer, hand: EnumHand, part: IBlockPart, hitBox: IBlockPartHitBox): Boolean {
        val stack = player.getHeldItem(hand)
        if ((part is PotteryPart) && !this.getWorld().isRemote && part.canBeHitWith(this.getWorld(), this.pos, this.getWorld().getBlockState(this.pos), player, stack)) {
            if (part.hit(this, stack, player)) {
                this.refreshBlock()
                return true
            }
        }

        return true // false
    }

    @SideOnly(Side.CLIENT)
    override fun onSyncPartSynced(key: String) {
        this.getWorld().markBlockRangeForRenderUpdate(this.pos, this.pos)
    }

    private fun refreshBlock() {
        PrimalPotteryMod.logger.info("POTTERY ENTITY :: refresh")

        if (this.pottery.all { it.pottery == null }) {
            this.getWorld().setBlockToAir(this.pos)
        }
        else {
            this.partialSync("pottery_parts", true)
            if (this.getWorld().isRemote) {
                this.getWorld().markBlockRangeForRenderUpdate(this.pos, this.pos)
            }
        }
    }

    fun addPottery(pottery: Pottery, hitX: Float = 0.0f, hitZ: Float = 0.0f): Boolean {
        val index = if (hitX < .5f) {
            if (hitZ < .5f) 0
            else 3
        } else if (hitZ < .5) 1
        else 2

        val empty = this.pottery[index].let { if (it.pottery == null) it else this.pottery.firstOrNull { it.pottery == null } }
        empty?.pottery = pottery
        this.refreshBlock()
        return (empty != null)
    }
}
