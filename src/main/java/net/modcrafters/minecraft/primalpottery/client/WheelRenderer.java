package net.modcrafters.minecraft.primalpottery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
//? if >=26.1
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.modcrafters.minecraft.primalpottery.block.PotteryWheelBlockEntity;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryDisc;

//? if >=26.1 {
public final class WheelRenderer implements BlockEntityRenderer<PotteryWheelBlockEntity, WheelRenderer.State> {
//?} else {
/*public final class WheelRenderer implements BlockEntityRenderer<PotteryWheelBlockEntity> {
*///?}
    private static final Mesh STAND = stand();
    private static final Mesh SPINNING = spinning();

    //? if >=26.1 {
    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            PotteryWheelBlockEntity blockEntity,
            State state,
            float partialTick,
            net.minecraft.world.phys.Vec3 camera,
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
        state.angle = blockEntity.angle();
        state.hasClay = blockEntity.hasClay();
        state.progress = blockEntity.clayProgress();
        state.template = blockEntity.template();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            net.minecraft.client.renderer.SubmitNodeCollector collector,
            net.minecraft.client.renderer.state.level.CameraRenderState camera
    ) {
        var renderType = net.minecraft.client.renderer.rendertype.RenderTypes.entitySolid(Sprites.blocks());
        poseStack.pushPose();
        poseStack.scale(1 / 32.0F, (float) (PotteryWheelBlockEntity.BLOCK_HEIGHT / 32.0), 1 / 32.0F);
        Mesh pattern = state.template == null ? null : pattern(state.template);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Sprites.draw(STAND, buffer, pose, state.lightCoords);
            if (pattern != null) {
                Sprites.draw(pattern, buffer, pose, state.lightCoords);
            }
        });
        poseStack.translate(16, 0, 16);
        poseStack.rotate(Axis.YP, -state.angle);
        poseStack.translate(-16, 0, -16);
        Mesh clay = state.hasClay ? clay(Pottery.formed(state.template), state.progress) : null;
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Sprites.draw(SPINNING, buffer, pose, state.lightCoords);
            if (clay != null) {
                Sprites.draw(clay, buffer, pose, state.lightCoords);
            }
        });
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    public static final class State extends BlockEntityRenderState {
        public float angle;
        public boolean hasClay;
        public float progress;
        public Pottery template;
    }
    //?} else {
    /*@Override
    public void render(
            PotteryWheelBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffers,
            int light,
            int overlay
    ) {
        poseStack.pushPose();
        poseStack.scale(1 / 32.0F, (float) (PotteryWheelBlockEntity.BLOCK_HEIGHT / 32.0), 1 / 32.0F);
        var buffer = buffers.getBuffer(net.minecraft.client.renderer.RenderType.entitySolid(Sprites.blocks()));
        var pose = poseStack.last();
        Sprites.draw(STAND, buffer, pose, light);
        if (blockEntity.template() != null) {
            Sprites.draw(pattern(blockEntity.template()), buffer, pose, light);
        }
        poseStack.pushPose();
        poseStack.translate(16, 0, 16);
        poseStack.mulPose(Axis.YP.rotation(-blockEntity.angle()));
        poseStack.translate(-16, 0, -16);
        pose = poseStack.last();
        Sprites.draw(SPINNING, buffer, pose, light);
        if (blockEntity.hasClay()) {
            Sprites.draw(clay(blockEntity.formed(), blockEntity.clayProgress()), buffer, pose, light);
        }
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(PotteryWheelBlockEntity blockEntity) {
        return true;
    }
    *///?}

    private static Mesh stand() {
        Mesh mesh = new Mesh();
        double leg = 4;
        double pad = 1;
        double[] edges = {pad, 32 - leg - pad};
        for (double x : edges) {
            for (double z : edges) {
                mesh.box(x, 0, z, x + leg, 20, z + leg, Mesh.Texture.PLANKS);
            }
        }
        mesh.box(0, 20, 0, 32, 24, 32, Mesh.Texture.PLANKS);
        mesh.boxRotated(15, 2, 0, 17, 4, 32, Math.PI / 4, Mesh.Texture.PLANKS);
        mesh.boxRotated(15, 2.001, 0, 17, 4.001, 32, -Math.PI / 4, Mesh.Texture.PLANKS);
        return mesh;
    }

    private static Mesh spinning() {
        Mesh mesh = new Mesh();
        mesh.cylinder(16, 5, 16, 3, 12, 0, Mesh.Texture.STONE);
        mesh.cylinder(16, 26, 16, 2, 6.5, 0, Mesh.Texture.STONE);
        mesh.cylinder(16, 0.5, 16, 25.5, 1, 0, Mesh.Texture.LOG);
        return mesh;
    }

    private static Mesh pattern(Pottery pottery) {
        Mesh mesh = new Mesh();
        mesh.board(30, 24, 2, 2, pottery.discs(), Mesh.Texture.PLANKS);
        mesh.fitSprite();
        return mesh;
    }

    private static Mesh clay(Pottery pottery, float progress) {
        List<PotteryDisc> formed = new ArrayList<>();
        for (PotteryDisc disc : pottery.discs()) {
            double radius = disc.radius() + (4.0 - disc.radius()) * (1.0 - progress);
            double inner = disc.innerRadius() * progress;
            formed.add(new PotteryDisc(disc.height(), radius, inner));
        }
        Mesh mesh = new Mesh();
        mesh.vessel(16, 28, 16, formed, Mesh.Texture.CLAY);
        mesh.fitSprite();
        return mesh;
    }
}
