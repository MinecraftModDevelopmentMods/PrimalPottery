package net.modcrafters.minecraft.primalpottery.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.Direction;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryDisc;

public final class Mesh {
    static final double FLUID_INSET = 0.05;

    public enum Texture {
        PLANKS,
        STONE,
        LOG,
        CLAY
    }

    public static final class Quad {
        final float[] x = new float[4];
        final float[] y = new float[4];
        final float[] z = new float[4];
        final float[] u = new float[4];
        final float[] v = new float[4];
        final float nx;
        final float ny;
        final float nz;
        final Texture texture;

        Quad(float nx, float ny, float nz, Texture texture) {
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.texture = texture;
        }
    }

    private final List<Quad> quads = new ArrayList<>();

    public List<Quad> quads() {
        return this.quads;
    }

    public void box(double x0, double y0, double z0, double x1, double y1, double z1, Texture texture) {
        this.face(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, Direction.UP, texture);
        this.face(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, Direction.DOWN, texture);
        this.face(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, Direction.NORTH, texture);
        this.face(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, Direction.SOUTH, texture);
        this.face(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, Direction.WEST, texture);
        this.face(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, Direction.EAST, texture);
    }

    public void boxRotated(double x0, double y0, double z0, double x1, double y1, double z1, double radians, Texture texture) {
        Mesh local = new Mesh();
        local.box(x0, y0, z0, x1, y1, z1, texture);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        for (Quad quad : local.quads) {
            Quad rotated = new Quad(quad.nx, quad.ny, quad.nz, quad.texture);
            for (int vertex = 0; vertex < 4; vertex++) {
                double dx = quad.x[vertex] - 16;
                double dz = quad.z[vertex] - 16;
                rotated.x[vertex] = (float) (16 + dx * cos - dz * sin);
                rotated.y[vertex] = quad.y[vertex];
                rotated.z[vertex] = (float) (16 + dx * sin + dz * cos);
                rotated.u[vertex] = quad.u[vertex];
                rotated.v[vertex] = quad.v[vertex];
            }
            double nx = quad.nx * cos - quad.nz * sin;
            double nz = quad.nx * sin + quad.nz * cos;
            Quad spun = new Quad((float) nx, quad.ny, (float) nz, quad.texture);
            System.arraycopy(rotated.x, 0, spun.x, 0, 4);
            System.arraycopy(rotated.y, 0, spun.y, 0, 4);
            System.arraycopy(rotated.z, 0, spun.z, 0, 4);
            System.arraycopy(rotated.u, 0, spun.u, 0, 4);
            System.arraycopy(rotated.v, 0, spun.v, 0, 4);
            this.quads.add(spun);
        }
    }

    public void add(Mesh other) {
        this.quads.addAll(other.quads);
    }

    public void vessel(double centerX, double base, double centerZ, List<PotteryDisc> discs, Texture texture) {
        List<PotteryDisc> rising = fromTheBottom(discs);
        double y = base;
        int count = rising.size();
        for (int index = 0; index < count; index++) {
            PotteryDisc disc = rising.get(index);
            double height = disc.height();
            double radius = clayRadius(disc);
            double inner = Math.min(Math.max(0, disc.innerRadius()), radius);
            if (radius > 0 && height > 0) {
                double start = Math.PI / 8.0;
                double step = Math.PI / 4.0;
                for (int side = 0; side < 8; side++) {
                    double a0 = start + side * step;
                    double a1 = a0 + step;
                    this.wall(centerX, centerZ, y, height, radius, a0, a1, texture, true);
                    if (inner > 0) {
                        this.wall(centerX, centerZ, y, height, inner, a0, a1, texture, false);
                    }
                }
            }
            if (index == 0) {
                this.floor(centerX, centerZ, y, radius, inner, texture, false);
            } else {
                PotteryDisc below = rising.get(index - 1);
                double belowRadius = clayRadius(below);
                double belowInner = Math.min(Math.max(0, below.innerRadius()), belowRadius);
                this.ledge(centerX, centerZ, y, belowRadius, radius, texture, false);
                this.ledge(centerX, centerZ, y, belowInner, inner, texture, true);
            }
            y += height;
        }
        if (count > 0) {
            PotteryDisc top = rising.get(count - 1);
            double radius = clayRadius(top);
            double inner = Math.min(Math.max(0, top.innerRadius()), radius);
            this.floor(centerX, centerZ, y, radius, inner, texture, true);
        }
    }

