package net.modcrafters.primalpottery.template

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.init.Blocks
import net.minecraft.inventory.Container
import net.modcrafters.primalpottery.getSprite
import net.modcrafters.primalpottery.pottery.Pottery
import net.modcrafters.primalpottery.pottery.PotteryDisc
import net.modcrafters.primalpottery.to_tcl.CylinderGenerator
import org.lwjgl.opengl.GL11

class TemplateContainerGui(container: Container) : GuiContainer(container) {
    private val pottery = Pottery(arrayOf(
        PotteryDisc(0.5, 5.0),
        PotteryDisc(0.5, 5.0),
        PotteryDisc(0.5, 5.0),
        PotteryDisc(0.5, 5.0),

        PotteryDisc(0.5, 5.5),
        PotteryDisc(0.5, 5.5),
        PotteryDisc(0.5, 5.5),
        PotteryDisc(0.5, 5.5),
        PotteryDisc(0.5, 5.5),
        PotteryDisc(0.5, 5.5),
        PotteryDisc(0.5, 5.5),
        PotteryDisc(0.5, 5.5),
        PotteryDisc(0.5, 5.5),
        PotteryDisc(0.5, 5.5),
        PotteryDisc(0.5, 5.5),
        PotteryDisc(0.5, 5.5),
        PotteryDisc(0.5, 5.5),
        PotteryDisc(0.5, 5.5),

        PotteryDisc(0.5, 5.0),
        PotteryDisc(0.5, 5.0),

        PotteryDisc(0.5, 4.5),
        PotteryDisc(0.5, 4.5),

        PotteryDisc(0.5, 3.0),
        PotteryDisc(0.5, 3.0)
    ))

    init {
        super.xSize = 180
        super.ySize = 170
    }

    override fun initGui() {
        super.initGui()

        this.addButton(GuiButton(1, this.guiLeft + 118,  this.guiTop + 88, 53, 20, "Simplify 2"))
        this.addButton(GuiButton(2, this.guiLeft + 118,  this.guiTop + 111, 53, 20, "Simplify 3"))
    }

    override fun actionPerformed(button: GuiButton) {
        if (button.id == 1) {
            (0 .. (this.pottery.discs.size / 2)).forEach {
                val x = it * 2
                if (x in (0 .. (this.pottery.discs.size - 2))) {
                    val inner1 = this.pottery.discs[x + 0].innerRadius ?: 0.0
                    val inner2 = this.pottery.discs[x + 1].innerRadius ?: 0.0
                    val inner = ((inner1 + inner2) / 2).let { if (it == 0.0) null else it }
                    this.pottery.discs[x + 0].innerRadius = inner
                    this.pottery.discs[x + 1].innerRadius = inner

                    val radius = (this.pottery.discs[x].radius + this.pottery.discs[x + 1].radius) / 2
                    this.pottery.discs[x + 0].radius = radius
                    this.pottery.discs[x + 1].radius = radius
                }
            }
        }
        else if (button.id == 2) {
            (0 .. (this.pottery.discs.size / 3)).forEach {
                val x = it * 3
                if (x in (0 .. (this.pottery.discs.size - 3))) {
                    val inner1 = this.pottery.discs[x + 0].innerRadius ?: 0.0
                    val inner2 = this.pottery.discs[x + 1].innerRadius ?: 0.0
                    val inner3 = this.pottery.discs[x + 2].innerRadius ?: 0.0
                    val inner = ((inner1 + inner2 + inner3) / 3).let { if (it == 0.0) null else it }
                    this.pottery.discs[x + 0].innerRadius = inner
                    this.pottery.discs[x + 1].innerRadius = inner
                    this.pottery.discs[x + 2].innerRadius = inner

                    val radius = (this.pottery.discs[x].radius + this.pottery.discs[x + 1].radius  + this.pottery.discs[x + 2].radius) / 3
                    this.pottery.discs[x + 0].radius = radius
                    this.pottery.discs[x + 1].radius = radius
                    this.pottery.discs[x + 2].radius = radius
                }
            }
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawDefaultBackground()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        super.drawScreen(mouseX, mouseY, partialTicks)
        super.renderHoveredToolTip(mouseX, mouseY)

        this.drawPottery(partialTicks)
    }

    override fun drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int) {
        TemplateTextures.MAIN.bind()
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize)
    }

    override fun drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY)

        (0 until 24).forEach { y ->
            val level = this.pottery.discs[y]
            if (level.radius > 0.0f) {
                this.drawGradientRect(
                    10 + ((level.innerRadius ?: 0.0) * 100.0 / 5.5).toInt().coerceIn(0, 100),
                    10 + y * 5,
                    10 + (level.radius * 100.0 / 5.5).toInt().coerceIn(0, 100),
                    15 + y * 5, 1615855616, 1615855616)
            }
        }

        val info = this.pottery.getPotteryInfo()
        super.drawString(super.fontRenderer, info.type.color.toString() + info.type.label, 10, 135, -1)
        super.drawString(super.fontRenderer, "${info.volume} mb", 10, 135 + super.fontRenderer.FONT_HEIGHT, -1)
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)

        val x = mouseX - this.guiLeft - 10
        val y = Math.floor((mouseY - this.guiTop - 10).toDouble() / 5.0).toInt()
        if ((x in (0 until 100)) && (y in (0 until 24))) {
            if (clickedMouseButton == 0) {
                this.pottery.discs[y].radius = x.toDouble() * 5.5 / 100.0
            }
            else if (clickedMouseButton == 1) {
                this.pottery.discs[y].innerRadius = x.toDouble() * 5.5 / 100.0
            }
        }
    }

    fun drawPottery(partialTicks: Float) {
        GlStateManager.pushMatrix()
        GlStateManager.translate(this.guiLeft.toDouble() + this.xSize.toDouble() - 35.0, this.guiTop.toDouble() + 20.0, 42.0)
        GlStateManager.scale(4.0, 4.0, 4.0)
        GlStateManager.rotate(-20.0f, 1.0f, 0.2f, 0.0f)
        GlStateManager.rotate(-(Minecraft.getSystemTime() / 20 % 360).toFloat(), 0.0f, 1.0f, 0.0f)

        Minecraft.getMinecraft().textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
        GlStateManager.disableLighting()
        GlStateManager.enableRescaleNormal()

        val buffer = Tessellator.getInstance().buffer
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR)

        val clay = Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(14)
        val clayModel = Minecraft.getMinecraft().blockRendererDispatcher.blockModelShapes.getModelForState(clay)

        var discOffset = 0.0
        this.pottery.discs.map {
            discOffset += it.height
            CylinderGenerator.generateCylinder8(
                0.0, discOffset - it.height, 0.0,
                it.height, it.radius, it.innerRadius, { face -> clayModel.getSprite(clay, face) })
        }.forEach {
            it.draw(buffer)
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, 0.4f)
        GlStateManager.enableCull()

//        GlStateManager.scale(1.0, 1.0, -1.0)

        Tessellator.getInstance().draw()

        GlStateManager.enableLighting()
        GlStateManager.popMatrix()
    }
}