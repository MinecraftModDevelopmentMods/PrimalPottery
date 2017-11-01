package net.modcrafters.primalpottery.potterywheel

import com.google.common.cache.CacheBuilder
import net.minecraft.block.Block
import net.minecraft.block.ITileEntityProvider
import net.minecraft.block.material.Material
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.vertex.VertexFormat
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockRenderLayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.model.TRSRTransformation
import net.minecraftforge.common.property.ExtendedBlockState
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.registries.IForgeRegistry
import net.modcrafters.primalpottery.MOD_ID
import net.modcrafters.primalpottery.PrimalPotteryMod
import net.modcrafters.primalpottery.getSprite
import net.modcrafters.primalpottery.to_tcl.CylinderGenerator
import net.modcrafters.primalpottery.to_tcl.TileEntityProperty.Companion.TILE_ENTITY_PROPERTY
import net.ndrei.teslacorelib.annotations.AutoRegisterBlock
import net.ndrei.teslacorelib.blocks.MultiPartBlock
import net.ndrei.teslacorelib.render.selfrendering.*
import java.util.concurrent.TimeUnit
import javax.vecmath.Matrix4d
import javax.vecmath.Vector3d

@AutoRegisterBlock
@SelfRenderingBlock
object PotteryWheelBlock : MultiPartBlock(MOD_ID, PrimalPotteryMod.creativeTab, "pottery_wheel", Material.WOOD), ISelfRenderingBlock, ITileEntityProvider {
    override fun registerBlock(registry: IForgeRegistry<Block>) {
        super.registerBlock(registry)
        GameRegistry.registerTileEntity(PotteryWheelEntity::class.java, this.registryName!!.toString())
    }

    override fun createBlockState(): BlockStateContainer {
        return ExtendedBlockState(this, arrayOf(), arrayOf(TILE_ENTITY_PROPERTY))
    }

    override fun getExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState {
        if (state is IExtendedBlockState) {
            val te = world.getTileEntity(pos)
            if (te is PotteryWheelEntity) {
                return state.withProperty(TILE_ENTITY_PROPERTY, te)
            }
        }
        return super.getExtendedState(state, world, pos)
    }

    override fun createNewTileEntity(worldIn: World, meta: Int) = PotteryWheelEntity()