    /**
     * A lid one layer above the pot, one pixel wider than the hole, with a small handle in the middle.
     */
    public void cover(double centerX, double base, double centerZ, Pottery.Cover cover, Texture texture) {
        double y = base + cover.top() + cover.layer();
        double radius = cover.hole() + Pottery.PIXEL / 2.0;
        double thickness = Pottery.PIXEL;
        this.cylinder(centerX, y, centerZ, thickness, radius, 0, texture);
        double handle = Math.min(thickness, radius / 2.0);
        this.cylinder(centerX, y + thickness, centerZ, thickness, handle, 0, texture);
    }

    private static double clayRadius(PotteryDisc disc) {
        return disc.radius() > Math.max(0, disc.innerRadius()) ? disc.radius() : 0;
    }

    public void board(double backX, double base, double z0, double depth, List<PotteryDisc> discs, Texture texture) {
        List<PotteryDisc> rising = fromTheBottom(discs);
        double reach = 0;
        double top = base;
        for (PotteryDisc disc : rising) {
            reach = Math.max(reach, Math.max(0, disc.radius()));
            top += Math.max(0, disc.height());
        }
        if (top <= base || depth <= 0) {
            return;
        }
        double heel = 2;
        double z1 = z0 + depth;
        this.face(backX, base, z1, backX, base, z0, backX, top, z0, backX, top, z1, Direction.EAST, texture);
        double y = base;
        int count = rising.size();
        for (int index = 0; index < count; index++) {
            PotteryDisc disc = rising.get(index);
            double height = Math.max(0, disc.height());
            double edge = cut(backX, heel, reach, Math.max(0, disc.radius()));
            double y1 = y + height;
            if (height > 0) {
                this.face(edge, y, z1, backX, y, z1, backX, y1, z1, edge, y1, z1, Direction.SOUTH, texture);
                this.face(backX, y, z0, edge, y, z0, edge, y1, z0, backX, y1, z0, Direction.NORTH, texture);
                this.face(edge, y, z0, edge, y, z1, edge, y1, z1, edge, y1, z0, Direction.WEST, texture);
            }
            if (index == 0) {
                this.face(edge, y, z0, backX, y, z0, backX, y, z1, edge, y, z1, Direction.DOWN, texture);
            } else {
                double belowEdge = cut(backX, heel, reach, Math.max(0, rising.get(index - 1).radius()));
                double west = Math.min(edge, belowEdge);
                double east = Math.max(edge, belowEdge);
                if (east - west > 1.0e-4) {
                    if (belowEdge < edge) {
                        this.face(west, y, z1, east, y, z1, east, y, z0, west, y, z0, Direction.UP, texture);
                    } else {
                        this.face(west, y, z0, east, y, z0, east, y, z1, west, y, z1, Direction.DOWN, texture);
                    }
                }
            }
            y = y1;
        }
        if (count > 0) {
            double edge = cut(backX, heel, reach, Math.max(0, rising.get(count - 1).radius()));
            this.face(edge, y, z1, backX, y, z1, backX, y, z0, edge, y, z0, Direction.UP, texture);
        }
    }

    private static List<PotteryDisc> fromTheBottom(List<PotteryDisc> discs) {
        List<PotteryDisc> rising = new ArrayList<>(discs);
        Collections.reverse(rising);
        return rising;
    }

    private static double cut(double backX, double heel, double reach, double radius) {
        return backX - heel - (reach - radius);
    }

    /**
     * The fluid body. Each open disc is an octagon at its inner radius, inset so it stays inside the clay.
     * Fill from the floor by slice volume. A full vessel's surface sits just below its rim.
     */
    public void cavity(double centerX, double base, double centerZ, List<PotteryDisc> discs, int amount) {
        int rim = openRim(discs);
        if (rim < 0 || amount <= 0) {
            return;
        }
        int end = rim;
        double capacity = 0;
        while (end < discs.size() && discs.get(end).innerRadius() > 0) {
            PotteryDisc disc = discs.get(end++);
            capacity += Pottery.cavityArea(disc.innerRadius()) * disc.height();
        }
        // Stored capacity is rounded to whole millilitres.
        double remaining = amount >= Math.round(capacity) ? capacity : amount;
        double y = base;
        for (int index = discs.size() - 1; index >= end; index--) {
            y += Math.max(0, discs.get(index).height());
        }
        for (int index = end - 1; index >= rim; index--) {
            PotteryDisc disc = discs.get(index);
            double height = disc.height();
            double radius = disc.innerRadius() - FLUID_INSET;
            double area = Pottery.cavityArea(disc.innerRadius());
            double sliceVolume = area * height;
            boolean top = index == rim || remaining <= sliceVolume;
            double drawHeight = Math.min(height, remaining / area);
            if (index == rim) {
                drawHeight = Math.min(drawHeight, height - Math.min(FLUID_INSET, height * 0.5));
            }
            if (radius > 0 && drawHeight > 0) {
                this.ringWalls(centerX, centerZ, y, drawHeight, radius, Texture.CLAY);
                if (top) {
                    this.cap(centerX, centerZ, y + drawHeight, radius, Texture.CLAY, true);
                }
            }
            if (top) {
                break;
            }
            remaining -= sliceVolume;
            y += height;
        }
    }

