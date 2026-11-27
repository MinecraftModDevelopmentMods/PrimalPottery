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
        return sprite(Ids.of("minecraft", path));
    }

    /** White concrete for a known dye, so a vertex tint stays bright. Fired clay with no dye stays red terracotta. */
    public static Identifier clayTexture(@Nullable String dye) {
        if (dye != null && vanillaDye(dye)) {
            return Ids.of("minecraft", "block/white_concrete");
        }
        return Ids.of("minecraft", "block/red_terracotta");
    }

    /** Unfired clay. The pottery shape uses this instead of terracotta. */
    public static Identifier rawTexture() {
        return Ids.of("minecraft", "block/clay");
    }

    /** White when the dye is absent or unknown, so an undyed surface keeps its texture. */
    public static int dyeTint(@Nullable String dye) {
        Integer color = net.modcrafters.minecraft.primalpottery.item.PotteryItem.dyeColor(dye);
        return color == null ? 0xFFFFFF : color;
    }

    public static TextureAtlasSprite sprite(Identifier id) {
        //? if >=26.1 {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).getSprite(id);
        //?} else
        //return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(id);
    }

    private static boolean vanillaDye(String dye) {
        return switch (dye) {
            case "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
                    "purple", "blue", "brown", "green", "red", "black" -> true;
            default -> false;
        };
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
        write(mesh, null, buffer, pose, light, alpha);
    }

    public static void draw(Mesh mesh, VertexConsumer buffer, PoseStack.Pose pose, int light, Identifier texture) {
        draw(mesh, null, buffer, pose, light, texture, 0xFFFFFF);
    }

    public static void draw(Mesh mesh, VertexConsumer buffer, PoseStack.Pose pose, int light, Identifier texture, int color) {
        draw(mesh, null, buffer, pose, light, texture, color);
    }

    /** Draws each mesh with one sprite. The meshes are not merged. */
    public static void draw(@Nullable Mesh first, @Nullable Mesh second, VertexConsumer buffer, PoseStack.Pose pose, int light, Identifier texture) {
        draw(first, second, buffer, pose, light, texture, 0xFFFFFF);
    }

    /** Draws each mesh with one sprite, multiplied by {@code color}. The meshes are not merged. */
    public static void draw(@Nullable Mesh first, @Nullable Mesh second, VertexConsumer buffer, PoseStack.Pose pose, int light, Identifier texture, int color) {
        draw(first, second, buffer, pose, light, texture, color, false);
    }

    /**
     * Same as {@link #draw(Mesh, Mesh, VertexConsumer, PoseStack.Pose, int, Identifier, int)}.
     * When {@code shade} is set, each face is darkened the way a block item is, so a flat-lit icon still reads as a solid.
     */
    public static void draw(@Nullable Mesh first, @Nullable Mesh second, VertexConsumer buffer, PoseStack.Pose pose, int light, Identifier texture, int color, boolean shade) {
        TextureAtlasSprite sprite = sprite(texture);
        int red = (color >> 16) & 255;
        int green = (color >> 8) & 255;
        int blue = color & 255;
        if (first != null) {
            write(first, sprite, buffer, pose, light, red, green, blue, 255, shade);
        }
        if (second != null) {
            write(second, sprite, buffer, pose, light, red, green, blue, 255, shade);
        }
    }

    private static void write(Mesh mesh, @Nullable TextureAtlasSprite forced, VertexConsumer buffer, PoseStack.Pose pose, int light, int alpha) {
        write(mesh, forced, buffer, pose, light, 255, 255, 255, alpha, false);
    }

    private static void write(Mesh mesh, @Nullable TextureAtlasSprite forced, VertexConsumer buffer, PoseStack.Pose pose, int light, int red, int green, int blue, int alpha, boolean shade) {
        for (Mesh.Quad quad : mesh.quads()) {
            TextureAtlasSprite sprite = forced == null ? get(quad.texture) : forced;
            float face = shade ? faceShade(quad.nx, quad.ny, quad.nz) : 1.0F;
            int shadedRed = (int) (red * face);
            int shadedGreen = (int) (green * face);
            int shadedBlue = (int) (blue * face);
            for (int vertex = 0; vertex < 4; vertex++) {
                put(buffer, pose, quad.x[vertex], quad.y[vertex], quad.z[vertex], sampleU(sprite, quad.u[vertex]), sampleV(sprite, quad.v[vertex]), light, shadedRed, shadedGreen, shadedBlue, alpha, quad.nx, quad.ny, quad.nz);
            }
        }
    }

    /** Block-item brightness for the nearest axis. Up is full, down is half, north and south are 0.8, east and west are 0.6. */
    private static float faceShade(float nx, float ny, float nz) {
        float ax = Math.abs(nx);
        float ay = Math.abs(ny);
        float az = Math.abs(nz);
        if (ay >= ax && ay >= az) {
            return ny > 0 ? 1.0F : 0.5F;
        }
        if (az >= ax) {
            return 0.8F;
        }
        return 0.6F;
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
