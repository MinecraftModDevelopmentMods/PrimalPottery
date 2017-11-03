package net.modcrafters.primalpottery.pottery

import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable
import net.modcrafters.primalpottery.to_tcl.getDoubleWithDefault

class PotteryDisc(var height: Double, var radius: Double): INBTSerializable<NBTTagCompound> {
    override fun deserializeNBT(nbt: NBTTagCompound?) {
        this.height = nbt.getDoubleWithDefault("height")
        this.radius = nbt.getDoubleWithDefault("radius")
    }

    override fun serializeNBT() = NBTTagCompound().also { nbt ->
        nbt.setDouble("height", this.height)
        nbt.setDouble("radius", this.radius)
    }
}
