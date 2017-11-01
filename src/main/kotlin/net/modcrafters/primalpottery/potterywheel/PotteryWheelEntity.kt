package net.modcrafters.primalpottery.potterywheel

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumHand
import net.minecraft.util.ITickable
import net.ndrei.teslacorelib.blocks.multipart.*
import net.ndrei.teslacorelib.utils.withAlpha
import java.awt.Color

class PotteryWheelEntity: TileEntity(), IBlockPartProvider, ITickable {
    private val table = object : BlockPart() {
        init { super.boxes.add(BlockPartHitBox.big32(0.0, 20.0, 0.0, 32.0, 24.0, 32.0)) }
    }

    private val bigWheel = object : BlockPart(Color.BLUE.withAlpha(.75f).rgb) {
        init { super.boxes.add(BlockPartHitBox.big32(4.0,5.0, 4.0, 28.0, 8.0, 28.0)) }
    }

    private val smallWheel = object : BlockPart(Color.RED.withAlpha(.75f).rgb) {
        init { super.boxes.add(BlockPartHitBox.big32(9.5,26.0, 9.5, 22.5, 28.0, 22.5)) }
    }

    private var rotationTicks = 0
    private var angle = 0.0

    override fun getParts(): List<IBlockPart> = listOf(table, bigWheel, smallWheel)

    override fun onPartActivated(player: EntityPlayer, hand: EnumHand, part: IBlockPart, hitBox: IBlockPartHitBox): Boolean {
        if (part == this.bigWheel) {
            this.rotationTicks += 24
            return true
        }

        return false
    }

    val rotationAngle get() = this.angle
    private val SCALE = 10000.0

    override fun update() {
        if (--this.rotationTicks > 0) {
            this.angle += Math.PI / 6
            while (this.angle > Math.PI * 2) this.angle -= Math.PI * 2
            this.angle = Math.round(this.angle * SCALE) / SCALE
            this.markDirty()
            this.getWorld().markBlockRangeForRenderUpdate(this.getPos(), this.getPos())
        } else {
            this.rotationTicks = 0
        }
    }
}