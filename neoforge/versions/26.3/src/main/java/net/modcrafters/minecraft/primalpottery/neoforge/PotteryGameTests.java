package net.modcrafters.minecraft.primalpottery.neoforge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.modcrafters.minecraft.primalpottery.Ids;
import net.modcrafters.minecraft.primalpottery.block.ModBlocks;
import net.modcrafters.minecraft.primalpottery.block.PotteryBlock;
import net.modcrafters.minecraft.primalpottery.block.PotteryBlockEntity;
import net.modcrafters.minecraft.primalpottery.block.PotteryWheelBlockEntity;
import net.modcrafters.minecraft.primalpottery.item.ModItems;
import net.modcrafters.minecraft.primalpottery.item.PotteryItem;
import net.modcrafters.minecraft.primalpottery.item.TemplateItem;
import net.modcrafters.minecraft.primalpottery.network.LidSync;
import net.modcrafters.minecraft.primalpottery.network.PlacedLidSync;
import net.modcrafters.minecraft.primalpottery.network.TemplateSync;
import net.modcrafters.minecraft.primalpottery.pottery.Nbt;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryDisc;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryType;
import net.modcrafters.minecraft.primalpottery.pottery.VesselContents;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

public final class PotteryGameTests {
    private PotteryGameTests() {
    }

    public static MapCodec<? extends GameTestInstance> codec() {
        return Wheel.CODEC;
    }

