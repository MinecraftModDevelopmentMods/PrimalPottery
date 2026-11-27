package net.modcrafters.minecraft.primalpottery.recipe;

import java.util.function.BiConsumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.modcrafters.minecraft.primalpottery.Ids;

public final class ModRecipes {
    private ModRecipes() {
    }

    public static void register(BiConsumer<Identifier, RecipeSerializer<?>> out) {
        out.accept(Ids.of("paint_pottery"), PaintPotteryRecipe.SERIALIZER);
        out.accept(Ids.of("fire_pottery"), FirePotteryRecipe.SERIALIZER);
    }
}
