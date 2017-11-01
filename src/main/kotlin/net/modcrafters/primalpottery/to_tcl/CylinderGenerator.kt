package net.modcrafters.primalpottery.to_tcl

import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import net.ndrei.teslacorelib.render.selfrendering.RawLump

object CylinderGenerator {
    private class V2d(val x: Double, val y: Double)

    private fun Array<V2d>.makeUVs(offset: Float = 8.0f) = this.map {
        Vec2f(offset + (it.x / 2.0).toFloat(), offset + (it.y / 2.0).toFloat())
    }.toTypedArray()

    private fun Array<V2d>.make3D(offsetX: Double, y: Double, offsetZ: Double) = this.map {
        Vec3d(it.x + offsetX, y, it.y + offsetZ)
    }.toTypedArray()

    fun generateCylinder8(centerX: Double, bottom: Double, centerZ: Double, height: Double, radius: Double, sprite: (face: EnumFacing) -> TextureAtlasSprite) =
        RawLump().also { lump ->
            val r = radius
            val r3 = radius / 3.0

            val p0 = V2d(-r3, -r)
            val p1 = V2d(r3, -r)
            val p2 = V2d(r, -r3)
            val p3 = V2d(r, r3)
            val p4 = V2d(r3, r)
            val p5 = V2d(-r3, r)
            val p6 = V2d(-r, r3)
            val p7 = V2d(-r, -r3)

            val top1 = arrayOf(p0, p5, p6, p7).reversedArray()
            val top2 = arrayOf(p0, p1, p4, p5).reversedArray()
            val top3 = arrayOf(p1, p2, p3, p4).reversedArray()

            // top
            lump.addFace(top1.make3D(centerX, bottom + height, centerZ), top1.makeUVs(), sprite(EnumFacing.UP), EnumFacing.UP)
            lump.addFace(top2.make3D(centerX, bottom + height, centerZ), top2.makeUVs(), sprite(EnumFacing.UP), EnumFacing.UP)
            lump.addFace(top3.make3D(centerX, bottom + height, centerZ), top3.makeUVs(), sprite(EnumFacing.UP), EnumFacing.UP)

            // bottom
            val bottom1 = top1.reversedArray()
            val bottom2 = top2.reversedArray()
            val bottom3 = top3.reversedArray()
            lump.addFace(bottom1.make3D(centerX, bottom, centerZ), bottom1.makeUVs(), sprite(EnumFacing.DOWN), EnumFacing.DOWN)
            lump.addFace(bottom2.make3D(centerX, bottom, centerZ), bottom2.makeUVs(), sprite(EnumFacing.DOWN), EnumFacing.DOWN)
            lump.addFace(bottom3.make3D(centerX, bottom, centerZ), bottom3.makeUVs(), sprite(EnumFacing.DOWN), EnumFacing.DOWN)

            fun addFace(p0: V2d, p1: V2d, p2: V2d, facing: EnumFacing) {
                val p0_U = Vec3d(centerX + p0.x, bottom + height, centerZ + p0.y)
                val p0_D = Vec3d(centerX + p0.x, bottom, centerZ + p0.y)
                val p1_U = Vec3d(centerX + p1.x, bottom + height, centerZ + p1.y)
                val p1_D = Vec3d(centerX + p1.x, bottom, centerZ + p1.y)
                val p2_U = Vec3d(centerX + p2.x, bottom + height, centerZ + p2.y)
                val p2_D = Vec3d(centerX + p2.x, bottom, centerZ + p2.y)

                val ps = arrayOf(p0_D, p0_U, p1_U, p1_D)
                val uvs = ps.map {
                    when (facing.axis) {
                        EnumFacing.Axis.X -> V2d(it.z, it.y)
                        /*EnumFacing.Axis.Z*/ else -> V2d(it.x, it.y)
                    }
                }.toTypedArray().makeUVs(0.0f)
                val s = sprite(facing)
                lump.addFace(ps, uvs, s, facing)
                lump.addFace(arrayOf(p1_D, p1_U, p2_U, p2_D), uvs, s, facing)
            }

            addFace(p0, p1, p2, EnumFacing.NORTH)
            addFace(p2, p3, p4, EnumFacing.EAST)
            addFace(p4, p5, p6, EnumFacing.SOUTH)
            addFace(p6, p7, p0, EnumFacing.WEST)
        }
}
