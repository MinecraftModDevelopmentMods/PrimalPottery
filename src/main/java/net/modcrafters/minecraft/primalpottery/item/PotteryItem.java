package net.modcrafters.minecraft.primalpottery.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.modcrafters.minecraft.primalpottery.block.ModBlocks;
import net.modcrafters.minecraft.primalpottery.block.PotteryBlock;
import net.modcrafters.minecraft.primalpottery.block.PotteryBlockEntity;
import net.modcrafters.minecraft.primalpottery.pottery.Nbt;
import net.modcrafters.minecraft.primalpottery.pottery.PartHits;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;

public class PotteryItem extends BlockItem {
    public PotteryItem(Properties properties) {
        super(ModBlocks.POTTERY, properties);
    }

    public static ItemStack stack(Pottery pottery) {
        ItemStack stack = new ItemStack(ModItems.POTTERY);
        CompoundTag tag = new CompoundTag();
        tag.put("pottery", pottery.save());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static Pottery read(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Pottery.display();
        }
        CompoundTag pottery = Nbt.compound(data.copyTag(), "pottery");
        if (pottery.isEmpty()) {
            return Pottery.display();
        }
        return Pottery.load(pottery);
    }

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
                    PotteryBlock.aimedPart(context.getLevel(), clicked, player, context.getClickLocation()),
                    player != null && player.isShiftKeyDown(),
                    context.getClickedFace()
            );
        }
        if (context.getClickedFace() != net.minecraft.core.Direction.UP) {
            return InteractionResult.FAIL;
        }
        Pottery shaped = read(context.getItemInHand());
        InteractionResult result = super.place(context);
        if (!result.consumesAction()) {
            return result;
        }
        Level level = context.getLevel();
        BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
        if (blockEntity instanceof PotteryBlockEntity pottery) {
            pottery.put(PartHits.quadrant(context.getClickedPos(), context.getClickLocation()), shaped);
        }
        return result;
    }
}
