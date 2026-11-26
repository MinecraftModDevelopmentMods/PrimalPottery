package net.modcrafters.minecraft.primalpottery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//? if >=26.1
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.modcrafters.minecraft.primalpottery.block.PotteryBlockEntity;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryDisc;
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
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            net.minecraft.client.renderer.SubmitNodeCollector collector,
            net.minecraft.client.renderer.state.level.CameraRenderState camera
    ) {
        Mesh mesh = clay(state.slots, state.lids);
        List<FluidDraw> draws = fluids(state.slots, state.contents);
        boolean solidFluid = has(draws, true);
        boolean translucent = has(draws, false);
        if (mesh.quads().isEmpty() && !solidFluid && !translucent) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(1 / 32.0F, 1 / 32.0F, 1 / 32.0F);
        if (!mesh.quads().isEmpty() || solidFluid) {
            collector.submitCustomGeometry(
                    poseStack,
                    net.minecraft.client.renderer.rendertype.RenderTypes.entitySolid(Sprites.blocks()),
                    (pose, buffer) -> {
                        if (!mesh.quads().isEmpty()) {
                            Sprites.draw(mesh, buffer, pose, state.lightCoords);
                        }
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
        Mesh mesh = clay(blockEntity.slots(), blockEntity.lids());
        List<FluidDraw> draws = fluids(blockEntity.slots(), blockEntity.contents());
        boolean solidFluid = has(draws, true);
        boolean translucent = has(draws, false);
        if (mesh.quads().isEmpty() && !solidFluid && !translucent) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(1 / 32.0F, 1 / 32.0F, 1 / 32.0F);
        if (!mesh.quads().isEmpty() || solidFluid) {
            var buffer = buffers.getBuffer(net.minecraft.client.renderer.RenderType.entitySolid(Sprites.blocks()));
            if (!mesh.quads().isEmpty()) {
                Sprites.draw(mesh, buffer, poseStack.last(), light);
            }
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

    private static Mesh clay(Pottery[] slots, boolean[] lids) {
        Mesh mesh = new Mesh();
        for (int index = 0; index < slots.length; index++) {
            Pottery pottery = slots[index];
            if (pottery == null) {
                continue;
            }
            boolean closed = lids != null && index < lids.length && lids[index];
            Mesh pot = new Mesh();
            placed(index, pot, pottery, closed, false);
            mesh.add(pot);
        }
        return mesh;
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
            placed(index, body, pottery, false, true);
            if (body.quads().isEmpty()) {
                continue;
            }
            draws.add(new FluidDraw(body, sprite, Sprites.fluidColor(held.fluid()), Sprites.fluidSolid(held.fluid())));
        }
        return draws;
    }

    private static void placed(int index, Mesh mesh, Pottery pottery, boolean closed, boolean fluid) {
        AABB box = PotteryBlockEntity.box(index);
        double centerX = (box.minX + box.maxX) / 2.0 * 32.0;
        double centerZ = (box.minZ + box.maxZ) / 2.0 * 32.0;
        double base = box.minY * 32.0;
        List<PotteryDisc> discs = pottery.discs();
        if (fluid) {
            mesh.cavity(centerX, base, centerZ, discs);
            return;
        }
        mesh.vessel(centerX, base, centerZ, discs, Mesh.Texture.CLAY);
        if (closed && pottery.cover() != null) {
            mesh.cover(centerX, base, centerZ, pottery.cover(), Mesh.Texture.CLAY);
        }
        mesh.fitSprite();
    }
}
