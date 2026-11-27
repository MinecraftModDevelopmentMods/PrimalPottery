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
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
//? if >=26.1
import net.modcrafters.minecraft.primalpottery.client.PotterySpecialRenderer;
//? if >=26.1
import net.modcrafters.minecraft.primalpottery.Ids;
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
        //? if <26.1 {
        /*modBus.addListener(PrimalPotteryNeoForgeClient::colors);
        *///?}
        //? if >=26.1 {
        modBus.addListener(PrimalPotteryNeoForgeClient::pictures);
        modBus.addListener(PrimalPotteryNeoForgeClient::specialModels);
        PotteryPreview.SINK = (graphics, state) -> graphics.submitPictureInPictureRenderState(state);
        //?}
        //? if >=1.21 && <26.1 {
        /*modBus.addListener(PrimalPotteryNeoForgeClient::items);
        *///?}
        //? if <1.21 {
        /*bindItems();
        *///?}
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

    private static void specialModels(RegisterSpecialModelRendererEvent event) {
        event.register(Ids.of("pottery"), PotterySpecialRenderer.Unbaked.MAP_CODEC);
    }
    //?}

    //? if <26.1 {
    /*private static net.neoforged.neoforge.client.extensions.common.IClientItemExtensions extensions() {
        return new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    var minecraft = net.minecraft.client.Minecraft.getInstance();
                    this.renderer = new net.modcrafters.minecraft.primalpottery.client.PotteryItemRenderer(
                            minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                return this.renderer;
            }
        };
    }
    *///?}

    //? if >=1.21 && <26.1 {
    /*private static void items(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(
                extensions(),
                net.modcrafters.minecraft.primalpottery.item.ModItems.POTTERY,
                net.modcrafters.minecraft.primalpottery.item.ModItems.RAW_POTTERY
        );
    }
    *///?}

    //? if <1.21 {
    /*private static void bindItems() {
        var extensions = extensions();
        net.modcrafters.minecraft.primalpottery.item.ModItems.POTTERY.renderProperties = extensions;
        net.modcrafters.minecraft.primalpottery.item.ModItems.RAW_POTTERY.renderProperties = extensions;
    }
    *///?}

    //? if <26.1 {
    /*private static void colors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> {
                    Integer color = net.modcrafters.minecraft.primalpottery.item.PotteryItem.dyeColor(
                            net.modcrafters.minecraft.primalpottery.item.PotteryItem.dye(stack)
                    );
                    return color == null ? -1 : 0xFF000000 | color;
                },
                net.modcrafters.minecraft.primalpottery.item.ModItems.RAW_POTTERY,
                net.modcrafters.minecraft.primalpottery.item.ModItems.POTTERY
        );
    }
    *///?}

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
