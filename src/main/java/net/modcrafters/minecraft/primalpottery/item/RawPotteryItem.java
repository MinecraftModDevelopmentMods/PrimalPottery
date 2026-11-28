package net.modcrafters.minecraft.primalpottery.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
//? if >=26.1
import net.minecraft.world.item.component.TooltipDisplay;
import net.modcrafters.minecraft.primalpottery.pottery.Nbt;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryDisc;
import org.jspecify.annotations.Nullable;

/** Unfired clay. It is not a block, and it carries no fluid and no lid. */
public class RawPotteryItem extends Item {
    public RawPotteryItem(Properties properties) {
        super(properties);
    }

    public static ItemStack stack(Pottery pottery) {
        return stack(pottery, null);
    }

    public static ItemStack stack(Pottery pottery, @Nullable String dye) {
        ItemStack stack = new ItemStack(ModItems.RAW_POTTERY);
        CompoundTag tag = new CompoundTag();
        tag.put("pottery", pottery.save());
        if (dye != null && !dye.isEmpty()) {
            tag.putString("dye", dye);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        PotteryItem.showColor(stack, dye);
        return stack;
    }

    /**
     * The profile a wheel may reshape. Null when the stack is dyed, or the pottery compound is missing or invalid.
     * {@link Pottery#load} would substitute the display vase for a bad tag, so that stack must not be accepted.
     */
    public static @Nullable Pottery wheelProfile(ItemStack stack) {
        if (!stack.is(ModItems.RAW_POTTERY) || PotteryItem.dye(stack) != null) {
            return null;
        }
        CompoundTag data = PotteryItem.customData(stack);
        if (data == null) {
            return null;
        }
        CompoundTag pottery = Nbt.compound(data, "pottery");
        if (pottery.isEmpty()) {
            return null;
        }
        ListTag list = Nbt.list(pottery, "discs");
        if (list.size() > Pottery.MAX_DISCS) {
            return null;
        }
        List<PotteryDisc> discs = new ArrayList<>();
        for (int index = 0; index < list.size(); index++) {
            CompoundTag discTag = Nbt.compoundAt(list, index);
            double radius = Nbt.getDouble(discTag, "radius");
            double inner = Nbt.getDouble(discTag, "inner");
            if (inner > radius && inner <= Pottery.MAX_RADIUS) {
                inner = radius;
            }
            discs.add(new PotteryDisc(Nbt.getDouble(discTag, "height"), radius, inner));
        }
        if (!Pottery.validProfile(discs)) {
            return null;
        }
        return new Pottery(discs);
    }

    @Override
    public Component getName(ItemStack stack) {
        return PotteryItem.dyedName(
                Component.translatable("item.primalpottery.raw_shape", PotteryItem.read(stack).classify().type().shapeName()),
                PotteryItem.dye(stack)
        );
    }

    //? if >=26.1 {
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip(stack, tooltip);
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip(stack, tooltip::add);
    }
    *///?}

    /** Firing, dyeing, and the volume the fired vessel will hold. The colored name stays the title. */
    public static void tooltip(ItemStack stack, Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable("tooltip.primalpottery.needs_firing").withStyle(ChatFormatting.GRAY));
        if (PotteryItem.dye(stack) == null) {
            tooltip.accept(Component.translatable("tooltip.primalpottery.can_be_dyed").withStyle(ChatFormatting.GRAY));
        }
        Pottery.Info info = PotteryItem.read(stack).classify();
        if (info.type().isVessel()) {
            tooltip.accept(Component.translatable("tooltip.primalpottery.will_contain", info.volume()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
