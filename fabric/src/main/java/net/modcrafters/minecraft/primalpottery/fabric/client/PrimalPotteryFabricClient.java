package net.modcrafters.minecraft.primalpottery.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.modcrafters.minecraft.primalpottery.block.ModBlockEntities;
import net.modcrafters.minecraft.primalpottery.client.PotteryPreview;
import net.modcrafters.minecraft.primalpottery.network.TemplatePayload;
import net.modcrafters.minecraft.primalpottery.network.TemplateSync;
import net.modcrafters.minecraft.primalpottery.client.PotteryRenderer;
import net.modcrafters.minecraft.primalpottery.client.WheelRenderer;
import net.modcrafters.minecraft.primalpottery.pottery.Placement;

public class PrimalPotteryFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TemplateSync.sender = (mainHand, discs) -> ClientPlayNetworking.send(new TemplatePayload(mainHand, discs));
        PictureInPictureRendererRegistry.register(context -> new PotteryPreview.Renderer());
        PotteryPreview.SINK = (graphics, state) -> graphics.guiRenderState.addPicturesInPictureState(state);
        BlockEntityRendererRegistry.register(ModBlockEntities.WHEEL, context -> new WheelRenderer());
        BlockEntityRendererRegistry.register(ModBlockEntities.POTTERY, context -> new PotteryRenderer());
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outline) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) {
                return true;
            }
            HitResult hit = minecraft.hitResult;
            if (!(hit instanceof BlockHitResult blockHit) || !blockHit.getBlockPos().equals(outline.pos())) {
                return true;
            }
            AABB box = Placement.outline(minecraft.level, minecraft.player, blockHit);
            if (box == null) {
                return true;
            }
            var pose = context.poseStack();
            var camera = context.levelState().cameraRenderState.pos;
            pose.pushPose();
            pose.translate(box.minX - camera.x, box.minY - camera.y, box.minZ - camera.z);
            context.submitNodeCollector().submitShapeOutline(
                    pose,
                    Shapes.box(0, 0, 0, box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ),
                    RenderTypes.lines(),
                    0x6B000000,
                    2.0F,
                    false
            );
            pose.popPose();
            return false;
        });
    }
}
