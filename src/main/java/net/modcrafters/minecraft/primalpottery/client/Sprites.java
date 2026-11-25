package net.modcrafters.minecraft.primalpottery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.modcrafters.minecraft.primalpottery.Ids;

public final class Sprites {
    private Sprites() {
    }

    public static TextureAtlasSprite get(Mesh.Texture texture) {
        String path = switch (texture) {
            case PLANKS -> "block/oak_planks";
            case STONE -> "block/stone";
            case LOG -> "block/oak_log";
            case CLAY -> "block/red_terracotta";
        };
        Identifier id = Ids.of("minecraft", path);
        //? if >=26.1 {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).getSprite(id);
        //?} else
        //return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(id);
    }

    public static Identifier blocks() {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    public static void draw(Mesh mesh, VertexConsumer buffer, PoseStack.Pose pose, int light) {
        draw(mesh, buffer, pose, light, 255);
    }

    public static void draw(Mesh mesh, VertexConsumer buffer, PoseStack.Pose pose, int light, int alpha) {
        for (Mesh.Quad quad : mesh.quads()) {
            TextureAtlasSprite sprite = get(quad.texture);
            for (int vertex = 0; vertex < 4; vertex++) {
                put(buffer, pose, quad.x[vertex], quad.y[vertex], quad.z[vertex], sampleU(sprite, quad.u[vertex]), sampleV(sprite, quad.v[vertex]), light, alpha, quad.nx, quad.ny, quad.nz);
            }
        }
    }

    private static float sampleU(TextureAtlasSprite sprite, float unit) {
        return sprite.getU(unit / 16.0F);
    }

    private static float sampleV(TextureAtlasSprite sprite, float unit) {
        return sprite.getV(unit / 16.0F);
    }

    private static void put(
            VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light, int alpha, float nx, float ny, float nz
    ) {
        //? if <1.21 {
        /*buffer.vertex(pose, x, y, z).color(255, 255, 255, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose, nx, ny, nz).endVertex();
        *///?} else {
        buffer.addVertex(pose, x, y, z).setColor(255, 255, 255, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setUv2(light & 0xFFFF, light >> 16).setNormal(pose, nx, ny, nz);
        //?}
    }
}
