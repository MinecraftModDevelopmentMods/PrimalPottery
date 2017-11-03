package net.modcrafters.primalpottery.potterywheel

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumHand
import net.minecraft.util.ITickable
import net.modcrafters.primalpottery.pottery.Pottery
import net.modcrafters.primalpottery.pottery.PotteryDisc
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
    var hasClay = false
    var clayProgress = 0.0f

    override fun getParts(): List<IBlockPart> = listOf(table, bigWheel, smallWheel)

    fun getPotteryModel() = Pottery(arrayOf(
        1.0 to 2.0,
        5.0 to 3.0,
        2.0 to 2.0,
        2.0 to 1.0,
        1.0 to 2.0
    ).map { PotteryDisc(it.first, it.second) }.toTypedArray())

    override fun onPartActivated(player: EntityPlayer, hand: EnumHand, part: IBlockPart, hitBox: IBlockPartHitBox): Boolean {
        if (part == this.bigWheel) {
            this.rotationTicks += 24
            return true
        }
        else if (part == this.smallWheel) {
            if (this.hasClay) {
                this.hasClay = false
                if (!this.getWorld().isRemote) {
                    val entity = /*Pottery(arrayOf(
                        PotteryDisc(2.0, 5.5),
                        PotteryDisc(2.0, 3.0),
                        PotteryDisc(5.0, 2.0),
                        PotteryDisc(1.0, 4.0),
                        PotteryDisc(1.0, 5.5)
                    ))*/ this.getPotteryModel().createEntity(this, player)
                    this.getWorld().spawnEntity(entity)
                }
            }
            else {
                val stack = player.getHeldItem(hand)
                if (!stack.isEmpty && (stack.item === Items.CLAY_BALL)) {
                    this.hasClay = true
                    this.clayProgress = 0.0f
                }
            }
            return true
        }

        return false
    }

    val rotationAngle get() = this.angle
    private val SCALE = 10000.0
    private val PROGRESS = .01f

    override fun update() {
        if (--this.rotationTicks > 0) {
            this.angle += Math.PI / 6
            while (this.angle > Math.PI * 2) this.angle -= Math.PI * 2
            this.angle = Math.round(this.angle * SCALE) / SCALE

            if (this.hasClay && (this.clayProgress < 1.0f)) {
                this.clayProgress += PROGRESS
            }

            this.markDirty()
        } else {
            this.rotationTicks = 0
        }
    }
}