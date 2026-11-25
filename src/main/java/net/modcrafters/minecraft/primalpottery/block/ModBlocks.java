package net.modcrafters.minecraft.primalpottery.block;

import java.util.function.BiConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.modcrafters.minecraft.primalpottery.Ids;

public final class ModBlocks {
    public static final ResourceKey<Block> WHEEL_ID = blockId("pottery_wheel");
    public static final ResourceKey<Block> POTTERY_ID = blockId("pottery");

    public static final PotteryWheelBlock WHEEL = new PotteryWheelBlock(properties(WHEEL_ID)
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.0F)
            .noOcclusion()
            //? if >=26.1 {
            .pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE));
            //?} else {
            /*.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
            *///?}
    public static final PotteryBlock POTTERY = new PotteryBlock(properties(POTTERY_ID)
            .mapColor(MapColor.TERRACOTTA_RED)
            .sound(SoundType.DECORATED_POT)
            .strength(-1.0F, 3600000.0F)
            .noOcclusion());

    private ModBlocks() {
    }

    public static void register(BiConsumer<Identifier, Block> out) {
        out.accept(WHEEL_ID.identifier(), WHEEL);
        out.accept(POTTERY_ID.identifier(), POTTERY);
    }

    private static ResourceKey<Block> blockId(String path) {
        return ResourceKey.create(Registries.BLOCK, Ids.of(path));
    }

    private static BlockBehaviour.Properties properties(ResourceKey<Block> id) {
        //? if >=1.21.2 {
        return BlockBehaviour.Properties.of().setId(id);
        //?} else
        //return BlockBehaviour.Properties.of();
    }
}
