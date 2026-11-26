package net.modcrafters.minecraft.primalpottery.item;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.modcrafters.minecraft.primalpottery.block.ModBlocks;
import net.modcrafters.minecraft.primalpottery.block.PotteryBlock;
import net.modcrafters.minecraft.primalpottery.block.PotteryBlockEntity;
import net.modcrafters.minecraft.primalpottery.client.PotteryScreen;
import net.modcrafters.minecraft.primalpottery.pottery.Nbt;
import net.modcrafters.minecraft.primalpottery.pottery.PartHits;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import net.modcrafters.minecraft.primalpottery.pottery.VesselContents;
import org.jspecify.annotations.Nullable;
//? if >=26.1
import net.minecraft.world.item.component.TooltipDisplay;
//? if <26.1
//import net.minecraft.world.InteractionResultHolder;

public class PotteryItem extends BlockItem {
    public PotteryItem(Properties properties) {
        super(ModBlocks.POTTERY, properties);
    }

    public static ItemStack stack(Pottery pottery) {
        return stack(pottery, VesselContents.EMPTY);
    }

    public static ItemStack stack(Pottery pottery, VesselContents contents) {
        return stack(pottery, contents, false);
    }

    public static ItemStack stack(Pottery pottery, VesselContents contents, boolean lid) {
        ItemStack stack = new ItemStack(ModItems.POTTERY);
        CompoundTag tag = new CompoundTag();
        tag.put("pottery", pottery.save());
        contents.write(tag);
        if (lid) {
            tag.putBoolean("lid", true);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static boolean lid(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag != null && Nbt.getBoolean(tag, "lid");
    }

    public static Component fluidName(Fluid fluid) {
        Component name = fluid.defaultFluidState().createLegacyBlock().getBlock().getName();
        int color = VesselContents.tooltipColor(fluid);
        if (color == -1) {
            return name;
        }
        return name.copy().withStyle(style -> style.withColor(color));
    }

    public static Pottery read(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (tag == null) {
            return Pottery.display();
        }
        CompoundTag pottery = Nbt.compound(tag, "pottery");
        if (pottery.isEmpty()) {
            return Pottery.display();
        }
        return Pottery.load(pottery);
    }

    public static VesselContents contents(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return tag == null ? VesselContents.EMPTY : VesselContents.read(tag);
    }

    private static @Nullable CompoundTag tag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    //? if >=26.1 {
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        appendFluid(stack, tooltip);
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        appendFluid(stack, tooltip::add);
    }
    *///?}

    public static void contentsTooltip(Pottery pottery, VesselContents contents, Consumer<Component> tooltip) {
        Pottery.Info info = pottery.classify();
        if (!info.type().isVessel()) {
            return;
        }
        tooltip.accept(Component.translatable("tooltip.primalpottery.capacity", info.volume()));
        if (!contents.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.primalpottery.contents", fluidName(contents.fluid()), contents.amount()));
        }
    }

    private static void appendFluid(ItemStack stack, Consumer<Component> tooltip) {
        contentsTooltip(read(stack), contents(stack), tooltip);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockHitResult look = player == null ? null : getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        BlockPlaceContext place = new BlockPlaceContext(context);
        VesselContents.Facts facts = facts(stack, player, level, look, place, true);
        if (VesselContents.choose(facts) == VesselContents.Choice.FILL) {
            if (!level.isClientSide() && look != null) {
                fill(level, stack, look);
            }
            return InteractionResult.SUCCESS;
        }
        if (denied(facts)) {
            return InteractionResult.FAIL;
        }
        return this.place(place);
    }

    //? if >=26.1 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult look = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        VesselContents.Facts facts = facts(stack, player, level, look, null, true);
        VesselContents.Choice choice = VesselContents.choose(facts);
        if (choice == VesselContents.Choice.FILL) {
            if (!level.isClientSide()) {
                fill(level, stack, look);
            }
            return InteractionResult.SUCCESS;
        }
        if (choice == VesselContents.Choice.REFUSE) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            PotteryScreen.open(player, hand);
        }
        return InteractionResult.SUCCESS;
    }
    //?} else {
    /*@Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult look = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        VesselContents.Facts facts = facts(stack, player, level, look, null, true);
        VesselContents.Choice choice = VesselContents.choose(facts);
        if (choice == VesselContents.Choice.FILL) {
            if (!level.isClientSide()) {
                fill(level, stack, look);
            }
            return InteractionResultHolder.success(stack);
        }
        if (choice == VesselContents.Choice.REFUSE) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide()) {
            PotteryScreen.open(player, hand);
        }
        return InteractionResultHolder.success(stack);
    }
    *///?}

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        BlockPos clicked = context.replacingClickedOnBlock()
                ? context.getClickedPos()
                : context.getClickedPos().relative(context.getClickedFace().getOpposite());
        if (context.getLevel().getBlockState(clicked).is(ModBlocks.POTTERY)) {
            Player player = context.getPlayer();
            return PotteryBlock.tryPlace(
                    context.getLevel(),
                    clicked,
                    player,
                    context.getItemInHand(),
                    PartHits.quadrant(clicked, context.getClickLocation()),
                    player != null && player.isShiftKeyDown(),
                    context.getClickedFace()
            );
        }
        VesselContents.Facts facts = facts(
                context.getItemInHand(), context.getPlayer(), context.getLevel(), null, context, false
        );
        if (VesselContents.choose(facts) == VesselContents.Choice.REFUSE) {
            return InteractionResult.FAIL;
        }
        if (context.getClickedFace() != net.minecraft.core.Direction.UP) {
            return InteractionResult.FAIL;
        }
        ItemStack stack = context.getItemInHand();
        Pottery shaped = read(stack);
        VesselContents contents = contents(stack);
        boolean closed = lid(stack);
        InteractionResult result = super.place(context);
        if (!result.consumesAction()) {
            return result;
        }
        Level level = context.getLevel();
        BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
        if (blockEntity instanceof PotteryBlockEntity pottery) {
            pottery.put(PartHits.quadrant(context.getClickedPos(), context.getClickLocation()), shaped, contents, closed);
        }
        return result;
    }

    private static void fill(Level level, ItemStack stack, BlockHitResult look) {
        if (lid(stack)) {
            return;
        }
        BlockPos pos = look.getBlockPos();
        BlockState state = level.getBlockState(pos);
        FluidState fluid = state.getFluidState();
        if (!VesselContents.ownsFluidBlock(state, fluid)) {
            return;
        }
        int volume = read(stack).classify().volume();
        Fluid source = VesselContents.source(fluid.getType());
        VesselContents contents = VesselContents.of(source, volume);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, contents::write);
        source.getPickupSound().ifPresent(sound ->
                level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F));
    }

    /** A fill the player is not allowed to perform. It must fail before placement. */
    private static boolean denied(VesselContents.Facts facts) {
        return facts.povFluid() && facts.player() && facts.vessel() && facts.empty() && facts.volume() > 0 && !facts.interact();
    }

    private static VesselContents.Facts facts(
            ItemStack stack,
            @Nullable Player player,
            Level level,
            @Nullable BlockHitResult look,
            @Nullable BlockPlaceContext place,
            boolean pov
    ) {
        Pottery pottery = read(stack);
        VesselContents contents = contents(stack);
        boolean fluidBlock = false;
        String sourceId = "";
        boolean interact = false;
        if (pov && player != null && look != null && look.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = look.getBlockPos();
            BlockState state = level.getBlockState(pos);
            FluidState fluid = state.getFluidState();
            fluidBlock = VesselContents.ownsFluidBlock(state, fluid);
            if (fluidBlock) {
                Identifier id = BuiltInRegistries.FLUID.getKey(VesselContents.source(fluid.getType()));
                sourceId = id == null ? "" : id.toString();
            }
            interact = level.mayInteract(player, pos)
                    && player.mayUseItemAt(pos.relative(look.getDirection()), look.getDirection(), stack);
        }
        boolean canPlace = false;
        boolean clickedFluid = false;
        boolean clickedSource = false;
        if (place != null) {
            canPlace = place.canPlace();
            BlockState state = level.getBlockState(place.getClickedPos());
            FluidState fluid = state.getFluidState();
            clickedFluid = !fluid.isEmpty();
            clickedSource = fluid.isSource() && (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.LAVA);
        }
        Pottery.Info info = pottery.classify();
        return new VesselContents.Facts(
                player != null,
                info.type().isVessel(),
                contents.isEmpty(),
                info.volume(),
                fluidBlock,
                sourceId,
                interact,
                canPlace,
                clickedFluid,
                clickedSource,
                player != null && player.isShiftKeyDown(),
                !contents.isEmpty()
        );
    }
}
