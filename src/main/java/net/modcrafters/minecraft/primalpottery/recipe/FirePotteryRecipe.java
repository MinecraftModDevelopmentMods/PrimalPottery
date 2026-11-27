package net.modcrafters.minecraft.primalpottery.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
//? if >=26.1
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.modcrafters.minecraft.primalpottery.item.ModItems;
import net.modcrafters.minecraft.primalpottery.item.PotteryItem;
//? if >=1.21
import net.minecraft.world.item.crafting.SingleRecipeInput;

/** Smelts raw pottery, dyed or not, into the fired item. The parent result is never used. */
public final class FirePotteryRecipe extends AbstractCookingRecipe {
    public static final FirePotteryRecipe INSTANCE = new FirePotteryRecipe();
    //? if >=26.1 {
    public static final RecipeSerializer<FirePotteryRecipe> SERIALIZER = new RecipeSerializer<>(MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));
    //?} else {
    /*public static final RecipeSerializer<FirePotteryRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<FirePotteryRecipe> codec() {
            return MapCodec.unit(INSTANCE);
        }

        @Override
        public StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, FirePotteryRecipe> streamCodec() {
            return StreamCodec.unit(INSTANCE);
        }
    };
    *///?}

    //? if >=26.1 {
    private FirePotteryRecipe() {
        super(
                new Recipe.CommonInfo(false),
                new CookingBookInfo(net.minecraft.world.item.crafting.CookingBookCategory.MISC, ""),
                Ingredient.of(ModItems.RAW_POTTERY),
                new net.minecraft.world.item.ItemStackTemplate(ModItems.POTTERY),
                0.1F,
                200
        );
    }
    //?} else {
    /*private FirePotteryRecipe() {
        super(
                RecipeType.SMELTING,
                "",
                net.minecraft.world.item.crafting.CookingBookCategory.MISC,
                Ingredient.of(ModItems.RAW_POTTERY),
                new ItemStack(ModItems.POTTERY),
                0.1F,
                200
        );
    }
    *///?}

    //? if >=26.1 {
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return input.item().is(ModItems.RAW_POTTERY);
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return this.fired(input.item());
    }
    //?} elif >=1.21 {
    /*@Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return input.item().is(ModItems.RAW_POTTERY);
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return this.fired(input.item());
    }
    *///?} else {
    /*@Override
    public boolean matches(net.minecraft.world.Container input, Level level) {
        return input.getItem(0).is(ModItems.RAW_POTTERY);
    }

    @Override
    public ItemStack assemble(net.minecraft.world.Container input, net.minecraft.core.HolderLookup.Provider registries) {
        return this.fired(input.getItem(0));
    }
    *///?}

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<FirePotteryRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<? extends AbstractCookingRecipe> getType() {
        return RecipeType.SMELTING;
    }

    //? if >=26.1 {
    @Override
    protected net.minecraft.world.item.Item furnaceIcon() {
        return net.minecraft.world.item.Items.FURNACE;
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
        return net.minecraft.world.item.crafting.RecipeBookCategories.FURNACE_MISC;
    }
    //?}

    private ItemStack fired(ItemStack raw) {
        ItemStack result = new ItemStack(ModItems.POTTERY);
        CompoundTag tag = new CompoundTag();
        PotteryItem.copyPottery(raw, tag);
        String dye = PotteryItem.dye(raw);
        if (dye != null) {
            tag.putString("dye", dye);
        }
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        PotteryItem.showColor(result, dye);
        return result;
    }
}
