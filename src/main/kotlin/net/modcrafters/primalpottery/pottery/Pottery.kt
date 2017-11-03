package net.modcrafters.primalpottery.pottery

import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.INBTSerializable
import net.modcrafters.primalpottery.to_tcl.getArray

class Pottery(var discs: Array<PotteryDisc> = arrayOf()): INBTSerializable<NBTTagCompound> {
    override fun deserializeNBT(nbt: NBTTagCompound?) {
        this.discs = nbt.getArray("pottery_def", { PotteryDisc(0.0, 0.0) })
    }

    override fun serializeNBT() = NBTTagCompound().also { nbt ->
        val array = NBTTagList()
        this.discs.forEach {
            array.appendTag(it.serializeNBT())
        }
        nbt.setTag("pottery_def", array)
    }

    fun createEntity(tile: TileEntity, entity: Entity? = null): EntityItem {
        val pos = (entity?.position ?: tile.pos).up()
        return EntityItem(tile.world,
            pos.x.toDouble() + .5, pos.y.toDouble(), pos.z.toDouble() + .5,
            ItemStack(PotteryBlock).also { stack ->
                stack.setTagInfo("pottery", this.serializeNBT())
            })
    }

    val cacheKey get() = this.discs.fold("pot") { str, disc ->
        str + "::${disc.radius}:${disc.height}"
    }
}
