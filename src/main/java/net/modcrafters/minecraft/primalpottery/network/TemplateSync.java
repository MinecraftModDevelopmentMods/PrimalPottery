package net.modcrafters.minecraft.primalpottery.network;

import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.modcrafters.minecraft.primalpottery.item.ModItems;
import net.modcrafters.minecraft.primalpottery.item.TemplateItem;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryDisc;

/** The server saves the profile and turns a raw template into a blueprint. */
public final class TemplateSync {
    public interface Sender {
        void send(boolean mainHand, List<PotteryDisc> discs);
    }

    public static Sender sender = (mainHand, discs) -> {
    };

    private TemplateSync() {
    }

    public static void send(boolean mainHand, List<PotteryDisc> discs) {
        sender.send(mainHand, List.copyOf(discs));
    }

    public static void apply(Player player, boolean mainHand, List<PotteryDisc> discs) {
        if (!Pottery.validProfile(discs)) {
            return;
        }
        InteractionHand hand = mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModItems.RAW_TEMPLATE)) {
            ItemStack blueprint = new ItemStack(ModItems.BLUEPRINT);
            TemplateItem.write(blueprint, new Pottery(discs));
            if (stack.getCount() == 1) {
                player.setItemInHand(hand, blueprint);
            } else {
                stack.shrink(1);
                if (!player.getInventory().add(blueprint)) {
                    //? if >=26.1 {
                    player.drop(blueprint, false, net.minecraft.util.Prediction.SERVER_ONLY);
                    //?} else
                    //player.drop(blueprint, false);
                }
            }
        } else if (stack.is(ModItems.BLUEPRINT)) {
            TemplateItem.write(stack, new Pottery(discs));
        }
    }
}
