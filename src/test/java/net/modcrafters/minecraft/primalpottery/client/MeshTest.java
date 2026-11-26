package net.modcrafters.minecraft.primalpottery.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryDisc;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class MeshTest {
    @Test
    void stackedDiscsShareOneShell() {
        List<PotteryDisc> discs = new ArrayList<>();
        for (int index = 0; index < 24; index++) {
            discs.add(new PotteryDisc(0.5, 5.5, 0));
        }
        Mesh mesh = new Mesh();
        mesh.vessel(16, 28, 16, discs, Mesh.Texture.CLAY);
        long interior = mesh.quads().stream()
                .filter(quad -> quad.ny != 0 && quad.y[0] > 28.01F && quad.y[0] < 39.99F)
                .count();
        assertEquals(0, interior);
        assertEquals(8 * 24 + 8, mesh.quads().size());
    }

    @Test
    void potWallsFaceOutward() {
        Mesh mesh = new Mesh();
        mesh.vessel(16, 0, 16, List.of(new PotteryDisc(4, 5, 3)), Mesh.Texture.CLAY);
        for (Mesh.Quad quad : mesh.quads()) {
            float ax = quad.x[1] - quad.x[0];
            float ay = quad.y[1] - quad.y[0];
            float az = quad.z[1] - quad.z[0];
            float bx = quad.x[2] - quad.x[1];
            float by = quad.y[2] - quad.y[1];
            float bz = quad.z[2] - quad.z[1];
            float nx = ay * bz - az * by;
            float ny = az * bx - ax * bz;
            float nz = ax * by - ay * bx;
            assertTrue(nx * quad.nx + ny * quad.ny + nz * quad.nz > 0);
        }
    }

    @Test
    void zeroWidthWallsDoNotProduceFaces() {
        Mesh mesh = new Mesh();
        mesh.vessel(0, 0, 0, List.of(
                new PotteryDisc(1, 3, 3),
                new PotteryDisc(1, 3, 4),
                new PotteryDisc(1, 3)
        ), Mesh.Texture.CLAY);

        assertEquals(16, mesh.quads().size());
        for (Mesh.Quad quad : mesh.quads()) {
            for (float y : quad.y) {
                assertTrue(y <= 1);
            }
        }
    }

    @Test
    void screenTopIsTheTopOfThePot() {
        Mesh mesh = new Mesh();
        mesh.vessel(0, 0, 0, List.of(new PotteryDisc(2, 6), new PotteryDisc(2, 2)), Mesh.Texture.CLAY);
        assertEquals(6.0, reach(mesh, 2, 4), 0.05);
        assertEquals(2.0, reach(mesh, 0, 2), 0.05);
    }

    @Test
    void spinningPreviewStaysUprightInsideTheRecess() {
        int width = 51;
        int height = 70;
        PotteryPreview.Model model = PotteryPreview.model(
                List.of(new PotteryDisc(1, 2), new PotteryDisc(30, 5)), width, height
        );
        Vector3f position = new Vector3f();
        for (int spin = 0; spin < 360; spin += 15) {
            PoseStack pose = new PoseStack();
            pose.translate(width / 2.0F, height / 2.0F, 0);
            //? if >=26.1 {
            pose.scale(model.scale(), model.scale(), -model.scale());
            //?} else
            //pose.scale(model.scale(), -model.scale(), model.scale());
            PotteryPreview.orient(pose, model, spin);
            var matrix = pose.last().pose();
            float top = matrix.transformPosition(0, 31, 0, position).y;
            float bottom = matrix.transformPosition(0, 0, 0, position).y;
            assertTrue(top < bottom);
            for (Mesh.Quad quad : model.mesh().quads()) {
                for (int vertex = 0; vertex < 4; vertex++) {
                    matrix.transformPosition(quad.x[vertex], quad.y[vertex], quad.z[vertex], position);
                    assertTrue(position.x >= 2 && position.x <= width - 2);
                    assertTrue(position.y >= 2 && position.y <= height - 2);
                }
            }
        }
    }

    @Test
    void boardPushesIntoTheNarrowerClay() {
        Mesh mesh = new Mesh();
        mesh.board(30, 24, 2, 2, List.of(new PotteryDisc(2, 4), new PotteryDisc(2, 6)), Mesh.Texture.PLANKS);
        assertEquals(26.0, westEdge(mesh, 26, 28), 0.01);
        assertEquals(28.0, westEdge(mesh, 24, 26), 0.01);
    }

    private static float maxY(Mesh mesh) {
        float highest = Float.NEGATIVE_INFINITY;
        for (Mesh.Quad quad : mesh.quads()) {
            for (int vertex = 0; vertex < 4; vertex++) {
                highest = Math.max(highest, quad.y[vertex]);
            }
        }
        return highest;
    }

    private static float reach(Mesh mesh, float low, float high) {
        float reach = 0;
        for (Mesh.Quad quad : mesh.quads()) {
            float midY = (quad.y[0] + quad.y[1] + quad.y[2] + quad.y[3]) / 4.0F;
            if (midY <= low || midY >= high) {
                continue;
            }
            for (int vertex = 0; vertex < 4; vertex++) {
                float x = quad.x[vertex];
                float z = quad.z[vertex];
                reach = Math.max(reach, (float) Math.hypot(x, z));
            }
        }
        return reach;
    }

    private static float westEdge(Mesh mesh, float low, float high) {
        float edge = Float.POSITIVE_INFINITY;
        for (Mesh.Quad quad : mesh.quads()) {
            if (quad.nx >= 0) {
                continue;
            }
            float midY = (quad.y[0] + quad.y[1] + quad.y[2] + quad.y[3]) / 4.0F;
            if (midY <= low || midY >= high) {
                continue;
            }
            for (int vertex = 0; vertex < 4; vertex++) {
                edge = Math.min(edge, quad.x[vertex]);
            }
        }
        return edge;
    }

    @Test
    void lidSitsOnTheRimAndCavityStaysOpen() {
        Pottery pottery = new Pottery(List.of(
                new PotteryDisc(2, 4, 2),
                new PotteryDisc(4, 6, 5),
                new PotteryDisc(1, 6, 0)
        ));
        Mesh open = new Mesh();
        open.vessel(0, 0, 0, pottery.discs(), Mesh.Texture.CLAY);
        Mesh lidded = new Mesh();
        lidded.vessel(0, 0, 0, pottery.discs(), Mesh.Texture.CLAY);
        lidded.cover(0, 0, 0, pottery.cover(), Mesh.Texture.CLAY);
        float rimTop = maxY(open);
        float layer = (float) pottery.cover().layer();
        float lidBottom = Float.POSITIVE_INFINITY;
        boolean above = false;
        for (Mesh.Quad quad : lidded.quads()) {
            float min = quad.y[0];
            float max = quad.y[0];
            for (int vertex = 1; vertex < 4; vertex++) {
                min = Math.min(min, quad.y[vertex]);
                max = Math.max(max, quad.y[vertex]);
            }
            if (max > rimTop + 0.01F) {
                above = true;
                lidBottom = Math.min(lidBottom, min);
            }
        }
        assertTrue(above);
        assertEquals(rimTop + layer, lidBottom, 0.05);
        assertEquals(rimTop + layer + (float) Pottery.PIXEL * 2, maxY(lidded), 0.05);
        float plateTop = rimTop + layer + (float) Pottery.PIXEL;
        assertEquals((float) (pottery.cover().hole() + Pottery.PIXEL / 2.0), reach(lidded, rimTop + layer, plateTop), 0.05);
        assertTrue(reach(lidded, plateTop, maxY(lidded)) < reach(lidded, rimTop + layer, plateTop));

        Mesh cavity = new Mesh();
        cavity.cavity(0, 0, 0, pottery.discs());
        assertFalse(cavity.quads().isEmpty());
        assertEquals(pottery.classify().volume(), new Pottery(pottery.discs()).classify().volume());
    }

    @Test
    void fluidSurfaceFollowsEachOpenDisc() {
        List<PotteryDisc> discs = List.of(
                new PotteryDisc(2, 4, 2),
                new PotteryDisc(4, 6, 5),
                new PotteryDisc(1, 6, 0)
        );
        Mesh mesh = new Mesh();
        mesh.cavity(0, 0, 0, discs);
        assertFalse(mesh.quads().isEmpty());
        float rimTop = 7;
        float belly = 0;
        float rim = 0;
        float highest = 0;
        for (Mesh.Quad quad : mesh.quads()) {
            for (int vertex = 0; vertex < 4; vertex++) {
                float y = quad.y[vertex];
                float radius = (float) Math.hypot(quad.x[vertex], quad.z[vertex]);
                assertTrue(y < rimTop);
                assertTrue(y >= 1);
                highest = Math.max(highest, y);
                if (y > 5.01F) {
                    assertTrue(radius < 2);
                    rim = Math.max(rim, radius);
                } else if (y > 1.01F) {
                    assertTrue(radius < 5);
                    belly = Math.max(belly, radius);
                }
            }
        }
        assertEquals(5 - Mesh.FLUID_INSET, belly, 0.02);
        assertEquals(2 - Mesh.FLUID_INSET, rim, 0.02);
        assertEquals(rimTop - Mesh.FLUID_INSET, highest, 0.02);

        Mesh skipped = new Mesh();
        skipped.cavity(0, 0, 0, List.of(new PotteryDisc(2, 4, Mesh.FLUID_INSET), new PotteryDisc(1, 4, 0)));
        assertEquals(0, skipped.quads().size());
    }

    @Test
    void fittedClayStaysOnOneSprite() {
        List<PotteryDisc> discs = List.of(new PotteryDisc(12, 5.5, 0));
        Mesh mesh = new Mesh();
        mesh.vessel(16, 28, 16, discs, Mesh.Texture.CLAY);
        mesh.fitSprite();
        for (Mesh.Quad quad : mesh.quads()) {
            for (int vertex = 0; vertex < 4; vertex++) {
                assertTrue(quad.u[vertex] >= 0 && quad.u[vertex] <= 16);
                assertTrue(quad.v[vertex] >= 0 && quad.v[vertex] <= 16);
            }
        }
    }
}
