package net.modcrafters.minecraft.primalpottery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
//? if >=26.1
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.modcrafters.minecraft.primalpottery.block.PotteryBlockEntity;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryDisc;
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
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            net.minecraft.client.renderer.SubmitNodeCollector collector,
            net.minecraft.client.renderer.state.level.CameraRenderState camera
    ) {
        Mesh mesh = pots(state.slots);
        if (mesh.quads().isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(1 / 32.0F, 1 / 32.0F, 1 / 32.0F);
        collector.submitCustomGeometry(
                poseStack,
                net.minecraft.client.renderer.rendertype.RenderTypes.entitySolid(Sprites.blocks()),
                (pose, buffer) -> Sprites.draw(mesh, buffer, pose, state.lightCoords)
        );
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public Pottery[] slots = new Pottery[PotteryBlockEntity.SLOTS];
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
        Mesh mesh = pots(blockEntity.slots());
        if (mesh.quads().isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(1 / 32.0F, 1 / 32.0F, 1 / 32.0F);
        Sprites.draw(mesh, buffers.getBuffer(net.minecraft.client.renderer.RenderType.entitySolid(Sprites.blocks())), poseStack.last(), light);
        poseStack.popPose();
    }
    *///?}

    private static Mesh pots(Pottery[] slots) {
        Mesh mesh = new Mesh();
        for (int index = 0; index < slots.length; index++) {
            Pottery pottery = slots[index];
            if (pottery == null) {
                continue;
            }
            AABB box = PotteryBlockEntity.box(index);
            double centerX = (box.minX + box.maxX) / 2.0 * 32.0;
            double centerZ = (box.minZ + box.maxZ) / 2.0 * 32.0;
            List<PotteryDisc> discs = new ArrayList<>();
            for (PotteryDisc disc : pottery.discs()) {
                discs.add(new PotteryDisc(disc.height(), disc.radius(), Math.max(0, disc.radius() - 1)));
            }
            Mesh pot = new Mesh();
            pot.vessel(centerX, box.minY * 32.0, centerZ, discs, Mesh.Texture.CLAY);
            pot.fitSprite();
            mesh.add(pot);
        }
        return mesh;
    }
}
