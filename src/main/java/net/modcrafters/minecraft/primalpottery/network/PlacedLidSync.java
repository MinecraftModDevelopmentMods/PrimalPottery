package net.modcrafters.minecraft.primalpottery.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.modcrafters.minecraft.primalpottery.block.ModBlocks;
import net.modcrafters.minecraft.primalpottery.block.PotteryBlockEntity;

/** The server writes the lid flag onto one occupied pot the player may interact with. */
public final class PlacedLidSync {
    public interface Sender {
        void send(BlockPos pos, int slot, boolean closed);
    }

    public static Sender sender = (pos, slot, closed) -> {
    };

    private PlacedLidSync() {
    }

    public static void send(BlockPos pos, int slot, boolean closed) {
        sender.send(pos, slot, closed);
    }

    public static void apply(Player player, BlockPos pos, int slot, boolean closed) {
        Level level = player.level();
        if (!level.mayInteract(player, pos) || !level.getBlockState(pos).is(ModBlocks.POTTERY)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof PotteryBlockEntity pottery)) {
            return;
        }
        if (slot < 0 || slot >= PotteryBlockEntity.SLOTS || pottery.get(slot) == null) {
            return;
        }
        if (closed) {
            if (pottery.get(slot).lidDisc() == null) {
                return;
            }
            pottery.setLid(slot, true);
            return;
        }
        pottery.setLid(slot, false);
    }
}
