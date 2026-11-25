package net.modcrafters.minecraft.primalpottery.pottery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.List;

class PotteryTest {
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

    private static Pottery vessel(double opening) {
        return new Pottery(List.of(
                new PotteryDisc(1, opening + 0.5, opening),
                new PotteryDisc(2, 5.5, 3),
                new PotteryDisc(1, 5)
        ));
    }
}
