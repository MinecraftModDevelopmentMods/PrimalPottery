package net.modcrafters.primalpottery.potterywheel

import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.renderer.vertex.VertexFormat
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.BlockRenderLayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import net.minecraftforge.common.model.TRSRTransformation
import net.modcrafters.primalpottery.MOD_ID
import net.modcrafters.primalpottery.PrimalPotteryMod
import net.modcrafters.primalpottery.getSprite
import net.modcrafters.primalpottery.to_tcl.CylinderGenerator
import net.modcrafters.primalpottery.to_tcl.MultiPartEntityBlock
import net.ndrei.teslacorelib.annotations.AutoRegisterBlock
import net.ndrei.teslacorelib.render.selfrendering.*
import org.lwjgl.opengl.GL11.GL_QUADS
import javax.vecmath.Matrix4d
import javax.vecmath.Vector3d

@AutoRegisterBlock
@SelfRenderingBlock
object PotteryWheelBlock
    : MultiPartEntityBlock<PotteryWheelEntity>(MOD_ID, PrimalPotteryMod.creativeTab, "pottery_wheel", Material.WOOD, PotteryWheelEntity::class.java), ISelfRenderingBlock {
    private val legHeight = 20.0
    private val tableHeight = 4.0

    private val bottomThingOffset = 2.0
    private val bottomThingWidth = 2.0

    private val bottomCylinderOffset = bottomThingOffset + bottomThingWidth + 1.0
    private val bottomCylinderRadius = 12.0
    private val bottomCylinderHeight = 3.0

    private val topCylinderOffset = legHeight + tableHeight + 2.0
    private val topCylinderRadius = 6.5
    private val topCylinderHeight = 2.0

    private val centerCylinderBottom = 0.5
    private val centerCylinderRadius = 1.0
    private val centerCylinderHeight = topCylinderOffset - centerCylinderBottom

    private val rotatingParts by lazy { mutableListOf<RawLump>().also { rotating ->
        val stone = Blocks.STONE.defaultState
        val stoneModel = Minecraft.getMinecraft().blockRendererDispatcher.blockModelShapes.getModelForState(stone)

        CylinderGenerator.generateCylinder8(16.0, bottomCylinderOffset, 16.0, bottomCylinderHeight, bottomCylinderRadius, { face ->
            stoneModel.getSprite(stone, face)
        }).also { rotating.add(it) }

        CylinderGenerator.generateCylinder8(16.0, topCylinderOffset, 16.0, topCylinderHeight, topCylinderRadius, { face ->
            stoneModel.getSprite(stone, face)
        }).also { rotating.add(it) }

        val log = Blocks.LOG.defaultState
        val logModel = Minecraft.getMinecraft().blockRendererDispatcher.blockModelShapes.getModelForState(log)

        CylinderGenerator.generateCylinder8(16.0, centerCylinderBottom, 16.0, centerCylinderHeight, centerCylinderRadius, { face ->
            logModel.getSprite(log, face)
        }).also { rotating.add(it) }
    }.toList() }

    override fun getBakeries(layer: BlockRenderLayer?, state: IBlockState?, stack: ItemStack?, side: EnumFacing?, rand: Long, transform: TRSRTransformation) = mutableListOf<IBakery>().also { bakeries ->
        if ((layer == BlockRenderLayer.SOLID) || (layer == null)) {
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

            if (layer == null) {
                staticBakeries.addAll(this@PotteryWheelBlock.rotatingParts)
            }

            staticBakeries.combine().static().addTo(bakeries)

            //#endregion
        }
    }

    override fun renderTESR(proxy: TESRProxy, te: TileEntity, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, alpha: Float) {
        val tile = (te as? PotteryWheelEntity) ?: return

        GlStateManager.pushMatrix()

        GlStateManager.scale(1.0f, -1.0f, 1.0f)
        GlStateManager.translate(16.0f, -32.0f, 16.0f)
        GlStateManager.rotate(-(tile.rotationAngle * 360.0 / (Math.PI * 2)).toFloat(), 0.0f, 1.0f, 0.0f)
        GlStateManager.translate(-16.0f, 0.0f, -16.0f)

        proxy.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
        GlStateManager.disableLighting()

        val buffer = Tessellator.getInstance().buffer
        buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR)
        this.rotatingParts.forEach { it.draw(buffer) }
        Tessellator.getInstance().draw()

        if (tile.hasClay) {
            val clay = Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(14)
            val clayModel = Minecraft.getMinecraft().blockRendererDispatcher.blockModelShapes.getModelForState(clay)

            var discOffset = 0.0

            buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR)
            /*arrayOf(
                1.0 to 2.0,
                5.0 to 3.0,
                2.0 to 2.0,
                2.0 to 1.0,
                1.0 to 2.0
            )*/tile.getPotteryModel().discs.map {
                discOffset += it.height
                CylinderGenerator.generateCylinder8(
                    16.0, discOffset - it.height, 16.0,
                    it.height, it.radius + (4.0 - it.radius) * (1.0 - tile.clayProgress), { face -> clayModel.getSprite(clay, face) })
            }.forEach {
                it.draw(buffer)
            }

            GlStateManager.translate(0.0, topCylinderOffset + topCylinderHeight, 0.0)
            Tessellator.getInstance().draw()
        }

        GlStateManager.enableLighting()
        GlStateManager.popMatrix()
    }
}
