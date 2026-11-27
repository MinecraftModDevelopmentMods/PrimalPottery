package net.modcrafters.minecraft.primalpottery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
//? if >=26.1
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.modcrafters.minecraft.primalpottery.block.PotteryBlockEntity;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import net.modcrafters.minecraft.primalpottery.pottery.VesselContents;
import net.minecraft.world.phys.AABB;

//? if >=26.1 {
public final class PotteryRenderer implements BlockEntityRenderer<PotteryBlockEntity, PotteryRenderer.State> {
//?} else {
/*public final class PotteryRenderer implements BlockEntityRenderer<PotteryBlockEntity> {
*///?}

    //? if >=26.1 {
    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            PotteryBlockEntity blockEntity,
            State state,
            float partialTick,
            net.minecraft.world.phys.Vec3 camera,
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
        state.slots = blockEntity.slots();
        state.contents = blockEntity.contents();
        state.lids = blockEntity.lids();
        state.dyes = blockEntity.dyes();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            net.minecraft.client.renderer.SubmitNodeCollector collector,
            net.minecraft.client.renderer.state.level.CameraRenderState camera
    ) {
        List<FluidDraw> draws = fluids(state.slots, state.contents);
        boolean solidFluid = has(draws, true);
        boolean translucent = has(draws, false);
        if (!occupied(state.slots) && !solidFluid && !translucent) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(1 / 32.0F, 1 / 32.0F, 1 / 32.0F);
        if (occupied(state.slots) || solidFluid) {
            collector.submitCustomGeometry(
                    poseStack,
                    net.minecraft.client.renderer.rendertype.RenderTypes.entitySolid(Sprites.blocks()),
                    (pose, buffer) -> {
                        drawClay(state.slots, state.lids, state.dyes, buffer, pose, state.lightCoords);
                        drawFluids(draws, buffer, pose, state.lightCoords, true);
                    }
            );
        }
        if (translucent) {
            collector.submitCustomGeometry(
                    poseStack,
                    net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(Sprites.blocks()),
                    (pose, buffer) -> drawFluids(draws, buffer, pose, state.lightCoords, false)
            );
        }
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public Pottery[] slots = new Pottery[PotteryBlockEntity.SLOTS];
        public VesselContents[] contents = new VesselContents[PotteryBlockEntity.SLOTS];
        public boolean[] lids = new boolean[PotteryBlockEntity.SLOTS];
        public String[] dyes = new String[PotteryBlockEntity.SLOTS];
    }
    //?} else {
    /*@Override
    public void render(
            PotteryBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        List<FluidDraw> draws = fluids(blockEntity.slots(), blockEntity.contents());
        boolean solidFluid = has(draws, true);
        boolean translucent = has(draws, false);
        if (!occupied(blockEntity.slots()) && !solidFluid && !translucent) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(1 / 32.0F, 1 / 32.0F, 1 / 32.0F);
        if (occupied(blockEntity.slots()) || solidFluid) {
            var buffer = buffers.getBuffer(net.minecraft.client.renderer.RenderType.entitySolid(Sprites.blocks()));
            drawClay(blockEntity.slots(), blockEntity.lids(), blockEntity.dyes(), buffer, poseStack.last(), light);
            drawFluids(draws, buffer, poseStack.last(), light, true);
        }
        if (translucent) {
            var buffer = buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(Sprites.blocks()));
            drawFluids(draws, buffer, poseStack.last(), light, false);
        }
        poseStack.popPose();
    }
    *///?}

    private static boolean has(List<FluidDraw> draws, boolean solid) {
        for (FluidDraw draw : draws) {
            if (draw.solid == solid) {
                return true;
            }
        }
        return false;
    }

    private static void drawFluids(
            List<FluidDraw> draws, com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose, int light, boolean solid
    ) {
        for (FluidDraw draw : draws) {
            if (draw.solid == solid) {
                Sprites.drawFluid(draw.mesh, buffer, pose, light, draw.sprite, draw.color);
            }
        }
    }

    private record FluidDraw(Mesh mesh, TextureAtlasSprite sprite, int color, boolean solid) {
    }

    private static boolean occupied(Pottery[] slots) {
        if (slots == null) {
            return false;
        }
        for (Pottery pottery : slots) {
            if (pottery != null) {
                return true;
            }
        }
        return false;
    }

    private static void drawClay(
            Pottery[] slots, boolean[] lids, String[] dyes, com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose, int light
    ) {
        if (slots == null) {
            return;
        }
        for (int index = 0; index < slots.length; index++) {
            Pottery pottery = slots[index];
            if (pottery == null) {
                continue;
            }
            String dye = dyes != null && index < dyes.length ? dyes[index] : null;
            Identifier texture = Sprites.clayTexture(dye);
            int color = Sprites.dyeTint(dye);
            AABB box = PotteryBlockEntity.box(index);
            double x = box.getCenter().x * 32;
            double z = box.getCenter().z * 32;
            Mesh vessel = new Mesh();
            vessel.vessel(x, 0, z, pottery.discs(), Mesh.Texture.CLAY);
            vessel.fitSprite();
            boolean closed = lids != null && index < lids.length && lids[index];
            Pottery.Cover cover = closed ? pottery.cover() : null;
            Mesh lid = null;
            if (cover != null) {
                lid = new Mesh();
                lid.cover(x, 0, z, cover, Mesh.Texture.CLAY);
                lid.fitSprite();
            }
            Sprites.draw(vessel, lid, buffer, pose, light, texture, color);
        }
    }

    private static List<FluidDraw> fluids(Pottery[] slots, VesselContents[] contents) {
        List<FluidDraw> draws = new ArrayList<>();
        if (contents == null) {
            return draws;
        }
        for (int index = 0; index < slots.length && index < contents.length; index++) {
            Pottery pottery = slots[index];
            VesselContents held = contents[index];
            if (pottery == null || held == null || held.isEmpty()) {
                continue;
            }
            TextureAtlasSprite sprite = Sprites.fluid(held.fluid());
            if (sprite == null) {
                continue;
            }
            Mesh body = new Mesh();
            AABB box = PotteryBlockEntity.box(index);
            body.cavity(box.getCenter().x * 32, 0, box.getCenter().z * 32, pottery.discs(), held.amount());
            if (body.quads().isEmpty()) {
                continue;
            }
            draws.add(new FluidDraw(body, sprite, Sprites.fluidColor(held.fluid()), Sprites.fluidSolid(held.fluid())));
        }
        return draws;
    }

}
