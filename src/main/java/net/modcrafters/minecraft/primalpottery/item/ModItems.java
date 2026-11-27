package net.modcrafters.minecraft.primalpottery.item;

import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.modcrafters.minecraft.primalpottery.Ids;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.modcrafters.minecraft.primalpottery.block.ModBlocks;

public final class ModItems {
    public static final ResourceKey<Item> WHEEL_ID = itemId("pottery_wheel");
    public static final ResourceKey<Item> POTTERY_ID = itemId("pottery");
    public static final ResourceKey<Item> RAW_POTTERY_ID = itemId("raw_pottery");
    public static final ResourceKey<Item> BLUEPRINT_ID = itemId("pottery_template");
    public static final ResourceKey<Item> RAW_TEMPLATE_ID = itemId("raw_pottery_template");

    public static final Item WHEEL = new BlockItem(ModBlocks.WHEEL, properties(WHEEL_ID));
    public static final Item POTTERY = new PotteryItem(properties(POTTERY_ID).stacksTo(1));
    public static final Item RAW_POTTERY = new RawPotteryItem(properties(RAW_POTTERY_ID).stacksTo(1));
    public static final Item BLUEPRINT = new TemplateItem(properties(BLUEPRINT_ID));
    public static final Item RAW_TEMPLATE = new TemplateItem(properties(RAW_TEMPLATE_ID));

    private ModItems() {
    }

    public static void register(BiConsumer<Identifier, Item> out) {
        out.accept(WHEEL_ID.identifier(), WHEEL);
        out.accept(POTTERY_ID.identifier(), POTTERY);
        out.accept(RAW_POTTERY_ID.identifier(), RAW_POTTERY);
        out.accept(BLUEPRINT_ID.identifier(), BLUEPRINT);
        out.accept(RAW_TEMPLATE_ID.identifier(), RAW_TEMPLATE);
    }

    public static List<ItemStack> tabContents() {
        return List.of(new ItemStack(WHEEL), new ItemStack(POTTERY), new ItemStack(RAW_TEMPLATE), new ItemStack(BLUEPRINT));
    }

    public static ItemStack tabIcon() {
        return new ItemStack(WHEEL);
    }

    private static Item.Properties properties(ResourceKey<Item> id) {
        //? if >=1.21.2 {
        return new Item.Properties().setId(id);
        //?} else
        //return new Item.Properties();
    }

    private static ResourceKey<Item> itemId(String path) {
        return ResourceKey.create(Registries.ITEM, Ids.of(path));
    }
}
