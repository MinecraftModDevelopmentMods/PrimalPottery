package net.modcrafters.minecraft.primalpottery.recipe;

import com.mojang.serialization.MapCodec;
import java.util.function.IntFunction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.modcrafters.minecraft.primalpottery.item.ModItems;
import net.modcrafters.minecraft.primalpottery.item.PotteryItem;
import org.jspecify.annotations.Nullable;
//? if >=1.21
import net.minecraft.world.item.crafting.CraftingInput;

/** One undyed raw pot and one vanilla dye. The grid size is not read here. */
public final class PaintPotteryRecipe extends CustomRecipe {
    public static final PaintPotteryRecipe INSTANCE = new PaintPotteryRecipe();
    //? if >=26.1 {
    public static final RecipeSerializer<PaintPotteryRecipe> SERIALIZER = new RecipeSerializer<>(MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));
    //?} else {
    /*public static final RecipeSerializer<PaintPotteryRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<PaintPotteryRecipe> codec() {
            return MapCodec.unit(INSTANCE);
        }

        @Override
        public StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, PaintPotteryRecipe> streamCodec() {
            return StreamCodec.unit(INSTANCE);
        }
    };
    *///?}

    private PaintPotteryRecipe() {
        //? if <26.1
        //super(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
    }

    //? if >=26.1 {
    @Override
    public boolean matches(CraftingInput input, Level level) {
        return this.matches(input.size(), input::getItem);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return this.assemble(input.size(), input::getItem);
    }
    //?} elif >=1.21 {
    /*@Override
    public boolean matches(CraftingInput input, Level level) {
        return this.matches(input.size(), input::getItem);
    }

    @Override
    public ItemStack assemble(CraftingInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return this.assemble(input.size(), input::getItem);
    }
    *///?} else {
    /*@Override
    public boolean matches(net.minecraft.world.inventory.CraftingContainer input, Level level) {
        return this.matches(input.getContainerSize(), input::getItem);
    }

    @Override
    public ItemStack assemble(net.minecraft.world.inventory.CraftingContainer input, net.minecraft.core.HolderLookup.Provider registries) {
        return this.assemble(input.getContainerSize(), input::getItem);
    }
    *///?}

    @Override
    public RecipeSerializer<PaintPotteryRecipe> getSerializer() {
        return SERIALIZER;
    }

    //? if <26.1 {
    /*@Override
    public boolean canCraftInDimensions(int width, int height) {
        return (width == 2 && height == 2) || (width == 3 && height == 3);
    }
    *///?}

    private boolean matches(int size, IntFunction<ItemStack> slot) {
        return this.found(size, slot) != null;
    }

    private ItemStack assemble(int size, IntFunction<ItemStack> slot) {
        Found found = this.found(size, slot);
        if (found == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(ModItems.RAW_POTTERY);
        CompoundTag tag = new CompoundTag();
        PotteryItem.copyPottery(found.raw, tag);
        tag.putString("dye", found.color.getName());
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        PotteryItem.showColor(result, found.color.getName());
        return result;
    }

    private @Nullable Found found(int size, IntFunction<ItemStack> slot) {
        ItemStack raw = ItemStack.EMPTY;
        DyeColor color = null;
        for (int index = 0; index < size; index++) {
            ItemStack stack = slot.apply(index);
            if (stack.isEmpty()) {
                continue;
            }
            if (raw.isEmpty() && stack.is(ModItems.RAW_POTTERY) && PotteryItem.dye(stack) == null) {
                raw = stack;
                continue;
            }
            DyeColor dye = vanillaDye(stack);
            if (color == null && dye != null) {
                color = dye;
                continue;
            }
            return null;
        }
        if (raw.isEmpty() || color == null) {
            return null;
        }
        return new Found(raw, color);
    }

    private static @Nullable DyeColor vanillaDye(ItemStack stack) {
        //? if >=26.1 {
        DyeColor color = stack.get(DataComponents.DYE);
        if (color == null || !stack.is(net.minecraft.world.item.Items.DYE.pick(color))) {
            return null;
        }
        return color;
        //?} else {
        /*if (!(stack.getItem() instanceof net.minecraft.world.item.DyeItem dye)) {
            return null;
        }
        if (!stack.is(net.minecraft.world.item.DyeItem.byColor(dye.getDyeColor()))) {
            return null;
        }
        return dye.getDyeColor();
        *///?}
    }

    private record Found(ItemStack raw, DyeColor color) {
    }
}
