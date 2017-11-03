package net.modcrafters.primalpottery.pottery

import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.util.INBTSerializable
import net.modcrafters.primalpottery.to_tcl.readObject
import net.modcrafters.primalpottery.to_tcl.writeObject
import net.ndrei.teslacorelib.blocks.multipart.BlockPart
import net.ndrei.teslacorelib.blocks.multipart.BlockPartHitBox
import net.ndrei.teslacorelib.blocks.multipart.IBlockPartHitBox

class PotteryPart(val xAlign: EnumFacing.AxisDirection, val zAlign: EnumFacing.AxisDirection) : BlockPart(), INBTSerializable<NBTTagCompound> {
    private val hitBox: IBlockPartHitBox
    var pottery: Pottery? = null

    init {
        this.hitBox = BlockPartHitBox.big32Sized(
            if (this.xAlign == EnumFacing.AxisDirection.NEGATIVE) OFFSET else (32.0 - OFFSET - SIZE),
            0.0,
            if (this.zAlign == EnumFacing.AxisDirection.NEGATIVE) OFFSET else (32.0 - OFFSET - SIZE),
            SIZE, SIZE, SIZE)
    }

    override fun canBeHitWith(world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer?, stack: ItemStack) =
        (this.pottery != null) || ((player != null) && this.isItemValid(world, pos, state, player, stack))

    override val hitBoxes get() = listOf(this.hitBox)

    override fun isItemValid(world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, stack: ItemStack) =
        (stack.isEmpty && this.pottery != null)
            ||
            (!stack.isEmpty && (this.pottery == null) && stack.hasTagCompound() && (stack.tagCompound?.hasKey("pottery") == true) && (stack.tagCompound.readObject("pottery", { Pottery() }) != null))

    fun hit(tile: PotteryEntity, stack: ItemStack, entity: Entity?): Boolean {
        val pottery = stack.tagCompound.readObject("pottery", { Pottery() })
        if ((this.pottery == null) && (pottery != null)) {
            val blockState = Blocks.BLACK_GLAZED_TERRACOTTA.defaultState
            val soundtype = blockState.block.getSoundType(blockState, tile.world, tile.pos, null)
            tile.world.playSound(null, tile.pos, soundtype.placeSound, SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0f) / 2.0f, soundtype.getPitch() * 0.8f)

            this.pottery = pottery
            return true
        }
        else if ((this.pottery != null) && (pottery == null)) {
            if (!tile.world.isRemote) {
                val entityItem = this.pottery!!.createEntity(tile, entity)
                tile.world.spawnEntity(entityItem)
            }
            this.pottery = null
            return true
        }
        return false
    }

    override fun serializeNBT() = NBTTagCompound().also { nbt ->
        nbt.writeObject("pottery", this.pottery)
    }

    override fun deserializeNBT(nbt: NBTTagCompound?) {
        this.pottery = nbt.readObject("pottery", { Pottery() })
    }

    companion object {
        const val SIZE = 12.0
        const val OFFSET = (32.0 - SIZE * 2.0) / 4.0
    }
}
