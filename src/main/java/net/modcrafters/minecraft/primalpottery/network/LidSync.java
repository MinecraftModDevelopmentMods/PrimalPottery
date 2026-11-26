package net.modcrafters.minecraft.primalpottery.network;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.modcrafters.minecraft.primalpottery.item.ModItems;
import net.modcrafters.minecraft.primalpottery.item.PotteryItem;

/** The server writes the lid flag onto the pottery item in the chosen hand. */
public final class LidSync {
    public interface Sender {
        void send(boolean mainHand, boolean closed);
    }

    public static Sender sender = (mainHand, closed) -> {
    };

    private LidSync() {
    }

    public static void send(boolean mainHand, boolean closed) {
        sender.send(mainHand, closed);
    }

    public static void apply(Player player, boolean mainHand, boolean closed) {
        InteractionHand hand = mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.POTTERY)) {
            return;
        }
        if (closed) {
            if (PotteryItem.read(stack).lidDisc() == null) {
                return;
            }
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("lid", true));
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove("lid"));
    }
}
