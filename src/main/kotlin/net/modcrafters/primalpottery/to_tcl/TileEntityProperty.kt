package net.modcrafters.primalpottery.to_tcl

import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.property.IUnlistedProperty

class TileEntityProperty<E: TileEntity>(private val clazz: Class<E>) : IUnlistedProperty<E> {
    override fun getType() = this.clazz
    override fun isValid(value: E) = this.clazz.isInstance(value)
    override fun getName() = "ENTITY"
    override fun valueToString(value: E) = value.toString()

    companion object {
        val TILE_ENTITY_PROPERTY = TileEntityProperty(TileEntity::class.java)
    }
}