    private static int openRim(List<PotteryDisc> discs) {
        int rim = -1;
        for (int index = 0; index < discs.size(); index++) {
            PotteryDisc disc = discs.get(index);
            if (!Double.isFinite(disc.height()) || !Double.isFinite(disc.radius()) || !Double.isFinite(disc.innerRadius())
                    || disc.height() <= 0 || disc.radius() < 0 || disc.innerRadius() < 0) {
                return -1;
            }
            if (disc.radius() == 0 && disc.innerRadius() == 0) {
                continue;
            }
            if (disc.radius() - disc.innerRadius() < 0.5) {
                return -1;
            }
            if (rim < 0) {
                rim = index;
            }
        }
        if (rim < 0 || discs.get(rim).innerRadius() == 0) {
            return -1;
        }
        return rim;
    }

    private void ringWalls(double centerX, double centerZ, double bottom, double height, double radius, Texture texture) {
        double start = Math.PI / 8.0;
        double step = Math.PI / 4.0;
        for (int side = 0; side < 8; side++) {
            double a0 = start + side * step;
            this.wall(centerX, centerZ, bottom, height, radius, a0, a0 + step, texture, true);
        }
    }

    public void fitSprite() {
        if (this.quads.isEmpty()) {
            return;
        }
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (Quad quad : this.quads) {
            for (int vertex = 0; vertex < 4; vertex++) {
                minX = Math.min(minX, quad.x[vertex]);
                minY = Math.min(minY, quad.y[vertex]);
                minZ = Math.min(minZ, quad.z[vertex]);
                maxX = Math.max(maxX, quad.x[vertex]);
                maxY = Math.max(maxY, quad.y[vertex]);
                maxZ = Math.max(maxZ, quad.z[vertex]);
            }
        }
        float spanX = span(minX, maxX);
        float spanY = span(minY, maxY);
        float spanZ = span(minZ, maxZ);
        for (Quad quad : this.quads) {
            for (int vertex = 0; vertex < 4; vertex++) {
                if (quad.nx != 0) {
                    quad.u[vertex] = unit(quad.z[vertex], minZ, spanZ);
                    quad.v[vertex] = unit(quad.y[vertex], minY, spanY);
                } else if (quad.ny != 0) {
                    quad.u[vertex] = unit(quad.x[vertex], minX, spanX);
                    quad.v[vertex] = unit(quad.z[vertex], minZ, spanZ);
                } else {
                    quad.u[vertex] = unit(quad.x[vertex], minX, spanX);
                    quad.v[vertex] = unit(quad.y[vertex], minY, spanY);
                }
            }
        }
    }

    private static float span(float min, float max) {
        float span = max - min;
        return span < 1.0e-4F ? 1.0F : span;
    }

    private static float unit(float value, float min, float span) {
        return (value - min) / span * 16.0F;
    }

    private void floor(double centerX, double centerZ, double y, double radius, double inner, Texture texture, boolean up) {
        if (radius <= 0) {
            return;
        }
        if (inner <= 0) {
            this.cap(centerX, centerZ, y, radius, texture, up);
        } else if (inner < radius) {
            this.ring(centerX, centerZ, y, radius, inner, texture, up);
        }
    }

    private void ledge(double centerX, double centerZ, double y, double lower, double upper, Texture texture, boolean hole) {
        double inner = Math.min(lower, upper);
        double outer = Math.max(lower, upper);
        if (outer <= 0 || outer - inner < 1.0e-4) {
            return;
        }
        boolean up = hole ? lower < upper : lower > upper;
        if (inner <= 0) {
            this.cap(centerX, centerZ, y, outer, texture, up);
        } else {
            this.ring(centerX, centerZ, y, outer, inner, texture, up);
        }
    }

    public void cylinder(double centerX, double bottom, double centerZ, double height, double radius, double inner, Texture texture) {
        double start = Math.PI / 8.0;
        double step = Math.PI / 4.0;
        for (int side = 0; side < 8; side++) {
            double a0 = start + side * step;
            double a1 = a0 + step;
            this.wall(centerX, centerZ, bottom, height, radius, a0, a1, texture, true);
            if (inner > 0) {
                this.wall(centerX, centerZ, bottom, height, inner, a0, a1, texture, false);
            }
        }
        if (inner <= 0) {
            this.cap(centerX, centerZ, bottom + height, radius, texture, true);
            this.cap(centerX, centerZ, bottom, radius, texture, false);
        } else {
            this.ring(centerX, centerZ, bottom + height, radius, inner, texture, true);
            this.ring(centerX, centerZ, bottom, radius, inner, texture, false);
        }
    }

