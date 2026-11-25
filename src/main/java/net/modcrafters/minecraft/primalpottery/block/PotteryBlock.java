package net.modcrafters.minecraft.primalpottery.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.modcrafters.minecraft.primalpottery.item.PotteryItem;
import net.modcrafters.minecraft.primalpottery.pottery.PartHits;
import net.modcrafters.minecraft.primalpottery.pottery.Placement;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import org.jspecify.annotations.Nullable;

public class PotteryBlock extends Block implements EntityBlock {
    private static final VoxelShape BASE = Shapes.box(0, 0, 0, 1, PotteryBlockEntity.SIZE / 32.0, 1);
    private static final VoxelShape[] PARTS = parts();
    private static final VoxelShape[] OCCUPIED = occupied();

    public PotteryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BASE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return occupied(level, pos);
    }

    //? if >=26.1 {
    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }
    //?} else {
    /*@Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
    *///?}

    //? if neoforge {
    /*//? if >=26.1 {
    @Override
    public boolean onDestroyedByPlayer(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            ItemStack tool,
            boolean willHarvest,
            net.minecraft.world.level.material.FluidState fluid
    ) {
        if (breakFocused(level, pos, player)) {
            return false;
        }
        return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
    }
    //?} else {
    /^@Override
    public boolean onDestroyedByPlayer(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            boolean willHarvest,
            net.minecraft.world.level.material.FluidState fluid
    ) {
        if (breakFocused(level, pos, player)) {
            return false;
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }
    ^///?}
    *///?}

    public static boolean breakFocused(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof PotteryBlockEntity pottery)) {
            return false;
        }
        int part = aimedPart(level, pos, player, aimedClick(pos, player));
        Pottery taken = part < 0 ? null : pottery.get(part);
        if (taken == null) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        pottery.take(part);
        if (!level.isClientSide()) {
            if (!player.hasInfiniteMaterials()) {
                popResource(level, pos, PotteryItem.stack(taken));
            }
            level.levelEvent(2001, pos, Block.getId(state));
        }
        return true;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PotteryBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    //? if >=1.21.2 {
    @Override
    public InteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
    ) {
        return interact(level, pos, player, stack, hit);
    }
    //?} else {
    /*@Override
    public net.minecraft.world.ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
    ) {
        InteractionResult result = interact(level, pos, player, stack, hit);
        if (result == InteractionResult.FAIL) {
            return net.minecraft.world.ItemInteractionResult.FAIL;
        }
        if (result.consumesAction()) {
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    *///?}

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(level, pos, player, ItemStack.EMPTY, hit);
    }

    public static InteractionResult tryPlace(
            Level level, BlockPos pos, @Nullable Player player, ItemStack stack, int part, boolean sneak, Direction face
    ) {
        if (!(level.getBlockEntity(pos) instanceof PotteryBlockEntity pottery)) {
            return InteractionResult.FAIL;
        }
        int target = Placement.destination(pottery, part, sneak, face);
        if (target < 0) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            if (!pottery.put(target, PotteryItem.read(stack))) {
                return InteractionResult.FAIL;
            }
            level.playSound(null, pos, SoundType.DECORATED_POT.getPlaceSound(), SoundSource.BLOCKS, 1, 0.8F);
            if (player != null && !player.hasInfiniteMaterials()) {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult interact(Level level, BlockPos pos, Player player, ItemStack stack, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PotteryBlockEntity pottery)) {
            return InteractionResult.PASS;
        }
        int part = aimedPart(level, pos, player, hit.getLocation());
        if (!player.isShiftKeyDown() && pottery.get(part) != null) {
            Pottery taken = pottery.get(part);
            if (!level.isClientSide()) {
                pottery.take(part);
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, PotteryItem.stack(taken)));
            }
            return InteractionResult.SUCCESS;
        }
        if (!Placement.holding(stack)) {
            return InteractionResult.PASS;
        }
        return tryPlace(level, pos, player, stack, part, player.isShiftKeyDown(), hit.getDirection());
    }

    private static VoxelShape occupied(BlockGetter level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof PotteryBlockEntity pottery)) {
            return Shapes.empty();
        }
        int mask = 0;
        for (int index = 0; index < PotteryBlockEntity.SLOTS; index++) {
            if (pottery.get(index) != null) {
                mask |= 1 << index;
            }
        }
        return OCCUPIED[mask];
    }

    public static int aimedPart(Level level, BlockPos pos, @Nullable Player player, Vec3 click) {
        if (player != null && level.getBlockEntity(pos) instanceof PotteryBlockEntity pottery) {
            int pot = PartHits.nearest(
                    PotteryBlockEntity.BOXES,
                    pos,
                    player.getEyePosition(),
                    Placement.reachEnd(player),
                    index -> pottery.get(index) != null
            );
            if (pot >= 0) {
                return pot;
            }
        }
        return PartHits.quadrant(pos, click);
    }

    private static Vec3 aimedClick(BlockPos pos, Player player) {
        Vec3 from = player.getEyePosition();
        Vec3 to = Placement.reachEnd(player);
        AABB base = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + PotteryBlockEntity.SIZE / 32.0, pos.getZ() + 1);
        return base.clip(from, to).orElse(from);
    }

    private static VoxelShape[] parts() {
        VoxelShape[] shapes = new VoxelShape[PotteryBlockEntity.SLOTS];
        for (int index = 0; index < shapes.length; index++) {
            shapes[index] = Shapes.create(PotteryBlockEntity.box(index));
        }
        return shapes;
    }

    private static VoxelShape[] occupied() {
        VoxelShape[] shapes = new VoxelShape[1 << PotteryBlockEntity.SLOTS];
        shapes[0] = Shapes.empty();
        for (int mask = 1; mask < shapes.length; mask++) {
            VoxelShape shape = Shapes.empty();
            for (int index = 0; index < PotteryBlockEntity.SLOTS; index++) {
                if ((mask & (1 << index)) != 0) {
                    shape = Shapes.or(shape, PARTS[index]);
                }
            }
            shapes[mask] = shape;
        }
        return shapes;
    }
}
