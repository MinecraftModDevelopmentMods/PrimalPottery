package net.modcrafters.minecraft.primalpottery.pottery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.Registry;
import net.modcrafters.minecraft.primalpottery.Ids;
import net.modcrafters.minecraft.primalpottery.block.ModBlocks;
import net.modcrafters.minecraft.primalpottery.client.Sprites;
import net.modcrafters.minecraft.primalpottery.item.ModItems;
import net.modcrafters.minecraft.primalpottery.item.PotteryItem;
import net.modcrafters.minecraft.primalpottery.item.RawPotteryItem;
import net.modcrafters.minecraft.primalpottery.recipe.FirePotteryRecipe;
import net.modcrafters.minecraft.primalpottery.recipe.PaintPotteryRecipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PotteryTest {
    private static final Map<String, Integer> DYE_COLORS = Map.ofEntries(
            Map.entry("white", 0xFFFFFF),
            Map.entry("orange", 0xFF5500),
            Map.entry("magenta", 0xFF2BD6),
            Map.entry("light_blue", 0x2ECFFF),
            Map.entry("yellow", 0xFFEA00),
            Map.entry("lime", 0x7DFF1A),
            Map.entry("pink", 0xFF5C9D),
            Map.entry("gray", 0x5A5A5A),
            Map.entry("light_gray", 0xC6C6C6),
            Map.entry("cyan", 0x00E5E5),
            Map.entry("purple", 0xC43BFF),
            Map.entry("blue", 0x2F4BFF),
            Map.entry("brown", 0xC46A28),
            Map.entry("green", 0x3DCC12),
            Map.entry("red", 0xFF2A2A),
            Map.entry("black", 0x1A1A1A)
    );

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Freezing the registries drops the intrusive-holder map. ModItems still constructs its
        // blocks through that map, so put it back before the first touch.
        openIntrusiveHolders();
    }

    private static boolean componentsBound(net.minecraft.world.item.Item item) {
        try {
            item.components();
            return true;
        } catch (NullPointerException unbound) {
            return false;
        }
    }

    private static void openIntrusiveHolders() {
        try {
            Class<?> type = Class.forName("net.minecraft.core.MappedRegistry");
            java.lang.reflect.Field holders = null;
            java.lang.reflect.Field frozen = null;
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                if (holders == null && java.util.Map.class.isAssignableFrom(field.getType()) && field.getName().toLowerCase(java.util.Locale.ROOT).contains("intrusive")) {
                    holders = field;
                }
                if (frozen == null && field.getType() == boolean.class && field.getName().toLowerCase(java.util.Locale.ROOT).contains("frozen")) {
                    frozen = field;
                }
            }
            if (holders == null) {
                throw new IllegalStateException("no intrusive holder field");
            }
            holders.setAccessible(true);
            if (frozen != null) {
                frozen.setAccessible(true);
            }
            for (Object registry : List.of(BuiltInRegistries.BLOCK, BuiltInRegistries.ITEM)) {
                if (frozen != null) {
                    frozen.set(registry, false);
                }
                if (holders.get(registry) == null) {
                    holders.set(registry, new java.util.IdentityHashMap<>());
                }
            }
            Class.forName("net.modcrafters.minecraft.primalpottery.item.ModItems");
            Registry.register(BuiltInRegistries.BLOCK, ModBlocks.WHEEL_ID, ModBlocks.WHEEL);
            Registry.register(BuiltInRegistries.BLOCK, ModBlocks.POTTERY_ID, ModBlocks.POTTERY);
            Registry.register(BuiltInRegistries.ITEM, ModItems.WHEEL_ID, ModItems.WHEEL);
            Registry.register(BuiltInRegistries.ITEM, ModItems.POTTERY_ID, ModItems.POTTERY);
            Registry.register(BuiltInRegistries.ITEM, ModItems.RAW_POTTERY_ID, ModItems.RAW_POTTERY);
            Registry.register(BuiltInRegistries.ITEM, ModItems.BLUEPRINT_ID, ModItems.BLUEPRINT);
            Registry.register(BuiltInRegistries.ITEM, ModItems.RAW_TEMPLATE_ID, ModItems.RAW_TEMPLATE);
            java.lang.reflect.Method bind = null;
            java.lang.reflect.Method holderOf = null;
            Object empty = null;
            for (net.minecraft.world.item.Item item : BuiltInRegistries.ITEM) {
                if (componentsBound(item)) {
                    continue;
                }
                if (bind == null) {
                    empty = net.minecraft.core.component.DataComponentMap.class.getField("EMPTY").get(null);
                    bind = net.minecraft.core.Holder.Reference.class.getMethod(
                            "bindComponents", net.minecraft.core.component.DataComponentMap.class);
                    holderOf = net.minecraft.world.item.Item.class.getMethod("builtInRegistryHolder");
                }
                bind.invoke(holderOf.invoke(item), empty);
            }
            for (Object registry : List.of(BuiltInRegistries.BLOCK, BuiltInRegistries.ITEM)) {
                frozen.set(registry, true);
            }
        } catch (ReflectiveOperationException | ExceptionInInitializerError failure) {
            throw new IllegalStateException(failure);
        }
    }

    @Test
    void classificationUsesTheOpeningAndWidestOutsideDiameter() {
        assertEquals(PotteryType.POT, vessel(5).classify().type());
        assertEquals(PotteryType.JAR, vessel(4.95).classify().type());
        assertEquals(PotteryType.JAR, vessel(2.75).classify().type());
        assertEquals(PotteryType.JUG, vessel(2.7).classify().type());
    }

    @Test
    void capacityUsesSliceAreaAndHeightUntilTheFirstSolidFloor() {
        Pottery pottery = new Pottery(List.of(
                new PotteryDisc(1, 4, 3),
                new PotteryDisc(2, 4, 2),
                new PotteryDisc(1, 4, 0),
                new PotteryDisc(4, 4, 2),
                new PotteryDisc(1, 4, 0)
        ));
        assertEquals(PotteryType.JAR, pottery.classify().type());
        assertEquals(48, pottery.classify().volume());
    }

    @Test
    void thinWallIsALump() {
        Pottery pottery = new Pottery(List.of(new PotteryDisc(2, 0.4, 0)));
        assertEquals(PotteryType.LUMP_THIN, pottery.classify().type());
    }

    @Test
    void missingBottomIsAPipe() {
        Pottery pottery = new Pottery(List.of(
                new PotteryDisc(2, 4, 2), new PotteryDisc(1, 0), new PotteryDisc(1, 4)
        ));
        assertEquals(PotteryType.LUMP_NO_BOTTOM, pottery.classify().type());
    }

    @Test
    void solidClayIsALump() {
        Pottery pottery = new Pottery(List.of(new PotteryDisc(2, 4, 0), new PotteryDisc(2, 4, 0)));
        assertEquals(PotteryType.LUMP, pottery.classify().type());
    }

    @Test
    void wheelWithoutATemplateKeepsTheVase() {
        assertEquals(5, Pottery.formed(null).discs().size());
        assertEquals(2, Pottery.formed(null).discs().getFirst().radius(), 0.0001);
    }

    @Test
    void shapedIsTheOnlyLerp() {
        assertEquals(4, Pottery.BLANK_RADIUS, 0);
        Pottery target = new Pottery(List.of(new PotteryDisc(1, 3, 1)));
        Pottery blank = Pottery.shaped(null, target, 0);
        assertEquals(1, blank.discs().getFirst().height(), 0.0001);
        assertEquals(4, blank.discs().getFirst().radius(), 0.0001);
        assertEquals(0, blank.discs().getFirst().innerRadius(), 0.0001);
        Pottery half = Pottery.shaped(null, target, 0.5);
        assertEquals(3.5, half.discs().getFirst().radius(), 0.0001);
        assertEquals(0.5, half.discs().getFirst().innerRadius(), 0.0001);
        assertSame(target, Pottery.shaped(null, target, 1));
        assertSame(target, Pottery.shaped(null, target, 2));
        assertEquals(blank.discs(), Pottery.shaped(null, target, -0.2).discs());

        Pottery start = new Pottery(List.of(new PotteryDisc(1, 2, 0)));
        Pottery wider = new Pottery(List.of(new PotteryDisc(1, 4, 2)));
        assertEquals(start.discs(), Pottery.shaped(start, wider, 0).discs());
        Pottery mid = Pottery.shaped(start, wider, 0.5);
        assertEquals(3, mid.discs().getFirst().radius(), 0.0001);
        assertEquals(1, mid.discs().getFirst().innerRadius(), 0.0001);

        Pottery stacked = new Pottery(List.of(new PotteryDisc(2, 1, 0), new PotteryDisc(2, 5, 0)));
        Pottery pair = new Pottery(List.of(new PotteryDisc(2, 4, 2), new PotteryDisc(2, 4, 2)));
        Pottery held = Pottery.shaped(stacked, pair, 0);
        assertEquals(1, held.discs().get(0).radius(), 0.0001);
        assertEquals(5, held.discs().get(1).radius(), 0.0001);

        Pottery edge = new Pottery(List.of(new PotteryDisc(1, 1), new PotteryDisc(1, 5)));
        Pottery tall = new Pottery(List.of(new PotteryDisc(2, 4)));
        assertEquals(5, Pottery.shaped(edge, tall, 0).discs().getFirst().radius(), 0.0001);

        Pottery vase = Pottery.shaped(null, Pottery.formed(null), 0.5);
        assertEquals(3, vase.discs().getFirst().radius(), 0.0001);
        assertEquals(Pottery.formed(null).discs().size(), vase.discs().size());
        assertTrue(Pottery.validProfile(vase.discs()));
        assertTrue(Pottery.validProfile(Pottery.shaped(stacked, pair, 0.5).discs()));
        assertEquals(Pottery.formed(null).discs(), Pottery.shaped(null, Pottery.formed(null), 1).discs());
    }

    @Test
    void itemNameUsesJugBowlOrPottery() {
        assertEquals(Component.translatable("pottery.primalpottery.shape.bowl"), vessel(5).classify().type().shapeName());
        assertEquals(Component.translatable("pottery.primalpottery.shape.pottery"), vessel(4).classify().type().shapeName());
        assertEquals(Component.translatable("pottery.primalpottery.shape.jug"), vessel(2.7).classify().type().shapeName());
        assertEquals(
                Component.translatable("item.primalpottery.raw_shape", Component.translatable("pottery.primalpottery.shape.jug")),
                RawPotteryItem.stack(vessel(2.7)).getHoverName()
        );
        assertEquals(
                Component.translatable("pottery.primalpottery.shape.bowl"),
                PotteryItem.stack(vessel(5)).getHoverName()
        );
    }

    @Test
    void rawTooltipSaysItNeedsFiringAndHowMuchItWillHold() {
        Pottery jar = new Pottery(List.of(
                new PotteryDisc(1, 4, 3),
                new PotteryDisc(2, 4, 2),
                new PotteryDisc(1, 4, 0)
        ));
        List<Component> plain = new ArrayList<>();
        RawPotteryItem.tooltip(RawPotteryItem.stack(jar), plain::add);
        assertEquals(List.of(
                Component.translatable("tooltip.primalpottery.needs_firing").withStyle(ChatFormatting.GRAY),
                Component.translatable("tooltip.primalpottery.can_be_dyed").withStyle(ChatFormatting.GRAY),
                Component.translatable("tooltip.primalpottery.will_contain", jar.classify().volume()).withStyle(ChatFormatting.DARK_GRAY)
        ), plain);

        List<Component> dyed = new ArrayList<>();
        RawPotteryItem.tooltip(RawPotteryItem.stack(jar, "red"), dyed::add);
        assertEquals(List.of(
                Component.translatable("tooltip.primalpottery.needs_firing").withStyle(ChatFormatting.GRAY),
                Component.translatable("tooltip.primalpottery.will_contain", jar.classify().volume()).withStyle(ChatFormatting.DARK_GRAY)
        ), dyed);

        List<Component> lump = new ArrayList<>();
        RawPotteryItem.tooltip(RawPotteryItem.stack(new Pottery(List.of(new PotteryDisc(1, 2, 0)))), lump::add);
        assertEquals(List.of(
                Component.translatable("tooltip.primalpottery.needs_firing").withStyle(ChatFormatting.GRAY),
                Component.translatable("tooltip.primalpottery.can_be_dyed").withStyle(ChatFormatting.GRAY)
        ), lump);
    }

    @Test
    void dyeRoundTripsAndClayTextureUsesTerracotta() {
        Pottery profile = new Pottery(List.of(new PotteryDisc(1, 2, 0)));
        ItemStack raw = RawPotteryItem.stack(profile, DyeColor.BLUE.getName());
        assertFalse(ModItems.RAW_POTTERY instanceof net.minecraft.world.item.BlockItem);
        assertEquals(1, new ItemStack(ModItems.RAW_POTTERY).getMaxStackSize());
        CompoundTag tag = PotteryItem.customData(raw);
        assertEquals("blue", Nbt.getString(tag, "dye"));
        assertEquals(profile.discs(), Pottery.load(Nbt.compound(tag, "pottery")).discs());
        assertFalse(tag.contains("fluid"));
        assertFalse(tag.contains("lid"));
        assertEquals(0x2F4BFF, raw.getItem().getName(raw).getStyle().getColor().getValue());
        assertEquals(16, DyeColor.values().length);
        for (DyeColor color : DyeColor.values()) {
            ItemStack tinted = RawPotteryItem.stack(profile, color.getName());
            assertEquals(DYE_COLORS.get(color.getName()).intValue(), tinted.getItem().getName(tinted).getStyle().getColor().getValue(), color.getName());
        }
        ItemStack plain = RawPotteryItem.stack(profile);
        assertNull(plain.getItem().getName(plain).getStyle().getColor());
        assertNull(RawPotteryItem.wheelProfile(raw));
        assertEquals(profile.discs(), RawPotteryItem.wheelProfile(plain).discs());

        ItemStack missing = new ItemStack(ModItems.RAW_POTTERY);
        assertNull(RawPotteryItem.wheelProfile(missing));
        CompoundTag broken = new CompoundTag();
        broken.put("pottery", new Pottery(List.of(new PotteryDisc(0, 2))).save());
        missing.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(broken));
        assertNull(RawPotteryItem.wheelProfile(missing));
        assertEquals(Pottery.display().discs(), Pottery.load(Nbt.compound(broken, "pottery")).discs());

        ItemStack fired = PotteryItem.stack(profile, VesselContents.EMPTY, false, "blue");
        ItemStack undyed = PotteryItem.stack(profile);
        assertEquals(PotteryItem.read(undyed).classify(), PotteryItem.read(fired).classify());
        assertEquals("blue", PotteryItem.dye(fired));
        assertNull(PotteryItem.dye(undyed));
        assertEquals(0x2F4BFF, fired.getItem().getName(fired).getStyle().getColor().getValue());
        assertNull(undyed.getItem().getName(undyed).getStyle().getColor());
        assertEquals(Ids.of("minecraft", "block/white_concrete"), Sprites.clayTexture("blue"));
        assertEquals(0x2F4BFF, Sprites.dyeTint("blue"));
        assertEquals(0xFFFFFF, Sprites.dyeTint(null));
        assertEquals(Ids.of("minecraft", "block/clay"), Sprites.rawTexture());
        assertEquals(Ids.of("minecraft", "block/red_terracotta"), Sprites.clayTexture(null));
        assertEquals(Ids.of("minecraft", "block/red_terracotta"), Sprites.clayTexture("blue_dye"));
    }

    @Test
    void paintAndFireKeepTheProfile() {
        Pottery profile = new Pottery(List.of(new PotteryDisc(1, 2, 0), new PotteryDisc(1, 3, 1)));
        ItemStack raw = RawPotteryItem.stack(profile);
        ItemStack dye = vanillaDye(DyeColor.BLUE);
        assertTrue(paintMatches(2, 2, pad(4, raw, dye)));
        assertTrue(paintMatches(3, 3, pad(9, raw, dye)));
        assertTrue(paintMatches(2, 1, pad(2, raw, dye)));
        assertFalse(paintMatches(3, 1, pad(3, raw, dye, new ItemStack(Items.STICK))));
        assertFalse(paintMatches(2, 1, pad(2, RawPotteryItem.stack(profile, "blue"), dye)));
        assertFalse(paintMatches(2, 1, pad(2, raw, outsideDye())));
        ItemStack painted = paintAssemble(2, 1, pad(2, raw, dye));
        assertEquals(profile.discs(), PotteryItem.read(painted).discs());
        assertEquals("blue", PotteryItem.dye(painted));
        for (DyeColor color : DyeColor.values()) {
            ItemStack one = paintAssemble(2, 1, pad(2, RawPotteryItem.stack(profile), vanillaDye(color)));
            assertEquals(color.getName(), PotteryItem.dye(one), color.getName());
            assertEquals(profile.discs(), PotteryItem.read(one).discs(), color.getName());
            assertTrue(paintMatches(2, 2, pad(4, RawPotteryItem.stack(profile), vanillaDye(color))), color.getName());
        }
        assertEquals(PaintPotteryRecipe.SERIALIZER, PaintPotteryRecipe.INSTANCE.getSerializer());
        //? if <26.1 {
        /*assertTrue(PaintPotteryRecipe.INSTANCE.canCraftInDimensions(2, 2));
        assertTrue(PaintPotteryRecipe.INSTANCE.canCraftInDimensions(3, 3));
        assertFalse(PaintPotteryRecipe.INSTANCE.canCraftInDimensions(2, 1));
        *///?}

        assertTrue(fireMatches(raw));
        assertTrue(fireMatches(painted));
        assertFalse(fireMatches(PotteryItem.stack(profile)));
        ItemStack fired = fireAssemble(painted);
        assertTrue(fired.is(ModItems.POTTERY));
        assertEquals(profile.discs(), PotteryItem.read(fired).discs());
        assertTrue(PotteryItem.contents(fired).isEmpty());
        assertFalse(PotteryItem.lid(fired));
        assertEquals("blue", PotteryItem.dye(fired));
        ItemStack clear = fireAssemble(raw);
        assertNull(PotteryItem.dye(clear));
        assertEquals(profile.discs(), PotteryItem.read(clear).discs());
        assertEquals(FirePotteryRecipe.SERIALIZER, FirePotteryRecipe.INSTANCE.getSerializer());
        //? if >=26.1 {
        assertEquals(net.minecraft.world.item.crafting.RecipeType.SMELTING, FirePotteryRecipe.INSTANCE.getType());
        assertEquals(net.minecraft.world.item.crafting.RecipeBookCategories.FURNACE_MISC, FirePotteryRecipe.INSTANCE.recipeBookCategory());
        assertEquals(200, FirePotteryRecipe.INSTANCE.cookingTime());
        assertEquals(0.1F, FirePotteryRecipe.INSTANCE.experience(), 0.0001F);
        //?} else {
        /*assertEquals(200, FirePotteryRecipe.INSTANCE.getCookingTime());
        assertEquals(0.1F, FirePotteryRecipe.INSTANCE.getExperience(), 0.0001F);
        *///?}
    }

    @Test
    void wheelFormsTheInstalledTemplate() {
        Pottery template = new Pottery(List.of(new PotteryDisc(1, 3, 1)));
        assertEquals(template, Pottery.formed(template));
        assertEquals(3, Pottery.formed(template).discs().getFirst().radius(), 0.0001);
        assertEquals(1, Pottery.formed(template).discs().getFirst().innerRadius(), 0.0001);
    }

    @Test
    void lidCopiesTheRimAndLeavesTheProfile() {
        Pottery pottery = new Pottery(List.of(
                new PotteryDisc(1, 0, 0),
                new PotteryDisc(2, 4, 2),
                new PotteryDisc(3, 5, 3),
                new PotteryDisc(1, 5, 0)
        ));
        int volume = pottery.classify().volume();
        PotteryDisc lid = pottery.lidDisc();
        assertEquals(Pottery.PIXEL, lid.height(), 0.0001);
        assertEquals(2 + Pottery.PIXEL / 2.0, lid.radius(), 0.0001);
        assertEquals(0, lid.innerRadius(), 0.0001);
        assertEquals(pottery.discs(), pottery.drawDiscs(true));
        assertEquals(pottery.discs(), pottery.drawDiscs(false));
        assertEquals(volume, pottery.classify().volume());
        Pottery.Cover cover = pottery.cover();
        assertEquals(1 + 2 + 3 + 1, cover.top(), 0.0001);
        assertEquals(2, cover.layer(), 0.0001);
        assertEquals(2, cover.hole(), 0.0001);

        Pottery lump = new Pottery(List.of(new PotteryDisc(2, 4, 0), new PotteryDisc(2, 4, 0)));
        assertNull(lump.lidDisc());
        assertEquals(lump.discs(), lump.drawDiscs(true));
        assertNull(new Pottery(List.of(new PotteryDisc(2, 0.4, 0))).lidDisc());
    }

    @Test
    void lidKeyIsOmittedWhenOpen() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("lid", true);
        assertTrue(Nbt.getBoolean(tag, "lid"));
        assertTrue(tag.contains("lid"));
        tag.remove("lid");
        assertFalse(Nbt.getBoolean(tag, "lid"));
        assertFalse(tag.contains("lid"));
    }

    @Test
    void tooltipColorTintsOnlyWater() {
        assertEquals(0x3F76E4, VesselContents.tooltipColor(Fluids.WATER));
        assertEquals(-1, VesselContents.tooltipColor(Fluids.LAVA));
        assertEquals(-1, VesselContents.tooltipColor(Fluids.EMPTY));
        assertEquals(0x3F76E4, PotteryItem.fluidName(Fluids.WATER).getStyle().getColor().getValue());
        assertEquals(Blocks.WATER.getName().getString(), PotteryItem.fluidName(Fluids.WATER).getString());
        assertNull(PotteryItem.fluidName(Fluids.LAVA).getStyle().getColor());
        assertEquals(Blocks.LAVA.getName().getString(), PotteryItem.fluidName(Fluids.LAVA).getString());
    }

    @Test
    void profilesRejectInvalidDimensionsAndKeepOlderZeroWidthWalls() {
        for (PotteryDisc invalid : List.of(new PotteryDisc(0, 2), new PotteryDisc(Double.NaN, 2),
                new PotteryDisc(1, Double.POSITIVE_INFINITY), new PotteryDisc(1, -1), new PotteryDisc(1, 6),
                new PotteryDisc(1, 2, Double.NaN), new PotteryDisc(1, 2, -1))) {
            Pottery bad = new Pottery(List.of(invalid));
            assertFalse(Pottery.validProfile(bad.discs()));
            assertEquals(Pottery.display().discs(), Pottery.load(bad.save()).discs());
        }
        assertFalse(Pottery.validProfile(List.of(new PotteryDisc(7, 2), new PotteryDisc(7, 2))));
        assertFalse(Pottery.validProfile(java.util.Collections.nCopies(65, new PotteryDisc(0.1, 2))));
        Pottery crossed = new Pottery(List.of(new PotteryDisc(1, 2, 3), new PotteryDisc(1, 2)));
        assertFalse(Pottery.validProfile(crossed.discs()));
        assertEquals(List.of(new PotteryDisc(1, 2, 2), new PotteryDisc(1, 2)), Pottery.load(crossed.save()).discs());
    }

    @Test
    void nbtRoundTripKeepsTheShape() {
        Pottery pottery = new Pottery(List.of(
                new PotteryDisc(2, 4, 1),
                new PotteryDisc(2, 4, 2),
                new PotteryDisc(1, 4, 0)
        ));
        Pottery loaded = Pottery.load(pottery.save());
        assertEquals(PotteryType.JUG, loaded.classify().type());
        assertEquals(3, loaded.discs().size());
        assertEquals(4, loaded.discs().getFirst().radius(), 0.0001);
        assertEquals(1, loaded.discs().getFirst().innerRadius(), 0.0001);
        assertEquals(2, loaded.discs().get(1).innerRadius(), 0.0001);
        assertEquals(1, loaded.discs().get(2).height(), 0.0001);
    }

    @Test
    void emissionFollowsTheFluidSourceBlock() {
        assertEquals(15, Blocks.LAVA.defaultBlockState().getLightEmission());
        assertEquals(0, Blocks.WATER.defaultBlockState().getLightEmission());
        assertEquals(15, VesselContents.of(Fluids.LAVA, 1).emission());
        assertEquals(15, VesselContents.of(Fluids.FLOWING_LAVA, 1).emission());
        assertEquals(0, VesselContents.of(Fluids.WATER, 1).emission());
        assertEquals(0, VesselContents.of(Fluids.FLOWING_WATER, 1).emission());
        assertEquals(0, VesselContents.EMPTY.emission());
    }

    @Test
    void chooseFillsFromASource() {
        assertEquals(VesselContents.Choice.FILL, VesselContents.choose(looking("minecraft:water")));
    }

    @Test
    void chooseFillsFromFlowingWaterAndLava() {
        assertEquals(VesselContents.Choice.FILL, VesselContents.choose(looking("minecraft:water")));
        assertEquals(Fluids.WATER, VesselContents.source(Fluids.FLOWING_WATER));
        assertEquals(VesselContents.Choice.FILL, VesselContents.choose(looking("minecraft:lava")));
        assertEquals(Fluids.LAVA, VesselContents.source(Fluids.FLOWING_LAVA));
        assertTrue(VesselContents.ownsFluidBlock(flowing(Blocks.WATER), flowing(Blocks.WATER).getFluidState()));
        assertTrue(VesselContents.ownsFluidBlock(flowing(Blocks.LAVA), flowing(Blocks.LAVA).getFluidState()));
    }

    @Test
    void chooseFillsFromAnotherFluid() {
        assertEquals(VesselContents.Choice.FILL, VesselContents.choose(looking("example:oil")));
    }

    @Test
    void waterloggedAndBubbleColumnDoNotFill() {
        BlockState slab = Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED, true);
        BlockState bubble = Blocks.BUBBLE_COLUMN.defaultBlockState();
        assertFalse(VesselContents.ownsFluidBlock(slab, slab.getFluidState()));
        assertFalse(bubble.getFluidState().isEmpty());
        assertFalse(VesselContents.ownsFluidBlock(bubble, bubble.getFluidState()));
        assertEquals(VesselContents.Choice.REFUSE, VesselContents.choose(blocked()));
        VesselContents.Facts logged = blocked();
        assertEquals(VesselContents.Choice.REFUSE, VesselContents.choose(new VesselContents.Facts(
                logged.player(), logged.vessel(), false, logged.volume(), logged.povFluid(), logged.sourceId(),
                logged.interact(), logged.canPlace(), logged.clickedFluid(), false, true, true
        )));
    }

    @Test
    void nullPlayerDoesNotFill() {
        VesselContents.Facts facts = looking("minecraft:water");
        assertEquals(VesselContents.Choice.REFUSE, VesselContents.choose(new VesselContents.Facts(
                false, facts.vessel(), facts.empty(), facts.volume(), facts.povFluid(), facts.sourceId(),
                false, facts.canPlace(), facts.clickedFluid(), facts.clickedSource(), false, false
        )));
    }

    @Test
    void failedInteractCheckDoesNotFill() {
        VesselContents.Facts facts = looking("minecraft:water");
        assertEquals(VesselContents.Choice.REFUSE, VesselContents.choose(new VesselContents.Facts(
                facts.player(), facts.vessel(), facts.empty(), facts.volume(), facts.povFluid(), facts.sourceId(),
                false, facts.canPlace(), facts.clickedFluid(), facts.clickedSource(), facts.sneaking(), facts.holding()
        )));
    }

    @Test
    void topFaceLumpRefusesFluid() {
        assertEquals(VesselContents.Choice.REFUSE, VesselContents.choose(new VesselContents.Facts(
                true, false, true, 0, true, "minecraft:water", true, true, true, true, false, false
        )));
    }

    @Test
    void nonSneakOnFluidRefuses() {
        assertEquals(VesselContents.Choice.REFUSE, VesselContents.choose(new VesselContents.Facts(
                true, true, false, 54, false, "minecraft:water", true, true, true, true, false, true
        )));
    }

    @Test
    void sneakOnAWaterSourcePlaces() {
        assertEquals(VesselContents.Choice.PLACE, VesselContents.choose(new VesselContents.Facts(
                true, true, false, 54, false, "minecraft:water", true, true, true, true, true, true
        )));
    }

    @Test
    void sneakOnFlowingFluidRefuses() {
        assertEquals(VesselContents.Choice.REFUSE, VesselContents.choose(new VesselContents.Facts(
                true, true, false, 54, false, "minecraft:water", true, true, true, false, true, true
        )));
    }

    @Test
    void pourMovesWhatFits() {
        VesselContents twenty = VesselContents.of(Fluids.WATER, 20);
        VesselContents hundred = VesselContents.of(Fluids.WATER, 100);
        assertEquals(20, VesselContents.poured(twenty, VesselContents.EMPTY, 54));
        assertEquals(44, VesselContents.poured(hundred, VesselContents.of(Fluids.WATER, 10), 54));
        assertEquals(0, VesselContents.poured(twenty, VesselContents.of(Fluids.WATER, 54), 54));
        assertEquals(0, VesselContents.poured(twenty, VesselContents.of(Fluids.LAVA, 10), 54));
        assertEquals(0, VesselContents.poured(VesselContents.EMPTY, VesselContents.EMPTY, 54));
        assertEquals(0, VesselContents.poured(twenty, VesselContents.EMPTY, 0));
    }

    @Test
    void contentsRoundTrip() {
        CompoundTag tag = new CompoundTag();
        VesselContents.of(Fluids.WATER, 54).write(tag);
        VesselContents loaded = VesselContents.read(tag);
        assertEquals(Fluids.WATER, loaded.fluid());
        assertEquals(54, loaded.amount());
        assertEquals("minecraft:water", Nbt.getString(tag, "fluid"));

        CompoundTag flowing = new CompoundTag();
        flowing.putString("fluid", BuiltInRegistries.FLUID.getKey(Fluids.FLOWING_WATER).toString());
        flowing.putInt("amount", 12);
        assertEquals(Fluids.WATER, VesselContents.read(flowing).fluid());
        assertEquals(12, VesselContents.read(flowing).amount());

        CompoundTag flowingLava = new CompoundTag();
        flowingLava.putString("fluid", BuiltInRegistries.FLUID.getKey(Fluids.FLOWING_LAVA).toString());
        flowingLava.putInt("amount", 3);
        assertEquals(Fluids.LAVA, VesselContents.read(flowingLava).fluid());

        CompoundTag empty = new CompoundTag();
        empty.putString("fluid", "minecraft:lava");
        empty.putInt("amount", 1);
        VesselContents.EMPTY.write(empty);
        assertEquals("", Nbt.getString(empty, "fluid"));
        assertEquals(0, Nbt.getInt(empty, "amount"));

        assertTrue(VesselContents.read(new CompoundTag()).isEmpty());
        CompoundTag blank = new CompoundTag();
        blank.putString("fluid", " ");
        blank.putInt("amount", 4);
        assertTrue(VesselContents.read(blank).isEmpty());
        CompoundTag unknown = new CompoundTag();
        unknown.putString("fluid", "example:oil");
        unknown.putInt("amount", 4);
        assertTrue(VesselContents.read(unknown).isEmpty());
        CompoundTag none = new CompoundTag();
        none.putString("fluid", "minecraft:water");
        none.putInt("amount", 0);
        assertTrue(VesselContents.read(none).isEmpty());
    }

    private static VesselContents.Facts looking(String sourceId) {
        return new VesselContents.Facts(true, true, true, 54, true, sourceId, true, true, true, true, false, false);
    }

    private static VesselContents.Facts blocked() {
        return new VesselContents.Facts(true, true, true, 54, false, "", true, true, true, false, false, false);
    }

    private static BlockState flowing(net.minecraft.world.level.block.Block block) {
        return block.defaultBlockState().setValue(LiquidBlock.LEVEL, 1);
    }

    private static List<ItemStack> pad(int size, ItemStack... stacks) {
        java.util.ArrayList<ItemStack> items = new java.util.ArrayList<>();
        for (ItemStack stack : stacks) {
            items.add(stack);
        }
        while (items.size() < size) {
            items.add(ItemStack.EMPTY);
        }
        return items;
    }

    private static ItemStack vanillaDye(DyeColor color) {
        //? if >=26.1 {
        ItemStack stack = new ItemStack(Items.DYE.pick(color));
        // Unit tests never run the server component bake, so a dye item can have an empty prototype.
        if (stack.get(net.minecraft.core.component.DataComponents.DYE) == null) {
            stack.set(net.minecraft.core.component.DataComponents.DYE, color);
        }
        return stack;
        //?} else
        //return new ItemStack(net.minecraft.world.item.DyeItem.byColor(color));
    }

    private static ItemStack outsideDye() {
        ItemStack stick = new ItemStack(Items.STICK);
        //? if >=26.1
        stick.set(net.minecraft.core.component.DataComponents.DYE, DyeColor.BLUE);
        return stick;
    }

    private static boolean paintMatches(int width, int height, List<ItemStack> items) {
        //? if >=1.21 {
        return PaintPotteryRecipe.INSTANCE.matches(net.minecraft.world.item.crafting.CraftingInput.of(width, height, items), null);
        //?} else {
        /*return PaintPotteryRecipe.INSTANCE.matches(crafting(width, height, items), null);
        *///?}
    }

    private static ItemStack paintAssemble(int width, int height, List<ItemStack> items) {
        //? if >=26.1 {
        return PaintPotteryRecipe.INSTANCE.assemble(net.minecraft.world.item.crafting.CraftingInput.of(width, height, items));
        //?} elif >=1.21 {
        /*return PaintPotteryRecipe.INSTANCE.assemble(net.minecraft.world.item.crafting.CraftingInput.of(width, height, items), null);
        *///?} else {
        /*return PaintPotteryRecipe.INSTANCE.assemble(crafting(width, height, items), null);
        *///?}
    }

    private static boolean fireMatches(ItemStack stack) {
        //? if >=1.21 {
        return FirePotteryRecipe.INSTANCE.matches(new net.minecraft.world.item.crafting.SingleRecipeInput(stack), null);
        //?} else {
        /*return FirePotteryRecipe.INSTANCE.matches(new net.minecraft.world.SimpleContainer(stack), null);
        *///?}
    }

    private static ItemStack fireAssemble(ItemStack stack) {
        //? if >=26.1 {
        return FirePotteryRecipe.INSTANCE.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(stack));
        //?} elif >=1.21 {
        /*return FirePotteryRecipe.INSTANCE.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(stack), null);
        *///?} else {
        /*return FirePotteryRecipe.INSTANCE.assemble(new net.minecraft.world.SimpleContainer(stack), null);
        *///?}
    }

    //? if <1.21 {
    /*private static net.minecraft.world.inventory.CraftingContainer crafting(int width, int height, List<ItemStack> items) {
        net.minecraft.core.NonNullList<ItemStack> slots = net.minecraft.core.NonNullList.withSize(width * height, ItemStack.EMPTY);
        for (int index = 0; index < items.size() && index < slots.size(); index++) {
            slots.set(index, items.get(index));
        }
        return new net.minecraft.world.inventory.TransientCraftingContainer(null, width, height, slots);
    }
    *///?}

    private static Pottery vessel(double opening) {
        return new Pottery(List.of(
                new PotteryDisc(1, opening + 0.5, opening),
                new PotteryDisc(2, 5.5, 3),
                new PotteryDisc(1, 5)
        ));
    }
}
