package net.modcrafters.primalpottery.to_tcl

import net.minecraft.block.Block
import net.minecraft.block.ITileEntityProvider
import net.minecraft.block.material.Material
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.property.ExtendedBlockState
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import net.minecraftforge.registries.IForgeRegistry
import net.ndrei.teslacorelib.blocks.MultiPartBlock
import net.ndrei.teslapoweredthingies.render.bakery.SelfRenderingTESR

open class MultiPartEntityBlock<T: TileEntity>(modId: String, tab: CreativeTabs?, registryName: String, material: Material, private val teClass: Class<T>)
    : MultiPartBlock(modId, tab, registryName, material), ITileEntityProvider {

    override fun registerBlock(registry: IForgeRegistry<Block>) {
        super.registerBlock(registry)
        GameRegistry.registerTileEntity(this.teClass, this.registryName!!.toString() + "_tile")
    }

    @SideOnly(Side.CLIENT)
    override fun registerRenderer() {
        super.registerRenderer()
        ClientRegistry.bindTileEntitySpecialRenderer(this.teClass, SelfRenderingTESR)
    }

    override fun createBlockState(): BlockStateContainer {
        return ExtendedBlockState(this, arrayOf(), arrayOf(TileEntityProperty.TILE_ENTITY_PROPERTY))
    }

    override fun getExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState {
        if (state is IExtendedBlockState) {
            val te = world.getTileEntity(pos)
            if (this.teClass.isInstance(te)) {
                return state.withProperty(TileEntityProperty.TILE_ENTITY_PROPERTY, te)
            }
        }
        return super.getExtendedState(state, world, pos)
    }

    override fun createNewTileEntity(worldIn: World, meta: Int): T =
        this.teClass.newInstance()
}