package net.modcrafters.minecraft.primalpottery.pottery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.modcrafters.minecraft.primalpottery.item.PotteryItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PotteryTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
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

    private static Pottery vessel(double opening) {
        return new Pottery(List.of(
                new PotteryDisc(1, opening + 0.5, opening),
                new PotteryDisc(2, 5.5, 3),
                new PotteryDisc(1, 5)
        ));
    }
}
