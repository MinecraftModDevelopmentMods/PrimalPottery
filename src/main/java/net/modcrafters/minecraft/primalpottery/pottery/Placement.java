package net.modcrafters.minecraft.primalpottery.pottery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.modcrafters.minecraft.primalpottery.block.ModBlocks;
import net.modcrafters.minecraft.primalpottery.block.PotteryBlock;
import net.modcrafters.minecraft.primalpottery.block.PotteryBlockEntity;
import net.modcrafters.minecraft.primalpottery.item.ModItems;
import org.jspecify.annotations.Nullable;

public final class Placement {
    private Placement() {
    }

    public static @Nullable AABB outline(Level level, @Nullable Player player, BlockHitResult hit) {
        if (player == null || !player.getMainHandItem().is(ModItems.POTTERY)) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        if (level.getBlockState(pos).is(ModBlocks.POTTERY)) {
            if (!(level.getBlockEntity(pos) instanceof PotteryBlockEntity pottery)) {
                return null;
            }
            int part = destination(
                    pottery,
                    PotteryBlock.aimedPart(level, pos, player, hit.getLocation()),
                    player.isShiftKeyDown(),
                    hit.getDirection()
            );
            if (part < 0) {
                return null;
            }
            return PotteryBlockEntity.box(part).move(pos);
        }
        if (hit.getDirection() != Direction.UP) {
            return null;
        }
        BlockPos above = pos.above();
        return PotteryBlockEntity.box(PartHits.quadrant(pos, hit.getLocation())).move(above);
    }

    public static Vec3 reachEnd(Player player) {
        Vec3 from = player.getEyePosition();
        return from.add(player.getViewVector(1).scale(player.blockInteractionRange() + 1));
    }

    public static boolean holding(ItemStack stack) {
        return stack.is(ModItems.POTTERY);
    }

    public static int destination(PotteryBlockEntity pottery, int part, boolean sneak, Direction face) {
        if (part < 0 || part >= PotteryBlockEntity.SLOTS) {
            return -1;
        }
        if (!sneak) {
            return pottery.get(part) == null ? part : -1;
        }
        if (pottery.get(part) == null) {
            return part;
        }
        int facing = PartHits.beside(part, face);
        if (facing >= 0 && pottery.get(facing) == null) {
            return facing;
        }
        for (int neighbor : PartHits.neighbors(part)) {
            if (pottery.get(neighbor) == null) {
                return neighbor;
            }
        }
        return -1;
    }
}
