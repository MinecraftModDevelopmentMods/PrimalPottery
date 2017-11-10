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

    fun getPotteryInfo(): PotteryInfo {
        var thin = false
        var bottom = -1
        var volume = 0
        this.discs.forEachIndexed { index, disc ->
            if ((disc.innerRadius ?: 0.0) <= 0.0) {
                bottom = index
            }
            if ((disc.radius - (disc.innerRadius ?: 0.0)) < .5) {
                thin = true
            }
        }

        if (thin) return PotteryInfo(PotteryType.LUMP_THIN, 0)
        if (bottom < 0) return PotteryInfo(PotteryType.LUMP_NO_BOTTOM, 0)

        var pot = false
        (0 until bottom).forEach {
            volume += ((this.discs[it].innerRadius ?: 0.0) * 10).toInt()
            if ((it > 0) && !pot) {
                val innerBefore = this.discs[it-1].innerRadius ?: 0.0
                val inner = this.discs[it].innerRadius ?: 0.0
                if (innerBefore < inner) {
                    pot = true
                }
            }
        }

        if (volume <= 0) return PotteryInfo(PotteryType.LUMP, 0)
        return PotteryInfo(if (pot) PotteryType.POT else PotteryType.BOWL, volume)
    }

    class PotteryInfo(val type: PotteryType, val volume: Int)
}
