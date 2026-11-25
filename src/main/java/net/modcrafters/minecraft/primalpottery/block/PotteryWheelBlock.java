package net.modcrafters.minecraft.primalpottery.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.modcrafters.minecraft.primalpottery.item.ModItems;
import net.modcrafters.minecraft.primalpottery.item.TemplateItem;
import net.modcrafters.minecraft.primalpottery.pottery.PartHits;
import net.modcrafters.minecraft.primalpottery.pottery.Placement;
import org.jspecify.annotations.Nullable;

public class PotteryWheelBlock extends Block implements EntityBlock {
    public static final DoubleBlockHalf LOWER = DoubleBlockHalf.LOWER;
    public static final DoubleBlockHalf UPPER = DoubleBlockHalf.UPPER;
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final VoxelShape UPPER_SHAPE = Shapes.box(0, 0, 0, 1, PotteryWheelBlockEntity.BLOCK_HEIGHT - 1, 1);
    private static boolean removingPartner;

    public PotteryWheelBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HALF, LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos above = context.getClickedPos().above();
        Level level = context.getLevel();
        if (level.isOutsideBuildHeight(above) || !level.getBlockState(above).canBeReplaced(context)) {
            return null;
        }
        return this.defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, UPPER), Block.UPDATE_ALL);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        removePartner(level, pos, state, !player.hasInfiniteMaterials());
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (state.getValue(HALF) == UPPER) {
            return List.of();
        }
        return super.getDrops(state, params);
    }

    //? if >=26.1 {
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        removePartner(level, pos, state, true);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
    //?} else {
    /*@Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            removePartner(level, pos, state, true);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    *///?}

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == UPPER ? UPPER_SHAPE : Shapes.block();
    }

    static void removePartner(Level level, BlockPos pos, BlockState state, boolean drop) {
        if (removingPartner) {
            return;
        }
        BlockPos other = state.getValue(HALF) == LOWER ? pos.above() : pos.below();
        BlockState otherState = level.getBlockState(other);
        if (!otherState.is(state.getBlock()) || otherState.getValue(HALF) == state.getValue(HALF)) {
            return;
        }
        removingPartner = true;
        try {
            if (drop && !level.isClientSide() && state.getValue(HALF) == UPPER) {
                popResource(level, other, new ItemStack(ModItems.WHEEL));
            }
            level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        } finally {
            removingPartner = false;
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(HALF) == UPPER) {
            return null;
        }
        return new PotteryWheelBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.WHEEL) {
            return null;
        }
        return (level1, pos, state1, blockEntity) -> ((PotteryWheelBlockEntity) blockEntity).tick();
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
        return interact(level, origin(state, pos), player, stack);
    }
    //?} else {
    /*@Override
    public net.minecraft.world.ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
    ) {
        InteractionResult result = interact(level, origin(state, pos), player, stack);
        if (result.consumesAction()) {
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    *///?}

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            stack = player.getOffhandItem();
        }
        return interact(level, origin(state, pos), player, stack);
    }

    private static BlockPos origin(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == UPPER ? pos.below() : pos;
    }

    private static InteractionResult interact(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof PotteryWheelBlockEntity wheel)) {
            return InteractionResult.PASS;
        }
        Vec3 from = player.getEyePosition();
        int part = PartHits.nearest(PotteryWheelBlockEntity.BOXES, pos, from, Placement.reachEnd(player));
        if (part == PotteryWheelBlockEntity.BIG_WHEEL) {
            wheel.spin();
            return InteractionResult.SUCCESS;
        }
        if (part == PotteryWheelBlockEntity.TABLE) {
            if (stack.is(ModItems.BLUEPRINT)) {
                wheel.install(TemplateItem.profile(stack));
                return InteractionResult.SUCCESS;
            }
            if (player.isShiftKeyDown() && stack.isEmpty() && !wheel.hasClay() && wheel.template() != null) {
                wheel.clearTemplate();
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (part != PotteryWheelBlockEntity.SMALL_WHEEL) {
            return InteractionResult.PASS;
        }
        if (wheel.hasClay()) {
            wheel.takeClay(player);
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.CLAY_BALL)) {
            wheel.startClay();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
