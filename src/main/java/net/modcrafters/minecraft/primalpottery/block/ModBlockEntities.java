package net.modcrafters.minecraft.primalpottery.block;

import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.resources.Identifier;
import net.modcrafters.minecraft.primalpottery.Ids;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    //? if >=26.1 {
    public static final BlockEntityType<PotteryWheelBlockEntity> WHEEL = new BlockEntityType<>(PotteryWheelBlockEntity::new, Set.of(ModBlocks.WHEEL));
    public static final BlockEntityType<PotteryBlockEntity> POTTERY = new BlockEntityType<>(PotteryBlockEntity::new, Set.of(ModBlocks.POTTERY));
    //?} else {
    /*public static final BlockEntityType<PotteryWheelBlockEntity> WHEEL = BlockEntityType.Builder.of(PotteryWheelBlockEntity::new, ModBlocks.WHEEL).build(null);
    public static final BlockEntityType<PotteryBlockEntity> POTTERY = BlockEntityType.Builder.of(PotteryBlockEntity::new, ModBlocks.POTTERY).build(null);
    *///?}

    private ModBlockEntities() {
    }

    public static void register(BiConsumer<Identifier, BlockEntityType<?>> out) {
        out.accept(Ids.of("pottery_wheel"), WHEEL);
        out.accept(Ids.of("pottery"), POTTERY);
    }
}
