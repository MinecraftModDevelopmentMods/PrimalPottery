package net.modcrafters.minecraft.primalpottery.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.modcrafters.minecraft.primalpottery.Constants;
import net.modcrafters.minecraft.primalpottery.Ids;

public final class ModTab {
    public static final ResourceKey<CreativeModeTab> ID = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Ids.of(Constants.MOD_ID)
    );

    private ModTab() {
    }

    public static CreativeModeTab.Builder titled(CreativeModeTab.Builder builder) {
        return builder.title(Component.translatable("itemGroup." + Constants.MOD_ID)).icon(ModItems::tabIcon);
    }
}
