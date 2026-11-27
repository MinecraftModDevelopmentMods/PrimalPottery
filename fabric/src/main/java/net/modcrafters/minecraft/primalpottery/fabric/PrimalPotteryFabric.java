package net.modcrafters.minecraft.primalpottery.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.modcrafters.minecraft.primalpottery.PrimalPottery;
import net.modcrafters.minecraft.primalpottery.block.ModBlockEntities;
import net.modcrafters.minecraft.primalpottery.block.ModBlocks;
import net.modcrafters.minecraft.primalpottery.block.PotteryBlock;
import net.modcrafters.minecraft.primalpottery.item.ModItems;
import net.modcrafters.minecraft.primalpottery.item.ModTab;
import net.modcrafters.minecraft.primalpottery.recipe.ModRecipes;
import net.modcrafters.minecraft.primalpottery.network.LidPayload;
import net.modcrafters.minecraft.primalpottery.network.LidSync;
import net.modcrafters.minecraft.primalpottery.network.PlacedLidPayload;
import net.modcrafters.minecraft.primalpottery.network.PlacedLidSync;
import net.modcrafters.minecraft.primalpottery.network.TemplatePayload;
import net.modcrafters.minecraft.primalpottery.network.TemplateSync;

public class PrimalPotteryFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModBlocks.register((id, block) -> Registry.register(BuiltInRegistries.BLOCK, id, block));
        ModBlockEntities.register((id, type) -> Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, type));
        ModItems.register((id, item) -> Registry.register(BuiltInRegistries.ITEM, id, item));
        ModRecipes.register((id, serializer) -> Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id, serializer));
        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                ModTab.ID,
                ModTab.titled(FabricCreativeModeTab.builder())
                        .displayItems((parameters, output) -> ModItems.tabContents().forEach(output::accept))
                        .build()
        );
        PayloadTypeRegistry.serverboundPlay().register(TemplatePayload.TYPE, TemplatePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LidPayload.TYPE, LidPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PlacedLidPayload.TYPE, PlacedLidPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TemplatePayload.TYPE, (payload, context) ->
                TemplateSync.apply(context.player(), payload.mainHand(), payload.discs()));
        ServerPlayNetworking.registerGlobalReceiver(LidPayload.TYPE, (payload, context) ->
                LidSync.apply(context.player(), payload.mainHand(), payload.closed()));
        ServerPlayNetworking.registerGlobalReceiver(PlacedLidPayload.TYPE, (payload, context) ->
                PlacedLidSync.apply(context.player(), payload.pos(), payload.slot(), payload.closed()));
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> {
            if (!(state.getBlock() instanceof PotteryBlock)) {
                return true;
            }
            PotteryBlock.breakFocused(level, pos, player);
            return false;
        });
        PrimalPottery.init();
    }
}
