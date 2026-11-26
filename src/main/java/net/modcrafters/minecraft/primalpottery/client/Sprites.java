package net.modcrafters.minecraft.primalpottery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.modcrafters.minecraft.primalpottery.Ids;
import org.jspecify.annotations.Nullable;

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

    public static final int WATER_TINT = 0x3F76E4;

    /** Water and lava use the still blocks-atlas sprite. Another fluid uses its particle sprite. A missing sprite is absent. */
    public static @Nullable TextureAtlasSprite fluid(Fluid fluid) {
        if (fluid == Fluids.WATER) {
            return still("block/water_still");
        }
        if (fluid == Fluids.LAVA) {
            return still("block/lava_still");
        }
        return particle(fluid.defaultFluidState().createLegacyBlock());
    }

    public static boolean fluidSolid(Fluid fluid) {
        return fluid == Fluids.LAVA;
    }

    public static int fluidColor(Fluid fluid) {
        return fluid == Fluids.WATER ? WATER_TINT : 0xFFFFFF;
    }

    private static @Nullable TextureAtlasSprite still(String path) {
        Identifier id = Ids.of("minecraft", path);
        //? if >=26.1 {
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS);
        TextureAtlasSprite sprite = atlas.getSprite(id);
        return sprite == atlas.missingSprite() ? null : sprite;
        //?} else {
        /*java.util.function.Function<Identifier, TextureAtlasSprite> atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite sprite = atlas.apply(id);
        TextureAtlasSprite missing = atlas.apply(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation());
        return sprite == missing ? null : sprite;
        *///?}
    }

    private static @Nullable TextureAtlasSprite particle(BlockState state) {
        //? if >=26.1 {
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite();
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS);
        if (sprite == null || sprite == atlas.missingSprite()) {
            return null;
        }
        return sprite;
        //?} else {
        /*TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModel(state).getParticleIcon();
        java.util.function.Function<Identifier, TextureAtlasSprite> atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite missing = atlas.apply(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation());
        if (sprite == null || sprite == missing) {
            return null;
        }
        return sprite;
        *///?}
    }

    public static void draw(Mesh mesh, VertexConsumer buffer, PoseStack.Pose pose, int light) {
        draw(mesh, buffer, pose, light, 255);
    }

    public static void draw(Mesh mesh, VertexConsumer buffer, PoseStack.Pose pose, int light, int alpha) {
        for (Mesh.Quad quad : mesh.quads()) {
            TextureAtlasSprite sprite = get(quad.texture);
            for (int vertex = 0; vertex < 4; vertex++) {
                put(buffer, pose, quad.x[vertex], quad.y[vertex], quad.z[vertex], sampleU(sprite, quad.u[vertex]), sampleV(sprite, quad.v[vertex]), light, 255, 255, 255, alpha, quad.nx, quad.ny, quad.nz);
            }
        }
    }

    public static void drawFluid(Mesh mesh, VertexConsumer buffer, PoseStack.Pose pose, int light, TextureAtlasSprite sprite, int color) {
        int red = (color >> 16) & 255;
        int green = (color >> 8) & 255;
        int blue = color & 255;
        for (Mesh.Quad quad : mesh.quads()) {
            for (int vertex = 0; vertex < 4; vertex++) {
                put(buffer, pose, quad.x[vertex], quad.y[vertex], quad.z[vertex], sampleU(sprite, quad.u[vertex]), sampleV(sprite, quad.v[vertex]), light, red, green, blue, 255, quad.nx, quad.ny, quad.nz);
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
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int light,
            int red,
            int green,
            int blue,
            int alpha,
            float nx,
            float ny,
            float nz
    ) {
        //? if <1.21 {
        /*buffer.vertex(pose, x, y, z).color(red, green, blue, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose, nx, ny, nz).endVertex();
        *///?} else {
        buffer.addVertex(pose, x, y, z).setColor(red, green, blue, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setUv2(light & 0xFFFF, light >> 16).setNormal(pose, nx, ny, nz);
        //?}
    }
}
