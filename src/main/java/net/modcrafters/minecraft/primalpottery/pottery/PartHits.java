package net.modcrafters.minecraft.primalpottery.pottery;

import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PartHits {
    private PartHits() {
    }

    public static int nearest(List<AABB> localBoxes, BlockPos pos, Vec3 from, Vec3 to) {
        return nearest(localBoxes, pos, from, to, index -> true);
    }

    public static int nearest(List<AABB> localBoxes, BlockPos pos, Vec3 from, Vec3 to, IntPredicate include) {
        int best = -1;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int index = 0; index < localBoxes.size(); index++) {
            if (!include.test(index)) {
                continue;
            }
            Optional<Vec3> hit = localBoxes.get(index).move(pos).clip(from, to);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = hit.get().distanceToSqr(from);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = index;
            }
        }
        return best;
    }

    public static int quadrant(BlockPos pos, Vec3 hit) {
        double x = hit.x - pos.getX();
        double z = hit.z - pos.getZ();
        if (x < 0.5) {
            return z < 0.5 ? 0 : 3;
        }
        return z < 0.5 ? 1 : 2;
    }

    public static int beside(int part, Direction direction) {
        return switch (part) {
            case 0 -> direction == Direction.EAST ? 1 : direction == Direction.SOUTH ? 3 : -1;
            case 1 -> direction == Direction.WEST ? 0 : direction == Direction.SOUTH ? 2 : -1;
            case 2 -> direction == Direction.NORTH ? 1 : direction == Direction.WEST ? 3 : -1;
            case 3 -> direction == Direction.NORTH ? 0 : direction == Direction.EAST ? 2 : -1;
            default -> -1;
        };
    }

    public static int[] neighbors(int part) {
        return switch (part) {
            case 0 -> new int[] {1, 3};
            case 1 -> new int[] {0, 2};
            case 2 -> new int[] {1, 3};
            case 3 -> new int[] {0, 2};
            default -> new int[0];
        };
    }
}