    public static MapCodec<? extends GameTestInstance> vesselsCodec() {
        return Vessels.CODEC;
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(Ids.of("empty"), new TestEnvironmentDefinition.AllOf());
        event.registerTest(
                Ids.of("wheel"),
                new Wheel(new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 100, 0, true))
        );
        event.registerTest(
                Ids.of("vessels"),
                new Vessels(new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 100, 0, true))
        );
    }

    private static void wheel(GameTestHelper helper) {
        BlockPos wheelPos = new BlockPos(1, 2, 1);
        helper.setBlock(wheelPos, ModBlocks.WHEEL);
        Player player = mockPlayer(helper);
        PotteryWheelBlockEntity wheel = helper.getBlockEntity(wheelPos, PotteryWheelBlockEntity.class);

        checkTemplateCycle(helper, player);

        aim(player, partCenter(helper, wheelPos, PotteryWheelBlockEntity.BIG_WHEEL));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.useBlock(wheelPos, player);
        helper.assertValueEqual(wheel.spinTicks(), 24, "spin ticks");

        ItemStack clay = new ItemStack(Items.CLAY_BALL);
        player.setItemInHand(InteractionHand.MAIN_HAND, clay);
        aim(player, partCenter(helper, wheelPos, PotteryWheelBlockEntity.SMALL_WHEEL));
        helper.useBlock(wheelPos, player);
        helper.assertTrue(wheel.hasClay(), "clay did not start");
        helper.assertTrue(clay.is(Items.CLAY_BALL), "clay ball was consumed");

        for (int tick = 0; tick < 10; tick++) {
            wheel.tick();
        }
        helper.assertTrue(Math.abs(wheel.clayProgress() - 0.1F) < 0.0001F, "clay progress was " + wheel.clayProgress());

        helper.useBlock(wheelPos, player);
        helper.assertFalse(wheel.hasClay(), "clay was not taken off");
        ItemStack shaped = potteryItem(helper);
        helper.assertValueEqual(PotteryItem.read(shaped).discs().size(), 5, "wheel discs");
        helper.assertValueEqual(PotteryItem.read(shaped).classify().type(), PotteryType.LUMP, "wheel pottery");

        BlockPos floor = new BlockPos(3, 1, 1);
        helper.setBlock(floor, Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, shaped.copy());
        shaped.useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        helper.absoluteVec(new Vec3(3.2, 2.0, 1.2)),
                        net.minecraft.core.Direction.UP,
                        helper.absolutePos(floor),
                        false
                )
        ));
        helper.assertBlockPresent(ModBlocks.POTTERY, floor.above());
        PotteryBlockEntity pots = helper.getBlockEntity(floor.above(), PotteryBlockEntity.class);
        helper.assertTrue(pots.get(0) != null, "pot was not placed");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        useItemOn(helper, player, floor.above(), slotPoint(floor.above(), 0), Direction.UP);
        player.setShiftKeyDown(false);
        helper.assertBlockNotPresent(ModBlocks.POTTERY, floor.above());
        potteryItem(helper);
        helper.succeed();
    }

    private static void checkTemplateCycle(GameTestHelper helper, Player player) {
        ItemStack raw = new ItemStack(ModItems.RAW_TEMPLATE);
        player.setItemInHand(InteractionHand.MAIN_HAND, raw);
        List<PotteryDisc> profile = List.of(new PotteryDisc(1, 4, 1), new PotteryDisc(1, 4));
        TemplateSync.apply(player, true, profile);

        ItemStack blueprint = player.getMainHandItem();
        helper.assertTrue(blueprint.is(ModItems.BLUEPRINT), "raw template did not become a blueprint");
        helper.assertValueEqual(TemplateItem.profile(blueprint).discs(), profile, "blueprint profile");
        var tooltip = blueprint.getTooltipLines(Item.TooltipContext.EMPTY, player, TooltipFlag.Default.NORMAL);
        helper.assertTrue(tooltip.contains(Component.translatable("tooltip.primalpottery.type",
                Component.translatable("pottery.primalpottery.type.jug"))), "blueprint is missing its jug tooltip");
        helper.assertTrue(tooltip.contains(Component.translatable("tooltip.primalpottery.capacity", 3)),
                "blueprint is missing its capacity tooltip");

        var recipes = helper.getLevel().recipeAccess();
        CraftingInput input = CraftingInput.of(1, 1, List.of(blueprint));
        var recipe = recipes.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "no reset recipe matched the saved blueprint");
        ItemStack reset = recipe.orElseThrow().value().assemble(input);
        helper.assertTrue(reset.is(ModItems.RAW_TEMPLATE) && TemplateItem.read(reset) == null,
                "the reset recipe did not produce a blank template");
        CraftingInput blankInput = CraftingInput.of(1, 1, List.of(reset));
        helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, blankInput, helper.getLevel()).isEmpty(),
                "raw templates should not match the reset recipe");
    }

    private static ItemStack potteryItem(GameTestHelper helper) {
        var items = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                helper.getBounds().inflate(8),
                entity -> entity.getItem().is(ModItems.POTTERY)
        );
        helper.assertFalse(items.isEmpty(), "pottery item was not dropped");
        return items.getFirst().getItem();
    }

    /** The batch places other tests nearby, so a bounds-wide search can see their empty pots. */
    private static ItemStack potteryItemNear(GameTestHelper helper, BlockPos relative) {
        AABB box = new AABB(helper.absolutePos(relative)).inflate(2);
        var items = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                box,
                entity -> entity.getItem().is(ModItems.POTTERY)
        );
        helper.assertFalse(items.isEmpty(), "pottery item was not dropped");
        return items.getFirst().getItem();
    }

    private static Vec3 partCenter(GameTestHelper helper, BlockPos relative, int part) {
        return PotteryWheelBlockEntity.BOXES.get(part).move(helper.absolutePos(relative)).getCenter();
    }

    private static void aim(Player player, Vec3 at) {
        // useBlock ignores the crosshair and hits the block center. The wheel and pottery
        // still choose a part from the eye ray, so the eyes have to sit on a ray through that part.
        placeEyes(player, at.add(0, 0, -1));
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, at);
    }

    private static void placeEyes(Player player, Vec3 eye) {
        Vec3 actual = player.getEyePosition();
        player.setPos(player.getX() + eye.x - actual.x, player.getY() + eye.y - actual.y, player.getZ() + eye.z - actual.z);
    }

    private static void vessels(GameTestHelper helper) {
        Player player = mockPlayer(helper);
        Pottery jug = jug();
        int volume = jug.classify().volume();
        helper.assertTrue(volume > 0, "jug has no volume");
        ItemStack empty = PotteryItem.stack(jug);
        List<Component> emptyLines = tooltip(empty, player);
        helper.assertFalse(emptyLines.contains(Blocks.WATER.getName()), "empty jug names a fluid");
        helper.assertTrue(emptyLines.contains(Component.translatable("tooltip.primalpottery.capacity", volume)), "empty capacity");
        helper.assertFalse(emptyLines.stream().anyMatch(line -> line.getString().contains("mL") && !line.getString().startsWith("Capacity")), "empty names an amount");

        BlockPos waterStone = new BlockPos(5, 1, 1);
        BlockPos water = pour(helper, waterStone, Blocks.WATER.defaultBlockState());
        hold(player, jug);
        clickThrough(helper, player, waterStone);
        helper.assertBlockState(water, Blocks.WATER.defaultBlockState());
        assertHeld(helper, player, Fluids.WATER, volume, "water source");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "fill shrank the jug");
        List<Component> waterLines = tooltip(player.getMainHandItem(), player);
        helper.assertTrue(waterLines.contains(Component.translatable("tooltip.primalpottery.capacity", volume)), "water capacity");
        helper.assertTrue(waterLines.contains(Component.translatable("tooltip.primalpottery.contents", PotteryItem.fluidName(Fluids.WATER), volume)), "water amount");
        helper.assertTrue(coloredLine(waterLines, Blocks.WATER.getName().getString(), 0x3F76E4), "water tooltip");

        clickThrough(helper, player, waterStone);
        helper.assertBlockState(water, Blocks.WATER.defaultBlockState());
        assertHeld(helper, player, Fluids.WATER, volume, "second click");

        hold(player, jug);
        clickThrough(helper, player, waterStone);
        helper.assertBlockState(water, Blocks.WATER.defaultBlockState());
        assertHeld(helper, player, Fluids.WATER, volume, "second jug");

        BlockPos lavaStone = new BlockPos(2, 1, 1);
        BlockPos lava = pour(helper, lavaStone, Blocks.LAVA.defaultBlockState());
        hold(player, jug);
        clickThrough(helper, player, lavaStone);
        helper.assertBlockState(lava, Blocks.LAVA.defaultBlockState());
        assertHeld(helper, player, Fluids.LAVA, volume, "lava source");
        List<Component> lavaLines = tooltip(player.getMainHandItem(), player);
        helper.assertTrue(lavaLines.contains(Component.translatable("tooltip.primalpottery.capacity", volume)), "lava capacity");
        helper.assertTrue(lavaLines.contains(Component.translatable("tooltip.primalpottery.contents", PotteryItem.fluidName(Fluids.LAVA), volume)), "lava amount");
        helper.assertTrue(plainLine(lavaLines, Blocks.LAVA.getName().getString()), "lava tooltip");
        ItemStack lavaJug = player.getMainHandItem().copy();
        holdStack(player, PotteryItem.stack(jug, VesselContents.EMPTY, true));
        clickThrough(helper, player, lavaStone);
        helper.assertBlockState(lava, Blocks.LAVA.defaultBlockState());
        helper.assertTrue(PotteryItem.contents(player.getMainHandItem()).isEmpty(), "lidded jug filled");
        helper.assertTrue(PotteryItem.lid(player.getMainHandItem()), "fill removed the lid");

        BlockState flowingWater = Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1);
        BlockPos flowStone = new BlockPos(8, 1, 1);
        BlockPos flow = pour(helper, flowStone, flowingWater);
        hold(player, jug);
        clickThrough(helper, player, flowStone);
        helper.assertBlockState(flow, flowingWater);
        assertHeld(helper, player, Fluids.WATER, volume, "flowing water");

        BlockState flowingLava = Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 1);
        BlockPos lavaFlowStone = new BlockPos(11, 1, 1);
        BlockPos lavaFlow = pour(helper, lavaFlowStone, flowingLava);
        hold(player, jug);
        clickThrough(helper, player, lavaFlowStone);
        helper.assertBlockState(lavaFlow, flowingLava);
        assertHeld(helper, player, Fluids.LAVA, volume, "flowing lava");

        BlockPos free = new BlockPos(6, 4, 1);
        helper.setBlock(free, Blocks.WATER);
        hold(player, jug);
        Vec3 freeCenter = helper.absoluteVec(new Vec3(free.getX() + 0.5, free.getY() + 0.5, free.getZ() + 0.5));
        placeEyes(player, freeCenter.add(0, 0, -2));
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, freeCenter);
        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertBlockPresent(Blocks.WATER, free);
        assertHeld(helper, player, Fluids.WATER, volume, "use");

        BlockPos slabPos = new BlockPos(6, 1, 1);
        helper.setBlock(slabPos, Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED, true));
        hold(player, jug);
        Vec3 slabTop = helper.absoluteVec(new Vec3(slabPos.getX() + 0.2, slabPos.getY() + 0.5, slabPos.getZ() + 0.2));
        placeEyes(player, slabTop.add(0, 0, -1.5));
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, slabTop);
        player.getMainHandItem().useOn(new UseOnContext(
                player, InteractionHand.MAIN_HAND, new BlockHitResult(slabTop, Direction.UP, helper.absolutePos(slabPos), false)
        ));
        helper.assertBlockProperty(slabPos, SlabBlock.WATERLOGGED, true);
        helper.assertTrue(PotteryItem.contents(player.getMainHandItem()).isEmpty(), "waterlogged slab filled the jug");

        BlockPos bubblePos = new BlockPos(6, 2, 5);
        helper.setBlock(bubblePos, Blocks.BUBBLE_COLUMN.defaultBlockState());
        hold(player, jug);
        Vec3 bubble = helper.absoluteVec(new Vec3(bubblePos.getX() + 0.5, bubblePos.getY() + 0.5, bubblePos.getZ() + 0.5));
        placeEyes(player, bubble.add(0, 0, -1.5));
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, bubble);
        player.getMainHandItem().useOn(new UseOnContext(
                player, InteractionHand.MAIN_HAND, new BlockHitResult(bubble, Direction.SOUTH, helper.absolutePos(bubblePos), false)
        ));
        helper.assertBlockPresent(Blocks.BUBBLE_COLUMN, bubblePos);
        helper.assertTrue(PotteryItem.contents(player.getMainHandItem()).isEmpty(), "bubble column filled the jug");

        hold(player, Pottery.wheelShape());
        clickThrough(helper, player, waterStone);
        helper.assertBlockState(water, Blocks.WATER.defaultBlockState());
        helper.assertTrue(PotteryItem.contents(player.getMainHandItem()).isEmpty(), "lump filled");
        List<Component> lumpLines = tooltip(player.getMainHandItem(), player);
        helper.assertFalse(lumpLines.contains(Component.translatable("tooltip.primalpottery.capacity", 0)), "lump capacity");
        helper.assertFalse(lumpLines.stream().anyMatch(line -> line.getString().contains("mL")), "lump amount");

        BlockPos sneakStone = new BlockPos(7, 1, 1);
        BlockPos sneakWater = pour(helper, sneakStone, Blocks.WATER.defaultBlockState());
        hold(player, jug);
        player.setShiftKeyDown(false);
        clickThrough(helper, player, sneakStone);
        assertHeld(helper, player, Fluids.WATER, volume, "jug for sneaking");
        player.setShiftKeyDown(true);
        Vec3 sneakSurface = new Vec3(sneakStone.getX() + 0.2, sneakStone.getY() + 1, sneakStone.getZ() + 0.2);
        placeEyes(player, helper.absoluteVec(sneakSurface.add(0, 0.5, -2)));
        player.getMainHandItem().useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(helper.absoluteVec(sneakSurface), Direction.UP, helper.absolutePos(sneakStone), false)
        ));
        player.setShiftKeyDown(false);
        helper.assertBlockPresent(ModBlocks.POTTERY, sneakWater);
        helper.assertBlockNotPresent(Blocks.WATER, sneakWater);
        PotteryBlockEntity sneaked = helper.getBlockEntity(sneakWater, PotteryBlockEntity.class);
        assertStored(helper, sneaked.contents(0), Fluids.WATER, volume, "sneak place");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        useItemOn(helper, player, sneakWater, slotPoint(sneakWater, 0), Direction.UP);
        player.setShiftKeyDown(false);
        assertFluid(helper, potteryItemNear(helper, sneakWater), Fluids.WATER, volume, "pickup");

        BlockPos floor = new BlockPos(7, 1, 3);
        helper.setBlock(floor, Blocks.STONE);
        BlockPos pots = floor.above();
        hold(player, jug);
        clickThrough(helper, player, waterStone);
        assertHeld(helper, player, Fluids.WATER, volume, "water jug to place");
        clickOn(helper, player, floor, new Vec3(floor.getX() + 0.2, floor.getY() + 1, floor.getZ() + 0.2), Direction.UP);
        helper.assertBlockPresent(ModBlocks.POTTERY, pots);
        holdStack(player, lavaJug);
        Vec3 secondSlot = new Vec3(pots.getX() + 0.8, pots.getY() + 1, pots.getZ() + 0.2);
        clickOn(helper, player, pots, secondSlot, Direction.UP);
        PotteryBlockEntity both = helper.getBlockEntity(pots, PotteryBlockEntity.class);
        assertStored(helper, both.contents(0), Fluids.WATER, volume, "water slot");
        assertStored(helper, both.contents(1), Fluids.LAVA, volume, "lava slot");

        BlockPos beside = pots.north();
        BlockState besideWater = Blocks.WATER.defaultBlockState();
        helper.setBlock(beside, besideWater);
        hold(player, jug);
        Vec3 slot = PotteryBlockEntity.BOXES.get(2).move(helper.absolutePos(pots)).getCenter();
        placeEyes(player, slot.add(1.5, 0, 0));
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, slot);
        player.getMainHandItem().useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        helper.absoluteVec(new Vec3(pots.getX() + 0.8, pots.getY() + 0.4, pots.getZ() + 0.8)),
                        Direction.NORTH,
                        helper.absolutePos(pots),
                        false
                )
        ));
        helper.assertBlockState(beside, besideWater);
        helper.assertTrue(both.get(2) != null, "adjacent water blocked the merge");
        focusedPots(helper, player);
        lidsAndBreaks(helper, player);
        helper.succeed();
    }

    private static void lidsAndBreaks(GameTestHelper helper, Player player) {
        ((ServerPlayer) player).setGameMode(GameType.SURVIVAL);
        Pottery jug = jug();
        int volume = jug.classify().volume();

        hold(player, jug);
        Vec3 sky = helper.absoluteVec(new Vec3(0.5, 30, 0.5));
        placeEyes(player, sky);
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, sky.add(0, 1, 0));
        InteractionResult used = player.getMainHandItem().getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(used == InteractionResult.SUCCESS, "air use " + used);
        helper.assertTrue(PotteryItem.contents(player.getMainHandItem()).isEmpty(), "air use filled");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "air use consumed the jug");

        hold(player, jug);
        LidSync.apply(player, true, true);
        helper.assertTrue(PotteryItem.lid(player.getMainHandItem()), "put lid");
        CompoundTag written = player.getMainHandItem().get(DataComponents.CUSTOM_DATA).copyTag();
        helper.assertTrue(written.contains("lid"), "lid key");
        LidSync.apply(player, true, false);
        CompoundTag cleared = player.getMainHandItem().get(DataComponents.CUSTOM_DATA).copyTag();
        helper.assertFalse(cleared.contains("lid"), "take lid");
        helper.assertFalse(PotteryItem.lid(player.getMainHandItem()), "lid flag");

        hold(player, Pottery.wheelShape());
        LidSync.apply(player, true, true);
        helper.assertFalse(PotteryItem.lid(player.getMainHandItem()), "lump lid");

        ItemStack lidded = PotteryItem.stack(jug, VesselContents.of(Fluids.WATER, volume), true);
        ItemStack open = PotteryItem.stack(jug);
        BlockPos pots = placePair(helper, player, new BlockPos(10, 1, 1), lidded, open);
        PotteryBlockEntity entity = helper.getBlockEntity(pots, PotteryBlockEntity.class);
        helper.assertTrue(entity.lid(0), "placed lid");
        helper.assertFalse(entity.lid(1), "open neighbor");
        assertStored(helper, entity.contents(0), Fluids.WATER, volume, "lidded water");
        helper.assertTrue(entity.contents(1).isEmpty(), "open neighbor holds fluid");

        CompoundTag saved = entity.saveCustomOnly(helper.getLevel().registryAccess());
        PotteryBlockEntity loaded = new PotteryBlockEntity(entity.getBlockPos(), entity.getBlockState());
        loaded.loadCustomOnly(TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved
        ));
        helper.assertTrue(loaded.lid(0) && !loaded.lid(1), "saved lids");
        helper.assertTrue(loaded.get(0) != null && loaded.get(1) != null, "saved pots");
        assertStored(helper, loaded.contents(0), Fluids.WATER, volume, "saved water");

        BlockPos absolute = helper.absolutePos(pots);
        PlacedLidSync.apply(player, absolute, 0, false);
        helper.assertFalse(entity.lid(0), "take placed lid");
        helper.assertFalse(entity.lid(1), "neighbor gained a lid");
        CompoundTag placedCleared = entity.saveCustomOnly(helper.getLevel().registryAccess());
        CompoundTag taken = Nbt.compoundAt(Nbt.list(Nbt.compound(placedCleared, "pottery"), "slots"), 0);
        helper.assertFalse(taken.contains("lid"), "lid key");
        PlacedLidSync.apply(player, absolute, 0, true);
        helper.assertTrue(entity.lid(0) && !entity.lid(1), "put placed lid");

        BlockPos lumpFloor = new BlockPos(12, 1, 6);
        helper.setBlock(lumpFloor, Blocks.STONE);
        hold(player, Pottery.wheelShape());
        useItemOn(helper, player, lumpFloor, topPoint(lumpFloor, 0), Direction.UP);
        BlockPos lumpPos = lumpFloor.above();
        helper.assertBlockPresent(ModBlocks.POTTERY, lumpPos);
        PotteryBlockEntity lump = helper.getBlockEntity(lumpPos, PotteryBlockEntity.class);
        PlacedLidSync.apply(player, helper.absolutePos(lumpPos), 0, true);
        helper.assertFalse(lump.lid(0), "lump lid");
        helper.assertTrue(lump.get(0) != null, "lump was removed");

        BlockState state = helper.getBlockState(pots);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        float progress = state.getDestroyProgress(player, helper.getLevel(), absolute);
        helper.assertTrue(progress > 0.0F && progress < 1.0F, "destroy progress " + progress);
        // The two-argument getCollisionShape returns the shape cached at startup, which is empty
        // because no pot exists then. The block methods are what the outline and entity collision use.
        assertFootprint(helper, pots, 0, 1);

        Vec3 gap = helper.absoluteVec(new Vec3(pots.getX() + 0.5, pots.getY() + 0.2, pots.getZ() + 0.5));
        placeEyes(player, gap.add(0, 2, 0));
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, gap);
        ((ServerPlayer) player).gameMode.destroyBlock(absolute);
        helper.assertTrue(entity.get(0) != null && entity.get(1) != null, "a miss removed a pot");
        helper.assertBlockPresent(ModBlocks.POTTERY, pots);

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        clearPottery(helper);
        player.setShiftKeyDown(true);
        useItemOn(helper, player, pots, slotPoint(pots, 1), Direction.UP);
        player.setShiftKeyDown(false);
        ItemStack picked = potteryItemNear(helper, pots);
        helper.assertFalse(PotteryItem.lid(picked), "picked the lidded pot");
        helper.assertTrue(PotteryItem.contents(picked).isEmpty(), "picked the water");
        helper.assertTrue(entity.get(0) != null && entity.lid(0), "aimed pickup took the other pot");
        helper.assertTrue(entity.get(1) == null, "aimed pickup missed");

        holdStack(player, open.copy());
        clickOn(helper, player, pots, new Vec3(pots.getX() + 0.8, pots.getY() + 1, pots.getZ() + 0.2), Direction.UP);
        helper.assertTrue(entity.get(1) != null && !entity.lid(1), "neighbor replaced");

        clearPottery(helper);
        aim(player, PotteryBlockEntity.BOXES.get(0).move(absolute).getCenter());
        ((ServerPlayer) player).gameMode.destroyBlock(absolute);
        var drops = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(absolute).inflate(2),
                dropped -> dropped.getItem().is(ModItems.POTTERY)
        );
        helper.assertValueEqual(drops.size(), 1, "break drops");
        ItemStack broken = drops.getFirst().getItem();
        helper.assertTrue(PotteryItem.lid(broken), "break dropped the lid");
        assertFluid(helper, broken, Fluids.WATER, volume, "break");
        helper.assertValueEqual(PotteryItem.read(broken).discs(), jug.discs(), "break profile");
        helper.assertTrue(entity.get(0) == null, "break removed both");
        helper.assertTrue(entity.get(1) != null && !entity.lid(1), "neighbor survived");
        helper.assertBlockPresent(ModBlocks.POTTERY, pots);

        clearPottery(helper);
        aim(player, PotteryBlockEntity.BOXES.get(1).move(absolute).getCenter());
        ((ServerPlayer) player).gameMode.destroyBlock(absolute);
        helper.assertBlockNotPresent(ModBlocks.POTTERY, pots);

        ((ServerPlayer) player).setGameMode(GameType.SURVIVAL);
        BlockPos creativePots = placePair(helper, player, new BlockPos(10, 1, 4), lidded, open);
        ((ServerPlayer) player).setGameMode(GameType.CREATIVE);
        clearPottery(helper);
        BlockPos creativeAbsolute = helper.absolutePos(creativePots);
        aim(player, PotteryBlockEntity.BOXES.get(0).move(creativeAbsolute).getCenter());
        ((ServerPlayer) player).gameMode.destroyBlock(creativeAbsolute);
        PotteryBlockEntity creativeEntity = helper.getBlockEntity(creativePots, PotteryBlockEntity.class);
        helper.assertTrue(creativeEntity.get(0) == null, "creative removed both");
        helper.assertTrue(creativeEntity.get(1) != null, "creative removed the neighbor");
        helper.assertBlockPresent(ModBlocks.POTTERY, creativePots);
        var leftover = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(creativeAbsolute).inflate(2),
                dropped -> dropped.getItem().is(ModItems.POTTERY)
        );
        helper.assertTrue(leftover.isEmpty(), "creative dropped a pot");
        ((ServerPlayer) player).setGameMode(GameType.SURVIVAL);
    }

    private static BlockPos placePair(GameTestHelper helper, Player player, BlockPos floor, ItemStack first, ItemStack second) {
        helper.setBlock(floor, Blocks.STONE);
        holdStack(player, first.copy());
        clickOn(helper, player, floor, new Vec3(floor.getX() + 0.2, floor.getY() + 1, floor.getZ() + 0.2), Direction.UP);
        BlockPos pots = floor.above();
        helper.assertBlockPresent(ModBlocks.POTTERY, pots);
        holdStack(player, second.copy());
        clickOn(helper, player, pots, new Vec3(pots.getX() + 0.8, pots.getY() + 1, pots.getZ() + 0.2), Direction.UP);
        PotteryBlockEntity placed = helper.getBlockEntity(pots, PotteryBlockEntity.class);
        helper.assertTrue(placed.get(0) != null && placed.get(1) != null, "pair was not placed");
        return pots;
    }

    private static void clearPottery(GameTestHelper helper) {
        // The empty structure is small. Pots placed at relative x=10 sit outside bounds.inflate(8),
        // so a pickup drop there survived and was counted as the break drop.
        BlockPos min = helper.absolutePos(BlockPos.ZERO);
        BlockPos max = helper.absolutePos(new BlockPos(16, 8, 16));
        AABB box = new AABB(min.getX() - 2, min.getY() - 2, min.getZ() - 2, max.getX() + 2, max.getY() + 2, max.getZ() + 2);
        helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                box,
                entity -> entity.getItem().is(ModItems.POTTERY)
        ).forEach(ItemEntity::discard);
    }

    private static boolean coloredLine(List<Component> lines, String text, int color) {
        for (Component line : lines) {
            if (styledText(line, text, color)) {
                return true;
            }
        }
        return false;
    }

    private static boolean plainLine(List<Component> lines, String text) {
        for (Component line : lines) {
            if (styledText(line, text, -1)) {
                return true;
            }
        }
        return false;
    }

    private static boolean styledText(Component component, String text, int color) {
        boolean colorMatches = color < 0
                ? component.getStyle().getColor() == null
                : component.getStyle().getColor() != null && component.getStyle().getColor().getValue() == color;
        if (component instanceof MutableComponent && component.getString().equals(text) && colorMatches) {
            return true;
        }
        for (Component sibling : component.getSiblings()) {
            if (styledText(sibling, text, color)) {
                return true;
            }
        }
        if (component.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents) {
            for (Object argument : contents.getArgs()) {
                if (argument instanceof Component child && styledText(child, text, color)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Player mockPlayer(GameTestHelper helper) {
        return helper.makeMockServerPlayerInLevel();
    }

    private static Pottery jug() {
        return new Pottery(List.of(
                new PotteryDisc(1, 4, 1),
                new PotteryDisc(2, 5, 3),
                new PotteryDisc(1, 5, 0)
        ));
    }

    private static BlockPos pour(GameTestHelper helper, BlockPos solid, BlockState fluid) {
        helper.setBlock(solid, Blocks.STONE);
        BlockPos cell = solid.above();
        helper.setBlock(cell, fluid);
        return cell;
    }

    private static void hold(Player player, Pottery pottery) {
        holdStack(player, PotteryItem.stack(pottery));
    }

    private static void holdStack(Player player, ItemStack stack) {
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    private static void clickThrough(GameTestHelper helper, Player player, BlockPos solid) {
        clickOn(helper, player, solid, new Vec3(solid.getX() + 0.2, solid.getY() + 1, solid.getZ() + 0.2), Direction.UP);
    }

    private static void clickOn(GameTestHelper helper, Player player, BlockPos block, Vec3 at, Direction face) {
        Vec3 absolute = helper.absoluteVec(at);
        placeEyes(player, absolute.add(0, 0.5, -2));
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, absolute);
        player.getMainHandItem().useOn(new UseOnContext(
                player, InteractionHand.MAIN_HAND, new BlockHitResult(absolute, face, helper.absolutePos(block), false)
        ));
    }

    private static InteractionResult useItemOn(GameTestHelper helper, Player player, BlockPos block, Vec3 at, Direction face) {
        ServerPlayer server = (ServerPlayer) player;
        Vec3 absolute = helper.absoluteVec(at);
        placeEyes(player, absolute.add(0, 0.5, -2));
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, absolute);
        return server.gameMode.useItemOn(
                server,
                helper.getLevel(),
                player.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND,
                new BlockHitResult(absolute, face, helper.absolutePos(block), false)
        );
    }

    private static Vec3 slotPoint(BlockPos relative, int slot) {
        Vec3 center = PotteryBlockEntity.box(slot).getCenter();
        return new Vec3(relative.getX() + center.x, relative.getY() + center.y, relative.getZ() + center.z);
    }

    private static Vec3 topPoint(BlockPos floor, int slot) {
        Vec3 center = PotteryBlockEntity.box(slot).getCenter();
        return new Vec3(floor.getX() + center.x, floor.getY() + 1, floor.getZ() + center.z);
    }

    private static void assertFootprint(GameTestHelper helper, BlockPos relative, int... occupied) {
        BlockPos absolute = helper.absolutePos(relative);
        BlockState state = helper.getBlockState(relative);
        CollisionContext context = CollisionContext.empty();
        var shape = state.getShape(helper.getLevel(), absolute, context);
        var collision = state.getCollisionShape(helper.getLevel(), absolute, context);
        for (int index = 0; index < PotteryBlockEntity.SLOTS; index++) {
            var box = Shapes.create(PotteryBlockEntity.box(index));
            helper.assertFalse(Shapes.joinIsNotEmpty(shape, box, BooleanOp.ONLY_SECOND), "outline missing " + index);
            boolean filled = false;
            for (int slot : occupied) {
                if (slot == index) {
                    filled = true;
                    break;
                }
            }
            helper.assertTrue(Shapes.joinIsNotEmpty(collision, box, BooleanOp.AND) == filled, "collision slot " + index);
        }
    }

    private static void assertNoPottery(GameTestHelper helper, BlockPos relative, String label) {
        var drops = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(helper.absolutePos(relative)).inflate(2),
                entity -> entity.getItem().is(ModItems.POTTERY)
        );
        helper.assertTrue(drops.isEmpty(), label);
    }

    private static void focusedPots(GameTestHelper helper, Player player) {
        ((ServerPlayer) player).setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(false);
        Pottery jug = jug();
        int volume = jug.classify().volume();
        helper.assertValueEqual(volume, 54, "jug volume");

        BlockPos floor = new BlockPos(1, 1, 10);
        helper.setBlock(floor, Blocks.STONE);
        holdStack(player, PotteryItem.stack(jug, VesselContents.of(Fluids.WATER, 10)));
        useItemOn(helper, player, floor, topPoint(floor, 3), Direction.UP);
        BlockPos pots = floor.above();
        helper.assertBlockPresent(ModBlocks.POTTERY, pots);
        helper.assertBlockNotPresent(ModBlocks.POTTERY, pots.above());
        helper.assertBlockNotPresent(ModBlocks.POTTERY, pots.north());
        PotteryBlockEntity entity = helper.getBlockEntity(pots, PotteryBlockEntity.class);
        helper.assertTrue(entity.get(3) != null && entity.get(0) == null, "far column slot");
        assertStored(helper, entity.contents(3), Fluids.WATER, 10, "far slot");
        assertFootprint(helper, pots, 3);

        clearPottery(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        useItemOn(helper, player, pots, slotPoint(pots, 3), Direction.UP);
        assertNoPottery(helper, pots, "empty click dropped a pot");
        assertStored(helper, entity.contents(3), Fluids.WATER, 10, "empty click");
        helper.assertTrue(entity.get(0) == null, "empty click filled the front slot");

        clearPottery(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        useItemOn(helper, player, pots, slotPoint(pots, 0), Direction.UP);
        player.setShiftKeyDown(false);
        assertNoPottery(helper, pots, "shift on an empty box dropped a pot");
        helper.assertTrue(entity.get(3) != null && entity.get(0) == null, "shift on an empty box removed the far pot");
        assertStored(helper, entity.contents(3), Fluids.WATER, 10, "empty box shift");

        holdStack(player, PotteryItem.stack(jug, VesselContents.of(Fluids.LAVA, 20)));
        useItemOn(helper, player, pots, slotPoint(pots, 0), Direction.UP);
        helper.assertTrue(entity.get(0) != null, "front slot stayed empty");
        assertStored(helper, entity.contents(0), Fluids.LAVA, 20, "front slot");
        assertStored(helper, entity.contents(3), Fluids.WATER, 10, "far slot changed");
        helper.assertBlockNotPresent(ModBlocks.POTTERY, pots.above());
        helper.assertTrue(player.getMainHandItem().isEmpty(), "filled vessel was not placed");

        hold(player, jug);
        useItemOn(helper, player, pots, slotPoint(pots, 1), Direction.UP);
        hold(player, jug);
        useItemOn(helper, player, pots, slotPoint(pots, 2), Direction.UP);
        helper.assertTrue(entity.get(1) != null && entity.get(2) != null, "side slots");
        assertStored(helper, entity.contents(0), Fluids.LAVA, 20, "front slot after fill");
        assertStored(helper, entity.contents(3), Fluids.WATER, 10, "far slot after fill");
        assertFootprint(helper, pots, 0, 1, 2, 3);

        hold(player, jug);
        useItemOn(helper, player, pots, slotPoint(pots, 0), Direction.UP);
        helper.assertTrue(player.getMainHandItem().is(ModItems.POTTERY), "fifth click placed the jug");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "fifth click consumed the jug");
        helper.assertTrue(PotteryItem.contents(player.getMainHandItem()).isEmpty(), "fifth click poured");
        assertStored(helper, entity.contents(0), Fluids.LAVA, 20, "fifth click changed the front slot");
        assertStored(helper, entity.contents(3), Fluids.WATER, 10, "fifth click changed the far slot");
        helper.assertBlockNotPresent(ModBlocks.POTTERY, pots.above());
        helper.assertBlockNotPresent(ModBlocks.POTTERY, pots.north());
        for (int index = 0; index < PotteryBlockEntity.SLOTS; index++) {
            helper.assertTrue(entity.get(index) != null, "slot " + index + " emptied");
        }

        pourAndBuckets(helper, player, jug, volume);
        focusedBreaks(helper, player, jug);
        player.setShiftKeyDown(false);
    }

    private static void pourAndBuckets(GameTestHelper helper, Player player, Pottery jug, int volume) {
        BlockPos floor = new BlockPos(4, 1, 10);
        helper.setBlock(floor, Blocks.STONE);
        hold(player, jug);
        useItemOn(helper, player, floor, topPoint(floor, 0), Direction.UP);
        BlockPos pots = floor.above();
        PotteryBlockEntity entity = helper.getBlockEntity(pots, PotteryBlockEntity.class);
        helper.assertTrue(entity.contents(0).isEmpty(), "placed jug was not empty");
        helper.assertBlockProperty(pots, PotteryBlock.LIGHT, 0);

        holdStack(player, PotteryItem.stack(jug, VesselContents.of(Fluids.WATER, 20), true));
        useItemOn(helper, player, pots, slotPoint(pots, 0), Direction.UP);
        helper.assertTrue(entity.contents(0).isEmpty(), "lidded jug poured");
        assertFluid(helper, player.getMainHandItem(), Fluids.WATER, 20, "lidded jug lost fluid");
        helper.assertTrue(PotteryItem.lid(player.getMainHandItem()), "pour removed the lid");
        helper.assertBlockProperty(pots, PotteryBlock.LIGHT, 0);

        holdStack(player, PotteryItem.stack(jug, VesselContents.of(Fluids.WATER, 20)));
        useItemOn(helper, player, pots, slotPoint(pots, 0), Direction.UP);
        assertStored(helper, entity.contents(0), Fluids.WATER, 20, "poured twenty");
        helper.assertTrue(PotteryItem.contents(player.getMainHandItem()).isEmpty(), "held fluid remained");
        helper.assertValueEqual(PotteryItem.read(player.getMainHandItem()).discs(), jug.discs(), "held profile");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "pour shrank the jug");
        helper.assertFalse(entity.lid(0), "pour closed the pot");
        helper.assertBlockProperty(pots, PotteryBlock.LIGHT, 0);

        entity.setContents(0, VesselContents.of(Fluids.WATER, 10));
        holdStack(player, PotteryItem.stack(jug, VesselContents.of(Fluids.WATER, 100)));
        useItemOn(helper, player, pots, slotPoint(pots, 0), Direction.UP);
        assertStored(helper, entity.contents(0), Fluids.WATER, volume, "poured the room");
        assertFluid(helper, player.getMainHandItem(), Fluids.WATER, 56, "remainder");
        helper.assertFalse(PotteryItem.lid(player.getMainHandItem()), "remainder gained a lid");
        helper.assertValueEqual(PotteryItem.read(player.getMainHandItem()).discs(), jug.discs(), "remainder profile");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "remainder count");

        holdStack(player, PotteryItem.stack(jug, VesselContents.of(Fluids.WATER, 20), true));
        useItemOn(helper, player, pots, slotPoint(pots, 0), Direction.UP);
        assertStored(helper, entity.contents(0), Fluids.WATER, volume, "full pot");
        assertFluid(helper, player.getMainHandItem(), Fluids.WATER, 20, "full pot took fluid");
        helper.assertTrue(PotteryItem.lid(player.getMainHandItem()), "full pot cleared the held lid");

        entity.setLid(0, true);
        holdStack(player, PotteryItem.stack(jug, VesselContents.of(Fluids.WATER, 20)));
        useItemOn(helper, player, pots, slotPoint(pots, 0), Direction.UP);
        assertStored(helper, entity.contents(0), Fluids.WATER, volume, "lidded pot");
        helper.assertTrue(entity.lid(0), "pour opened the lid");
        assertFluid(helper, player.getMainHandItem(), Fluids.WATER, 20, "lidded pot took fluid");

        hold(player, Pottery.wheelShape());
        useItemOn(helper, player, pots, slotPoint(pots, 1), Direction.UP);
        helper.assertTrue(entity.get(1) != null, "lump was not placed");
        holdStack(player, PotteryItem.stack(jug, VesselContents.of(Fluids.WATER, 20)));
        useItemOn(helper, player, pots, slotPoint(pots, 1), Direction.UP);
        helper.assertTrue(entity.contents(1).isEmpty(), "lump took fluid");
        helper.assertFalse(entity.lid(1), "lump closed");
        assertFluid(helper, player.getMainHandItem(), Fluids.WATER, 20, "lump pour");

        holdStack(player, PotteryItem.stack(jug, VesselContents.of(Fluids.LAVA, 10)));
        useItemOn(helper, player, pots, slotPoint(pots, 2), Direction.UP);
        assertStored(helper, entity.contents(2), Fluids.LAVA, 10, "lava pot");
        helper.assertFalse(entity.lid(2), "lava pot closed itself");
        helper.assertBlockProperty(pots, PotteryBlock.LIGHT, 15);
        entity.setLid(2, true);
        helper.assertBlockProperty(pots, PotteryBlock.LIGHT, 0);
        entity.setLid(2, false);
        helper.assertBlockProperty(pots, PotteryBlock.LIGHT, 15);
        holdStack(player, PotteryItem.stack(jug, VesselContents.of(Fluids.WATER, 20)));
        useItemOn(helper, player, pots, slotPoint(pots, 2), Direction.UP);
        assertStored(helper, entity.contents(2), Fluids.LAVA, 10, "mixed fluids");
        assertFluid(helper, player.getMainHandItem(), Fluids.WATER, 20, "mixed pour");

        holdStack(player, PotteryItem.stack(jug, VesselContents.of(Fluids.WATER, 40), true));
        player.setShiftKeyDown(true);
        useItemOn(helper, player, pots, slotPoint(pots, 0), Direction.UP);
        player.setShiftKeyDown(false);
        assertStored(helper, entity.contents(0), Fluids.WATER, volume, "sneak poured");
        helper.assertTrue(entity.lid(0), "sneak opened the lid");
        helper.assertTrue(entity.get(3) != null, "sneak did not place");
        assertStored(helper, entity.contents(3), Fluids.WATER, 40, "sneak place");
        helper.assertTrue(entity.lid(3), "sneak dropped the lid");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "sneak kept the jug");

        VesselContents kept = entity.contents(0);
        boolean keptLid = entity.lid(0);
        // 26.3 BucketItem places and picks up in Item.use. The client calls that after a block click that does not consume.
        bucketClick(helper, player, pots, entity, Items.BUCKET, kept, keptLid, "empty bucket");
        bucketClick(helper, player, pots, entity, Items.WATER_BUCKET, kept, keptLid, "water bucket");
        bucketClick(helper, player, pots, entity, Items.LAVA_BUCKET, kept, keptLid, "lava bucket");
    }

    private static void bucketClick(
            GameTestHelper helper,
            Player player,
            BlockPos pots,
            PotteryBlockEntity entity,
            Item item,
            VesselContents kept,
            boolean keptLid,
            String label
    ) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        InteractionResult clicked = useItemOn(helper, player, pots, slotPoint(pots, 0), Direction.UP);
        helper.assertFalse(clicked.consumesAction(), label + " opened the pot");
        ItemStack hand = player.getMainHandItem();
        if (!hand.isEmpty()) {
            hand.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        }
        helper.assertBlockPresent(ModBlocks.POTTERY, pots);
        helper.assertTrue(entity.get(0) != null, label + " removed the pot");
        assertStored(helper, entity.contents(0), kept.fluid(), kept.amount(), label);
        helper.assertTrue(entity.lid(0) == keptLid, label + " changed the lid");
    }

    private static void focusedBreaks(GameTestHelper helper, Player player, Pottery jug) {
        ((ServerPlayer) player).setGameMode(GameType.SURVIVAL);
        BlockPos floor = new BlockPos(8, 1, 10);
        helper.setBlock(floor, Blocks.STONE);
        holdStack(player, PotteryItem.stack(jug, VesselContents.of(Fluids.WATER, 20), true));
        useItemOn(helper, player, floor, topPoint(floor, 0), Direction.UP);
        BlockPos pots = floor.above();
        holdStack(player, PotteryItem.stack(jug, VesselContents.of(Fluids.LAVA, 5)));
        useItemOn(helper, player, pots, slotPoint(pots, 1), Direction.UP);
        PotteryBlockEntity entity = helper.getBlockEntity(pots, PotteryBlockEntity.class);
        helper.assertTrue(entity.lid(0) && !entity.lid(1), "break setup lids");
        assertStored(helper, entity.contents(1), Fluids.LAVA, 5, "pot B");

        clearPottery(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BlockPos absolute = helper.absolutePos(pots);
        aim(player, PotteryBlockEntity.BOXES.get(0).move(absolute).getCenter());
        ((ServerPlayer) player).gameMode.destroyBlock(absolute);
        var drops = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(absolute).inflate(2),
                dropped -> dropped.getItem().is(ModItems.POTTERY)
        );
        helper.assertValueEqual(drops.size(), 1, "focused break drops");
        ItemStack broken = drops.getFirst().getItem();
        helper.assertTrue(PotteryItem.lid(broken), "break dropped A's lid");
        assertFluid(helper, broken, Fluids.WATER, 20, "break A");
        helper.assertTrue(entity.get(0) == null, "break left A");
        helper.assertTrue(entity.get(1) != null, "break removed B");
        assertStored(helper, entity.contents(1), Fluids.LAVA, 5, "pot B after break");
        helper.assertBlockPresent(ModBlocks.POTTERY, pots);

        BlockPos behindFloor = new BlockPos(11, 1, 10);
        helper.setBlock(behindFloor, Blocks.STONE);
        holdStack(player, PotteryItem.stack(jug, VesselContents.of(Fluids.WATER, 20), true));
        useItemOn(helper, player, behindFloor, topPoint(behindFloor, 3), Direction.UP);
        BlockPos behind = behindFloor.above();
        holdStack(player, PotteryItem.stack(jug, VesselContents.of(Fluids.LAVA, 5)));
        useItemOn(helper, player, behind, slotPoint(behind, 1), Direction.UP);
        PotteryBlockEntity column = helper.getBlockEntity(behind, PotteryBlockEntity.class);
        helper.assertTrue(column.get(0) == null && column.get(3) != null && column.get(1) != null, "empty front");

        clearPottery(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BlockPos behindAbsolute = helper.absolutePos(behind);
        Vec3 front = PotteryBlockEntity.BOXES.get(0).move(behindAbsolute).getCenter();
        Vec3 far = PotteryBlockEntity.BOXES.get(3).move(behindAbsolute).getCenter();
        placeEyes(player, front.add(0, 0, -1));
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, far);
        ((ServerPlayer) player).gameMode.destroyBlock(behindAbsolute);
        assertNoPottery(helper, behind, "empty box in front dropped a pot");
        helper.assertTrue(column.get(3) != null && column.get(1) != null, "empty box in front removed a pot");
        helper.assertTrue(column.lid(3), "empty box in front took the lid");
        assertStored(helper, column.contents(3), Fluids.WATER, 20, "far pot");
        assertStored(helper, column.contents(1), Fluids.LAVA, 5, "side pot");
        helper.assertBlockPresent(ModBlocks.POTTERY, behind);
    }

    private static void assertHeld(GameTestHelper helper, Player player, Fluid fluid, int amount, String label) {
        assertFluid(helper, player.getMainHandItem(), fluid, amount, label);
    }

    private static void assertFluid(GameTestHelper helper, ItemStack stack, Fluid fluid, int amount, String label) {
        VesselContents contents = PotteryItem.contents(stack);
        helper.assertTrue(contents.fluid() == fluid, label + " stored " + BuiltInRegistries.FLUID.getKey(contents.fluid()));
        helper.assertValueEqual(contents.amount(), amount, label + " amount");
    }

    private static void assertStored(GameTestHelper helper, VesselContents contents, Fluid fluid, int amount, String label) {
        helper.assertTrue(contents.fluid() == fluid, label + " stored " + BuiltInRegistries.FLUID.getKey(contents.fluid()));
        helper.assertValueEqual(contents.amount(), amount, label + " amount");
    }

    private static List<Component> tooltip(ItemStack stack, Player player) {
        return stack.getTooltipLines(Item.TooltipContext.EMPTY, player, TooltipFlag.Default.NORMAL);
    }

    private static final class Vessels extends GameTestInstance {
        private static final MapCodec<Vessels> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                TestData.CODEC.forGetter(Vessels::data)
        ).apply(instance, Vessels::new));

        private Vessels(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        private TestData<Holder<TestEnvironmentDefinition<?>>> data() {
            return this.info();
        }

        @Override
        public void run(GameTestHelper helper) {
            vessels(helper);
        }

        @Override
        public MapCodec<Vessels> codec() {
            return CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("vessels");
        }
    }

    private static final class Wheel extends GameTestInstance {
        private static final MapCodec<Wheel> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                TestData.CODEC.forGetter(Wheel::data)
        ).apply(instance, Wheel::new));

        private Wheel(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        private TestData<Holder<TestEnvironmentDefinition<?>>> data() {
            return this.info();
        }

        @Override
        public void run(GameTestHelper helper) {
            wheel(helper);
        }

        @Override
        public MapCodec<Wheel> codec() {
            return CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("wheel");
        }
    }
}
