package net.modcrafters.minecraft.primalpottery.neoforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.modcrafters.minecraft.primalpottery.block.ModBlockEntities;
import net.modcrafters.minecraft.primalpottery.client.PotteryRenderer;
import net.modcrafters.minecraft.primalpottery.client.WheelRenderer;
import net.modcrafters.minecraft.primalpottery.network.LidPayload;
import net.modcrafters.minecraft.primalpottery.network.LidSync;
import net.modcrafters.minecraft.primalpottery.network.PlacedLidPayload;
import net.modcrafters.minecraft.primalpottery.network.PlacedLidSync;
import net.modcrafters.minecraft.primalpottery.network.TemplatePayload;
import net.modcrafters.minecraft.primalpottery.network.TemplateSync;
//? if >=26.1
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
//? if <26.1
//import net.neoforged.neoforge.network.PacketDistributor;
import net.modcrafters.minecraft.primalpottery.pottery.Placement;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
//? if >=26.1
import net.modcrafters.minecraft.primalpottery.client.PotteryPreview;
//? if >=26.1
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
//? if >=26.1
import net.minecraft.client.renderer.rendertype.RenderTypes;
//? if >=26.1
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
//? if <26.1
//import net.minecraft.client.renderer.LevelRenderer;
//? if <26.1
//import net.minecraft.client.renderer.RenderType;
//? if <26.1
//import net.neoforged.neoforge.client.event.RenderHighlightEvent;

public final class PrimalPotteryNeoForgeClient {
    private PrimalPotteryNeoForgeClient() {
    }

    public static void register(IEventBus modBus) {
        TemplateSync.sender = (mainHand, discs) -> {
            //? if >=26.1 {
            ClientPacketDistributor.sendToServer(new TemplatePayload(mainHand, discs));
            //?} else {
            /*PacketDistributor.sendToServer(new TemplatePayload(mainHand, discs));
            *///?}
        };
        LidSync.sender = (mainHand, closed) -> {
            //? if >=26.1 {
            ClientPacketDistributor.sendToServer(new LidPayload(mainHand, closed));
            //?} else {
            /*PacketDistributor.sendToServer(new LidPayload(mainHand, closed));
            *///?}
        };
        PlacedLidSync.sender = (pos, slot, closed) -> {
            //? if >=26.1 {
            ClientPacketDistributor.sendToServer(new PlacedLidPayload(pos, slot, closed));
            //?} else {
            /*PacketDistributor.sendToServer(new PlacedLidPayload(pos, slot, closed));
            *///?}
        };
        modBus.addListener(PrimalPotteryNeoForgeClient::renderers);
        //? if >=26.1 {
        modBus.addListener(PrimalPotteryNeoForgeClient::pictures);
        PotteryPreview.SINK = (graphics, state) -> graphics.submitPictureInPictureRenderState(state);
        //?}
    }

    public static void registerGame(IEventBus gameBus) {
        //? if >=26.1 {
        gameBus.addListener(PrimalPotteryNeoForgeClient::outline);
        //?} else {
        /*gameBus.addListener(PrimalPotteryNeoForgeClient::outlineOld);
        *///?}
    }

    //? if >=26.1 {
    private static void pictures(RegisterPictureInPictureRenderersEvent event) {
        event.register(PotteryPreview.State.class, PotteryPreview.Renderer::new);
    }
    //?}

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.WHEEL, context -> new WheelRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.POTTERY, context -> new PotteryRenderer());
    }

    //? if >=26.1 {
    private static void outline(ExtractBlockOutlineRenderStateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        AABB box = Placement.outline(event.getLevel(), minecraft.player, event.getHitResult());
        if (box == null || minecraft.player == null) {
            return;
        }
        event.addCustomRenderer((state, collector, poseStack, levelState) -> {
            var camera = levelState.cameraRenderState.pos;
            poseStack.pushPose();
            poseStack.translate(box.minX - camera.x, box.minY - camera.y, box.minZ - camera.z);
            collector.submitShapeOutline(
                    poseStack,
                    Shapes.box(0, 0, 0, box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ),
                    RenderTypes.lines(),
                    0x6B000000,
                    2.0F,
                    false
            );
            poseStack.popPose();
            return true;
        });
    }
    //?} else {
    /*private static void outlineOld(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        AABB box = Placement.outline(minecraft.level, minecraft.player, event.getTarget());
        if (box == null) {
            return;
        }
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        LevelRenderer.renderLineBox(pose, event.getMultiBufferSource().getBuffer(RenderType.lines()), box, 0, 0, 0, 0.42F);
        pose.popPose();
        event.setCanceled(true);
    }
    *///?}
}
