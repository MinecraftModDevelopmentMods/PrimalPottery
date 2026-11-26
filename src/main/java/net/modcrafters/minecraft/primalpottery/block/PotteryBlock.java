package net.modcrafters.minecraft.primalpottery.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.modcrafters.minecraft.primalpottery.client.PotteryScreen;
import net.modcrafters.minecraft.primalpottery.item.PotteryItem;
import net.modcrafters.minecraft.primalpottery.pottery.PartHits;
import net.modcrafters.minecraft.primalpottery.pottery.Placement;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import net.modcrafters.minecraft.primalpottery.pottery.VesselContents;
import org.jspecify.annotations.Nullable;

public class PotteryBlock extends Block implements EntityBlock {
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
    private static final VoxelShape[] PARTS = parts();
    private static final VoxelShape[] OCCUPIED = occupied();
    private static final VoxelShape FOOTPRINT = OCCUPIED[(1 << PotteryBlockEntity.SLOTS) - 1];

    public PotteryBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIGHT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FOOTPRINT;
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

    public static boolean breakFocused(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof PotteryBlockEntity pottery)) {
            return false;
        }
        int part = PartHits.nearest(
                PotteryBlockEntity.BOXES,
                pos,
                player.getEyePosition(),
                Placement.reachEnd(player)
        );
        if (part < 0 || pottery.get(part) == null) {
            return true;
        }
        Pottery taken = pottery.get(part);
        VesselContents contents = pottery.contents(part);
        boolean lid = pottery.lid(part);
        BlockState state = level.getBlockState(pos);
        pottery.take(part);
        if (!level.isClientSide()) {
            if (!player.hasInfiniteMaterials()) {
                popResource(level, pos, PotteryItem.stack(taken, contents, lid));
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
        if (!stack.isEmpty()) {
            return net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
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
            if (!pottery.put(target, PotteryItem.read(stack), PotteryItem.contents(stack), PotteryItem.lid(stack))) {
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
        int part = PartHits.quadrant(pos, hit.getLocation());
        if (stack.isEmpty()) {
            if (pottery.get(part) == null) {
                return InteractionResult.PASS;
            }
            if (player.isShiftKeyDown()) {
                if (!level.isClientSide()) {
                    Pottery taken = pottery.get(part);
                    VesselContents contents = pottery.contents(part);
                    boolean lid = pottery.lid(part);
                    pottery.take(part);
                    level.addFreshEntity(new ItemEntity(
                            level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, PotteryItem.stack(taken, contents, lid)
                    ));
                }
                return InteractionResult.SUCCESS;
            }
            if (level.isClientSide()) {
                PotteryScreen.open(level, pos, part);
            }
            return InteractionResult.SUCCESS;
        }
        if (Placement.holding(stack) && !PotteryItem.contents(stack).isEmpty() && !player.isShiftKeyDown() && pottery.get(part) != null) {
            pour(level, pos, stack, pottery, part);
            return InteractionResult.SUCCESS;
        }
        if (Placement.holding(stack)) {
            return tryPlace(level, pos, player, stack, part, player.isShiftKeyDown(), hit.getDirection());
        }
        return InteractionResult.PASS;
    }

    private static void pour(Level level, BlockPos pos, ItemStack stack, PotteryBlockEntity pottery, int part) {
        if (level.isClientSide()) {
            return;
        }
        Pottery pot = pottery.get(part);
        if (pot == null || pottery.lid(part) || PotteryItem.lid(stack)) {
            return;
        }
        Pottery.Info info = pot.classify();
        if (!info.type().isVessel() || info.volume() <= 0) {
            return;
        }
        VesselContents held = PotteryItem.contents(stack);
        VesselContents stored = pottery.contents(part);
        int moved = VesselContents.poured(held, stored, info.volume());
        if (moved <= 0) {
            return;
        }
        int storedAmount = stored.isEmpty() ? 0 : stored.amount();
        pottery.setContents(part, VesselContents.of(held.fluid(), storedAmount + moved));
        int left = held.amount() - moved;
        VesselContents remainder = left > 0 ? VesselContents.of(held.fluid(), left) : VesselContents.EMPTY;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, remainder::write);
        held.fluid().getPickupSound().ifPresent(sound ->
                level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F));
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
