package net.modcrafters.minecraft.primalpottery.neoforge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.modcrafters.minecraft.primalpottery.Ids;
import net.modcrafters.minecraft.primalpottery.block.ModBlocks;
import net.modcrafters.minecraft.primalpottery.block.PotteryBlockEntity;
import net.modcrafters.minecraft.primalpottery.block.PotteryWheelBlockEntity;
import net.modcrafters.minecraft.primalpottery.item.ModItems;
import net.modcrafters.minecraft.primalpottery.item.PotteryItem;
import net.modcrafters.minecraft.primalpottery.item.TemplateItem;
import net.modcrafters.minecraft.primalpottery.network.TemplateSync;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryDisc;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryType;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

public final class PotteryGameTests {
    private PotteryGameTests() {
    }

    public static MapCodec<? extends GameTestInstance> codec() {
        return Wheel.CODEC;
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(Ids.of("empty"), new TestEnvironmentDefinition.AllOf());
        event.registerTest(
                Ids.of("wheel"),
                new Wheel(new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 100, 0, true))
        );
    }

    private static void wheel(GameTestHelper helper) {
        BlockPos wheelPos = new BlockPos(1, 2, 1);
        helper.setBlock(wheelPos, ModBlocks.WHEEL);
        Player player = helper.makeMockServerPlayerInLevel();
        PotteryWheelBlockEntity wheel = helper.getBlockEntity(wheelPos, PotteryWheelBlockEntity.class);

        checkTemplateCycle(helper, player);

        look(helper, player, 1.5, 2.2, -0.2, 1.5, 2.2, 1.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.useBlock(wheelPos, player);
        helper.assertValueEqual(wheel.spinTicks(), 24, "spin ticks");

        ItemStack clay = new ItemStack(Items.CLAY_BALL);
        player.setItemInHand(InteractionHand.MAIN_HAND, clay);
        look(helper, player, 1.5, 4.0, 1.5, 1.5, 2.84, 1.5);
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
        look(helper, player, 3.25, 4.0, 1.25, 3.25, 2.2, 1.25);
        helper.useBlock(floor.above(), player);
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

    private static void look(GameTestHelper helper, Player player, double x, double y, double z, double lookX, double lookY, double lookZ) {
        Vec3 eye = helper.absoluteVec(new Vec3(x, y, z));
        player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, helper.absoluteVec(new Vec3(lookX, lookY, lookZ)));
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
