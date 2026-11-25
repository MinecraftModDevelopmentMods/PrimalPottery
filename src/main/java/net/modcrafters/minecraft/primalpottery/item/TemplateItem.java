package net.modcrafters.minecraft.primalpottery.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.modcrafters.minecraft.primalpottery.client.TemplateScreen;
import net.modcrafters.minecraft.primalpottery.pottery.Nbt;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryDisc;
//? if >=26.1
import net.minecraft.world.item.component.TooltipDisplay;
//? if <26.1
//import net.minecraft.world.InteractionResultHolder;

/** A reusable profile. Using it sculpts; the table keeps that profile. */
public final class TemplateItem extends Item {
    public TemplateItem(Properties properties) {
        super(properties);
    }

    /** Starting shape for a raw template and for blueprints saved without a profile. */
    public static Pottery fresh() {
        double[] radii = {
                5, 5, 5, 5,
                5.5, 5.5, 5.5, 5.5, 5.5, 5.5, 5.5, 5.5, 5.5, 5.5, 5.5, 5.5, 5.5, 5.5,
                5, 5,
                4.5, 4.5,
                3, 3
        };
        List<PotteryDisc> discs = new ArrayList<>(radii.length);
        for (double radius : radii) {
            discs.add(new PotteryDisc(0.5, radius));
        }
        return new Pottery(discs);
    }

    public static void write(ItemStack stack, Pottery pottery) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put("pottery", pottery.save()));
    }

    public static Pottery read(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag pottery = Nbt.compound(data.copyTag(), "pottery");
        if (pottery.isEmpty()) {
            return null;
        }
        return Pottery.load(pottery);
    }

    /** The saved profile, or the editor's starting shape. */
    public static Pottery profile(ItemStack stack) {
        Pottery saved = read(stack);
        return saved == null ? fresh() : saved;
    }

    //? if >=26.1 {
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        appendBlueprintDetails(stack, tooltip);
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        appendBlueprintDetails(stack, tooltip::add);
    }
    *///?}

    private static void appendBlueprintDetails(ItemStack stack, Consumer<Component> tooltip) {
        if (stack.is(ModItems.BLUEPRINT)) {
            Pottery.Info info = profile(stack).classify();
            tooltip.accept(Component.translatable("tooltip.primalpottery.type", info.type().tooltipName()));
            if (info.type().isVessel()) {
                tooltip.accept(Component.translatable("tooltip.primalpottery.capacity", info.volume()));
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && context.getLevel().isClientSide()) {
            TemplateScreen.open(player, context.getHand());
        }
        return InteractionResult.SUCCESS;
    }

    //? if >=26.1 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            TemplateScreen.open(player, hand);
        }
        return InteractionResult.SUCCESS;
    }
    //?} else {
    /*@Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            TemplateScreen.open(player, hand);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
    *///?}
}
