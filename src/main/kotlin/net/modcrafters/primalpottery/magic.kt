package net.modcrafters.primalpottery

import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.model.IBakedModel
import net.minecraft.util.EnumFacing
import java.util.*

fun IBakedModel.getSprite(state: IBlockState, it: EnumFacing) =
    this.getQuads(state, it, Random().nextLong())
        .firstOrNull { q -> q.face == it }?.sprite ?: Minecraft.getMinecraft().textureMapBlocks.missingSprite

fun IBakedModel.getTintIndex(state: IBlockState, it: EnumFacing) =
    this.getQuads(state, it, Random().nextLong())
        .firstOrNull { q -> q.face == it }?.tintIndex ?: -1
