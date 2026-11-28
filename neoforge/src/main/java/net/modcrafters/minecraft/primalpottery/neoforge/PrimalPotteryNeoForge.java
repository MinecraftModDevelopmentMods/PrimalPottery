package net.modcrafters.minecraft.primalpottery.neoforge;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.modcrafters.minecraft.primalpottery.Constants;
import net.modcrafters.minecraft.primalpottery.PrimalPottery;
import net.modcrafters.minecraft.primalpottery.block.ModBlockEntities;
import net.modcrafters.minecraft.primalpottery.block.ModBlocks;
import net.modcrafters.minecraft.primalpottery.item.ModItems;
import net.modcrafters.minecraft.primalpottery.item.ModTab;
import net.modcrafters.minecraft.primalpottery.recipe.ModRecipes;
import net.modcrafters.minecraft.primalpottery.neoforge.client.PrimalPotteryNeoForgeClient;
//? if >=26.1
import net.modcrafters.minecraft.primalpottery.neoforge.PotteryGameTests;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.modcrafters.minecraft.primalpottery.network.LidPayload;
import net.modcrafters.minecraft.primalpottery.network.LidSync;
import net.modcrafters.minecraft.primalpottery.network.PlacedLidPayload;
import net.modcrafters.minecraft.primalpottery.network.PlacedLidSync;
import net.modcrafters.minecraft.primalpottery.network.TemplatePayload;
import net.modcrafters.minecraft.primalpottery.network.TemplateSync;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Constants.MOD_ID)
public class PrimalPotteryNeoForge {
    public PrimalPotteryNeoForge(IEventBus modBus) {
        modBus.addListener(PrimalPotteryNeoForge::registerContent);
        modBus.addListener(PrimalPotteryNeoForge::registerPayloads);
        //? if >=1.21.9 {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
        //?} else
        //if (FMLEnvironment.dist == Dist.CLIENT) {
            PrimalPotteryNeoForgeClient.register(modBus);
            PrimalPotteryNeoForgeClient.registerGame(NeoForge.EVENT_BUS);
        }
        //? if >=26.1
        modBus.addListener(PotteryGameTests::registerTests);
        PrimalPottery.init();
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(Constants.MOD_ID)
                .playToServer(
                        TemplatePayload.TYPE,
                        TemplatePayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() ->
                                TemplateSync.apply(context.player(), payload.mainHand(), payload.discs()))
                )
                .playToServer(
                        LidPayload.TYPE,
                        LidPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() ->
                                LidSync.apply(context.player(), payload.mainHand(), payload.closed()))
                )
                .playToServer(
                        PlacedLidPayload.TYPE,
                        PlacedLidPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() ->
                                PlacedLidSync.apply(context.player(), payload.pos(), payload.slot(), payload.closed()))
                );
    }

    private static void registerContent(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> ModBlocks.register(helper::register));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> ModBlockEntities.register(helper::register));
        event.register(Registries.ITEM, helper -> ModItems.register(helper::register));
        event.register(Registries.RECIPE_SERIALIZER, helper -> ModRecipes.register(helper::register));
        //? if >=26.1 {
        event.register(net.minecraft.core.registries.Registries.TEST_INSTANCE_TYPE, helper -> {
            helper.register(net.modcrafters.minecraft.primalpottery.Ids.of("wheel"), PotteryGameTests.codec());
            helper.register(net.modcrafters.minecraft.primalpottery.Ids.of("vessels"), PotteryGameTests.vesselsCodec());
        });
        //?}
        event.register(Registries.CREATIVE_MODE_TAB, ModTab.ID.identifier(), () -> ModTab.titled(CreativeModeTab.builder())
                .displayItems((parameters, output) -> ModItems.tabContents().forEach(output::accept))
                .build());
    }
}
