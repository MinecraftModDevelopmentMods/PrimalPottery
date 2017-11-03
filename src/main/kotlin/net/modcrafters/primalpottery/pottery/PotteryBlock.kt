package net.modcrafters.primalpottery.pottery

import com.google.common.cache.CacheBuilder
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.vertex.VertexFormat
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockRenderLayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraftforge.common.model.TRSRTransformation
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.registries.IForgeRegistry
import net.modcrafters.primalpottery.MOD_ID
import net.modcrafters.primalpottery.PrimalPotteryMod
import net.modcrafters.primalpottery.getSprite
import net.modcrafters.primalpottery.to_tcl.CylinderGenerator
import net.modcrafters.primalpottery.to_tcl.MultiPartEntityBlock
import net.modcrafters.primalpottery.to_tcl.TileEntityProperty
import net.modcrafters.primalpottery.to_tcl.readObject
import net.ndrei.teslacorelib.annotations.AutoRegisterBlock
import net.ndrei.teslacorelib.blocks.multipart.BlockPartHitBox
import net.ndrei.teslacorelib.blocks.multipart.IBlockPartHitBox
import net.ndrei.teslacorelib.render.selfrendering.*
import java.util.concurrent.TimeUnit

@AutoRegisterBlock
@SelfRenderingBlock
object PotteryBlock
    : MultiPartEntityBlock<PotteryEntity>(MOD_ID, PrimalPotteryMod.creativeTab, "pottery", Material.CLAY, PotteryEntity::class.java), ISelfRenderingBlock {

    init {
        this.setBlockUnbreakable()
    }

    override fun registerItem(registry: IForgeRegistry<Item>) { /* nothing, because we use a custom item block */ }

    override fun getBakeries(layer: BlockRenderLayer?, state: IBlockState?, stack: ItemStack?, side: EnumFacing?, rand: Long, transform: TRSRTransformation) = mutableListOf<IBakery>().also { bakeries ->
        val clay = Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(14)
        val clayModel = Minecraft.getMinecraft().blockRendererDispatcher.blockModelShapes.getModelForState(clay)

        if (layer == null) {
            var discOffset = 0.0
            val scale = 2.5

            val pottery = stack?.tagCompound?.readObject("pottery", { Pottery(arrayOf()) })
            (pottery?.discs?.map { it.height to it.radius }?.toTypedArray() ?: arrayOf(
                1.0 to 2.0,
                5.0 to 3.0,
                2.0 to 2.0,
                2.0 to 1.0,
                1.0 to 2.0
            )).map {
                val bottom = discOffset
                val height = it.first * scale
                discOffset += height
                CylinderGenerator.generateCylinder8(
                    16.0, bottom, 16.0,
                    height, it.second * scale, { face -> clayModel.getSprite(clay, face) })
            }.combine().static().addTo(bakeries)
        }
        else if (layer == BlockRenderLayer.SOLID) {
            bakeries.add(object: IBakery {
                private val cache = CacheBuilder.newBuilder().expireAfterAccess(42, TimeUnit.SECONDS).build<String, List<BakedQuad>>()

                override fun getQuads(state: IBlockState?, stack: ItemStack?, side: EnumFacing?, vertexFormat: VertexFormat, transform: TRSRTransformation) = mutableListOf<BakedQuad>().also { quads ->
                    val tile = (state as? IExtendedBlockState)?.getValue(TileEntityProperty.TILE_ENTITY_PROPERTY) as? PotteryEntity
                    if (tile != null) {
                        tile.getParts().filter { it.pottery != null }.forEach {
                            val cacheKey = "${it.xAlign}::${it.zAlign}::${it.pottery!!.cacheKey}"
                            quads.addAll(this.cache.get(cacheKey, {
                                PrimalPotteryMod.logger.info("Creating pottery model for: <$cacheKey>.")
                                val area = it.hitBoxes.fold<IBlockPartHitBox, AxisAlignedBB?>(null) { aabb, box ->
                                    if (aabb == null) box.aabb else aabb.union(box.aabb)
                                } ?: BlockPartHitBox.big32Sized(10.0, 10.0, 10.0, 12.0, 12.0, 12.0).aabb
                                val centerX = (area.maxX + area.minX) / 2.0 * 32.0
                                val centerZ = (area.maxZ + area.minZ) / 2.0 * 32.0
                                var bottom = area.minY * 32.0
                                it.pottery!!.discs.map {
                                    val b = bottom
                                    bottom += it.height
                                    CylinderGenerator.generateCylinder8(centerX, b, centerZ, it.height, it.radius,
                                        { face -> clayModel.getSprite(clay, face) })
                                }.combine().getQuads(state, stack, side, vertexFormat, transform)
                            }))
                        }
                    }
                }
            })
        }
    }.toList()

//    override fun onBlockPlacedBy(world: World?, pos: BlockPos?, state: IBlockState?, placer: EntityLivingBase?, stack: ItemStack?) {
//        val pottery = stack?.tagCompound?.readObject("pottery", { Pottery() })
//        if ((pottery != null) && (world != null) && (!world.isRemote) && (state != null) && (pos != null)) {
//            val te = this.createTileEntity(world, state) as? PotteryEntity
//            if (te != null) {
//                world.setTileEntity(pos, te)
//                te.addPottery(pottery)
//            }
//        }
//        super.onBlockPlacedBy(world, pos, state, placer, stack)
//    }
}
