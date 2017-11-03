package net.modcrafters.primalpottery.to_tcl

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraftforge.common.util.Constants
import net.minecraftforge.common.util.INBTSerializable

fun NBTTagCompound?.getDoubleWithDefault(key: String, default: Double = 0.0) =
    if ((this == null) || !this.hasKey(key, Constants.NBT.TAG_DOUBLE)) default else this.getDouble(key)

inline fun<reified T: INBTSerializable<NBTTagCompound>> NBTTagCompound?.getArray(key: String, maker: () -> T) = mutableListOf<T>().also { result ->
    if ((this != null) && this.hasKey(key, Constants.NBT.TAG_LIST)) {
        this.getTagList(key, Constants.NBT.TAG_COMPOUND)
            .filterIsInstance<NBTTagCompound>()
            .forEach { nbt ->
                result.add(maker().also { it.deserializeNBT(nbt) })
            }
    }
}.toTypedArray()

inline fun<reified T: INBTSerializable<NBTTagCompound>> NBTTagCompound?.readList(key: String, array: List<T>, checkSize: Boolean = false, required: Boolean = false) {
    if ((this == null) || !this.hasKey(key, Constants.NBT.TAG_LIST)) {
        if (required)
            throw Exception("List with key '$key' not found in NBT data.")
    }
    else {
        val list = this.getTagList(key, Constants.NBT.TAG_COMPOUND)
        if (checkSize && (list.tagCount() != array.size))
            throw Exception("List with key '$key' has incorrect number of items <${list.tagCount()}>, was expecting: <${array.size}>.")

        (0 until Math.min(list.tagCount(), array.size)).forEach { index ->
            array[index].deserializeNBT(list.getCompoundTagAt(index))
        }
    }
}

inline fun<reified T: INBTSerializable<NBTTagCompound>> NBTTagCompound.writeList(key: String, list: List<T>) {
    val nbt = NBTTagList()
    list.forEach { nbt.appendTag(it.serializeNBT()) }
    this.setTag(key, nbt)
}

inline fun<reified T: INBTSerializable<NBTTagCompound>> NBTTagCompound?.readObject(key: String, maker: () -> T) =
    if ((this == null) || !this.hasKey(key, Constants.NBT.TAG_COMPOUND)) null else maker().also { it.deserializeNBT(this.getCompoundTag(key)) }

inline fun<reified T: INBTSerializable<NBTTagCompound>> NBTTagCompound.writeObject(key: String, obj: T?) {
    if (obj != null) this.setTag(key, obj.serializeNBT() ?: NBTTagCompound())
}