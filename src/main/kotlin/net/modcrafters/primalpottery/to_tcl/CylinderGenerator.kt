package net.modcrafters.primalpottery.to_tcl

import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import net.ndrei.teslacorelib.render.selfrendering.RawLump

object CylinderGenerator {
    private class V2d(val x: Double, val y: Double)

    private fun Array<V2d>.makeUVs(offsetX: Float = 8.0f, offsetY: Float = 8.0f) = this.map {
        Vec2f(offsetX + (it.x / 2.0).toFloat(), offsetY + (it.y / 2.0).toFloat())
    }.toTypedArray()

    private fun Array<V2d>.make3D(offsetX: Double, y: Double, offsetZ: Double) = this.map {
        Vec3d(it.x + offsetX, y, it.y + offsetZ)
    }.toTypedArray()

    fun generateCylinder8(centerX: Double, bottom: Double, centerZ: Double, height: Double, radius: Double, sprite: (face: EnumFacing) -> TextureAtlasSprite) =
        this.generateCylinder8(centerX, bottom, centerZ, height, radius, null, sprite)

    fun generateCylinder8(centerX: Double, bottom: Double, centerZ: Double, height: Double, radius: Double, innerRadius: Double?, sprite: (face: EnumFacing) -> TextureAtlasSprite) =
        RawLump().also { lump ->
            val r = radius
            val r3 = radius / 3.0

            fun getPoint(angle: Double, radius: Double) =
                V2d(radius * Math.cos(angle), radius * Math.sin(angle))

            val uOffset = 8.0f - (centerX / 2).toFloat()
            val vOffset = 8.0f - (centerZ / 2).toFloat()

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
                }.toTypedArray().makeUVs(uOffset, vOffset)
                val s = sprite(facing)
                lump.addFace(ps, uvs, s, facing)
                lump.addFace(arrayOf(p1_D, p1_U, p2_U, p2_D), uvs, s, facing)
            }

            val pi = Math.PI
            val pi8 = pi / 8.0
            val pi4 = pi / 4.0

            val p0 = getPoint(pi8, radius)
            val p1 = getPoint(pi8 + pi4, radius)
            val p2 = getPoint(pi8 + pi4 * 2, radius)
            val p3 = getPoint(pi8 + pi4 * 3, radius)
            val p4 = getPoint(pi8 + pi4 * 4, radius)
            val p5 = getPoint(pi8 + pi4 * 5, radius)
            val p6 = getPoint(pi8 + pi4 * 6, radius)
            val p7 = getPoint(pi8 + pi4 * 7, radius)

            if ((innerRadius == null) || (innerRadius <= 0.0)) {
                // top
                val top1 = arrayOf(p0, p5, p6, p7).reversedArray()
                val top2 = arrayOf(p0, p1, p4, p5).reversedArray()
                val top3 = arrayOf(p1, p2, p3, p4).reversedArray()
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
            }
            else {
                val q0 = getPoint(pi8, innerRadius)
                val q1 = getPoint(pi8 + pi4, innerRadius)
                val q2 = getPoint(pi8 + pi4 * 2, innerRadius)
                val q3 = getPoint(pi8 + pi4 * 3, innerRadius)
                val q4 = getPoint(pi8 + pi4 * 4, innerRadius)
                val q5 = getPoint(pi8 + pi4 * 5, innerRadius)
                val q6 = getPoint(pi8 + pi4 * 6, innerRadius)
                val q7 = getPoint(pi8 + pi4 * 7, innerRadius)

                val top1 = arrayOf(p0, q0, q1, p1)
                val top2 = arrayOf(p1, q1, q2, p2)
                val top3 = arrayOf(p2, q2, q3, p3)
                val top4 = arrayOf(p3, q3, q4, p4)
                val top5 = arrayOf(p4, q4, q5, p5)
                val top6 = arrayOf(p5, q5, q6, p6)
                val top7 = arrayOf(p6, q6, q7, p7)
                val top8 = arrayOf(p7, q7, q0, p0)

                arrayOf(top1, top2, top3, top4, top5, top6, top7, top8).forEach {
                    // top
                    lump.addFace(it.make3D(centerX, bottom+height, centerZ), it.makeUVs(), sprite(EnumFacing.UP), EnumFacing.UP)

                    val otherIt = it.reversedArray()
                    lump.addFace(otherIt.make3D(centerX, bottom+height, centerZ), otherIt.makeUVs(), sprite(EnumFacing.UP), EnumFacing.UP)
                }

                addFace(q2, q1, q0, EnumFacing.NORTH)
                addFace(q4, q3, q2, EnumFacing.EAST)
                addFace(q6, q5, q4, EnumFacing.SOUTH)
                addFace(q0, q7, q6, EnumFacing.WEST)
            }

            addFace(p0, p1, p2, EnumFacing.NORTH)
            addFace(p2, p3, p4, EnumFacing.EAST)
            addFace(p4, p5, p6, EnumFacing.SOUTH)
            addFace(p6, p7, p0, EnumFacing.WEST)
        }
}