    private void wall(
            double centerX, double centerZ, double bottom, double height, double radius, double a0, double a1, Texture texture, boolean outward
    ) {
        double x0 = centerX + radius * Math.cos(a0);
        double z0 = centerZ + radius * Math.sin(a0);
        double x1 = centerX + radius * Math.cos(a1);
        double z1 = centerZ + radius * Math.sin(a1);
        double top = bottom + height;
        Direction facing = facing(x0, z0, x1, z1, centerX, centerZ, outward);
        if (outward) {
            this.face(x1, bottom, z1, x0, bottom, z0, x0, top, z0, x1, top, z1, facing, texture);
        } else {
            this.face(x0, bottom, z0, x1, bottom, z1, x1, top, z1, x0, top, z0, facing, texture);
        }
    }

    private void cap(double centerX, double centerZ, double y, double radius, Texture texture, boolean up) {
        double start = Math.PI / 8.0;
        double step = Math.PI / 4.0;
        Direction facing = up ? Direction.UP : Direction.DOWN;
        for (int side = 0; side < 8; side += 2) {
            double a0 = start + side * step;
            double a1 = a0 + step;
            double a2 = a1 + step;
            double x0 = centerX + radius * Math.cos(a0);
            double z0 = centerZ + radius * Math.sin(a0);
            double x1 = centerX + radius * Math.cos(a1);
            double z1 = centerZ + radius * Math.sin(a1);
            double x2 = centerX + radius * Math.cos(a2);
            double z2 = centerZ + radius * Math.sin(a2);
            if (up) {
                this.face(centerX, y, centerZ, x2, y, z2, x1, y, z1, x0, y, z0, facing, texture);
            } else {
                this.face(centerX, y, centerZ, x0, y, z0, x1, y, z1, x2, y, z2, facing, texture);
            }
        }
    }

    private void ring(double centerX, double centerZ, double y, double outer, double inner, Texture texture, boolean up) {
        double start = Math.PI / 8.0;
        double step = Math.PI / 4.0;
        Direction facing = up ? Direction.UP : Direction.DOWN;
        for (int side = 0; side < 8; side++) {
            double a0 = start + side * step;
            double a1 = a0 + step;
            double ox0 = centerX + outer * Math.cos(a0);
            double oz0 = centerZ + outer * Math.sin(a0);
            double ox1 = centerX + outer * Math.cos(a1);
            double oz1 = centerZ + outer * Math.sin(a1);
            double ix0 = centerX + inner * Math.cos(a0);
            double iz0 = centerZ + inner * Math.sin(a0);
            double ix1 = centerX + inner * Math.cos(a1);
            double iz1 = centerZ + inner * Math.sin(a1);
            if (up) {
                this.face(ox0, y, oz0, ix0, y, iz0, ix1, y, iz1, ox1, y, oz1, facing, texture);
            } else {
                this.face(ox0, y, oz0, ox1, y, oz1, ix1, y, iz1, ix0, y, iz0, facing, texture);
            }
        }
    }

    private static Direction facing(double x0, double z0, double x1, double z1, double centerX, double centerZ, boolean outward) {
        double mx = (x0 + x1) / 2.0 - centerX;
        double mz = (z0 + z1) / 2.0 - centerZ;
        if (!outward) {
            mx = -mx;
            mz = -mz;
        }
        if (Math.abs(mx) > Math.abs(mz)) {
            return mx > 0 ? Direction.EAST : Direction.WEST;
        }
        return mz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private void face(
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            Direction facing,
            Texture texture
    ) {
        Quad quad = new Quad(facing.getStepX(), facing.getStepY(), facing.getStepZ(), texture);
        put(quad, 0, x0, y0, z0, facing);
        put(quad, 1, x1, y1, z1, facing);
        put(quad, 2, x2, y2, z2, facing);
        put(quad, 3, x3, y3, z3, facing);
        this.quads.add(quad);
    }

    private static void put(Quad quad, int vertex, double x, double y, double z, Direction facing) {
        quad.x[vertex] = (float) x;
        quad.y[vertex] = (float) y;
        quad.z[vertex] = (float) z;
        float[] uv = uv(x, y, z, facing);
        quad.u[vertex] = uv[0];
        quad.v[vertex] = uv[1];
    }

    private static float[] uv(double x, double y, double z, Direction facing) {
        return switch (facing.getAxis()) {
            case X -> new float[] {(float) (z / 2.0), (float) (y / 2.0)};
            case Y -> new float[] {(float) (x / 2.0), (float) (z / 2.0)};
            case Z -> new float[] {(float) (x / 2.0), (float) (y / 2.0)};
        };
    }
}