    override fun getBakeries(layer: BlockRenderLayer?, state: IBlockState?, stack: ItemStack?, side: EnumFacing?, rand: Long, transform: TRSRTransformation) = mutableListOf<IBakery>().also { bakeries ->
        if (layer == BlockRenderLayer.SOLID) {
            //#region static stuff

            val staticBakeries = mutableListOf<IBakery>()

            val legWidth = 4.0
            val legPadding = 1.0
            val legHeight = 20.0
            val legs = arrayOf(legPadding, 32.0 - legWidth - legPadding)

            val planks = Blocks.PLANKS.defaultState
            val model = Minecraft.getMinecraft().blockRendererDispatcher.blockModelShapes.getModelForState(planks)

            legs.forEach { x ->
                legs.forEach { z ->
                    RawCube(Vec3d(x, 0.0, z), Vec3d(x + legWidth, legHeight, z + legWidth)).autoUV().also {
                        arrayOf(EnumFacing.DOWN, *EnumFacing.HORIZONTALS).forEach { face ->
                            it.addFace(face)
                            it.sprite(model.getSprite(planks, face))
                        }
                    }.static().addTo(staticBakeries)
                }
            }

            val tableHeight = 4.0
            RawCube(Vec3d(0.0, legHeight, 0.0), Vec3d(32.0, legHeight + tableHeight, 32.0)).autoUV().also {
                EnumFacing.VALUES.forEach { face ->
                    it.addFace(face)
                    it.sprite(model.getSprite(planks, face))
                }
            }.static().addTo(staticBakeries)

            val bottomThingOffset = 2.0
            val bottomThingWidth = 2.0
            object: IBakery {
                override fun getQuads(state: IBlockState?, stack: ItemStack?, side: EnumFacing?, vertexFormat: VertexFormat, transform: TRSRTransformation) = mutableListOf<BakedQuad>().also { quads ->
                    val matrix1 = Matrix4d().also { it.setIdentity() }
                    matrix1.mul(Matrix4d().also { it.set(Vector3d(16.0, 0.0, 16.0)) })
                    matrix1.mul(Matrix4d().also { it.rotY(Math.PI / 4) })
                    matrix1.mul(Matrix4d().also { it.set(Vector3d(-16.0, 0.0, -16.0)) })

                    RawCube(
                        Vec3d(16.0 - bottomThingWidth / 2.0, bottomThingOffset, 0.0),
                        Vec3d(16.0 + bottomThingWidth / 2.0, bottomThingOffset + bottomThingWidth, 32.0)
                    ).autoUV().also {
                        EnumFacing.VALUES.forEach { face ->
                            it.addFace(face)
                            it.sprite(model.getSprite(planks, face))
                        }
                    }.bake(quads, vertexFormat, transform, matrix1)

                    val matrix2 = Matrix4d().also { it.setIdentity() }
                    matrix2.mul(Matrix4d().also { it.set(Vector3d(16.0, 0.0, 16.0)) })
                    matrix2.mul(Matrix4d().also { it.rotY(-Math.PI / 4) })
                    matrix2.mul(Matrix4d().also { it.set(Vector3d(-16.0, 0.001, -16.0)) })
                    RawCube(
                        Vec3d(16.0 - bottomThingWidth / 2.0, bottomThingOffset, 0.0),
                        Vec3d(16.0 + bottomThingWidth / 2.0, bottomThingOffset + bottomThingWidth, 32.0)
                    ).autoUV().also {
                        EnumFacing.VALUES.forEach { face ->
                            it.addFace(face)
                            it.sprite(model.getSprite(planks, face))
                        }
                    }.bake(quads, vertexFormat, transform, matrix2)
                }
            }.static().addTo(staticBakeries)

            staticBakeries.combine().static().addTo(bakeries)

            //#endregion

            val rotating = mutableListOf<RawLump>()

            val stone = Blocks.STONE.defaultState
            val stoneModel = Minecraft.getMinecraft().blockRendererDispatcher.blockModelShapes.getModelForState(stone)

            val bottomCylinderOffset = bottomThingOffset + bottomThingWidth + 1.0
            val bottomCylinderRadius = 12.0
            val bottomCylinderHeight = 3.0
            CylinderGenerator.generateCylinder8(16.0, bottomCylinderOffset, 16.0, bottomCylinderHeight, bottomCylinderRadius, { face ->
                stoneModel.getSprite(stone, face)
            }).also { rotating.add(it) }

            val topCylinderOffset = legHeight + tableHeight + 2.0
            val topCylinderRadius = 6.5
            val topCylinderHeight = 2.0
            CylinderGenerator.generateCylinder8(16.0, topCylinderOffset, 16.0, topCylinderHeight, topCylinderRadius, { face ->
                stoneModel.getSprite(stone, face)
            }).also { rotating.add(it) }

            val log = Blocks.LOG.defaultState
            val logModel = Minecraft.getMinecraft().blockRendererDispatcher.blockModelShapes.getModelForState(log)

            val centerCylinderBottom = 0.5
            val centerCylinderRadius = 1.0
            val centerCylinderHeight = topCylinderOffset - centerCylinderBottom
            CylinderGenerator.generateCylinder8(16.0, centerCylinderBottom, 16.0, centerCylinderHeight, centerCylinderRadius, { face ->
                logModel.getSprite(log, face)
            }).also { rotating.add(it) }

            object: IBakery {
                private val cache = CacheBuilder.newBuilder().expireAfterAccess(42, TimeUnit.SECONDS).build<String, MutableList<BakedQuad>>()
                private lateinit var lumps: Array<RawLump>

                override fun getQuads(state: IBlockState?, stack: ItemStack?, side: EnumFacing?, vertexFormat: VertexFormat, transform: TRSRTransformation): MutableList<BakedQuad> {
                    val angle = (state as? IExtendedBlockState)?.getValue(TILE_ENTITY_PROPERTY)?.rotationAngle ?: 0.0
                    return this.cache.get(angle.toString(), {
                        PrimalPotteryMod.logger.info("Created Rotating Bakery for: <$angle>!")
                        val matrix = Matrix4d().also { it.setIdentity() }
                        matrix.mul(Matrix4d().also { it.set(Vector3d(16.0, 0.0, 16.0)) })
                        matrix.mul(Matrix4d().also { it.rotY(angle) })
                        matrix.mul(Matrix4d().also { it.set(Vector3d(-16.0, 0.0, -16.0)) })

                        this.lumps.fold(mutableListOf<BakedQuad>()) { list, lump ->
                            list.also { lump.bake(it, vertexFormat, transform, matrix) }
                        }
                    })
                }

                fun initBakery(lumps: Array<RawLump>) = this.also { it.lumps = lumps }
            }.initBakery(rotating.toTypedArray()).addTo(bakeries)

            PrimalPotteryMod.logger.info("Created Block Bakeries!")
        }
    }
}
