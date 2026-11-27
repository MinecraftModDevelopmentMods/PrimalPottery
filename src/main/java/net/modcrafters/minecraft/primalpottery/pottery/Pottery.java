package net.modcrafters.minecraft.primalpottery.pottery;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.jspecify.annotations.Nullable;

public final class Pottery {
    public static final int MAX_DISCS = 64;
    public static final double MAX_RADIUS = 5.5;
    /** Radius of the clay blank a wheel shapes from when no profile is put back on. */
    public static final double BLANK_RADIUS = 4;
    private static final double MAX_HEIGHT = 12; // Height of the wheel's editable clay blank.
    // The clay mesh uses regular octagons whose vertices lie on the disc radius.
    private static final double CAVITY_AREA_FACTOR = 2 * Math.sqrt(2);
    private final List<PotteryDisc> discs;

    public Pottery(List<PotteryDisc> discs) {
        this.discs = List.copyOf(discs);
    }

    public static boolean validProfile(List<PotteryDisc> discs) {
        if (discs.isEmpty() || discs.size() > MAX_DISCS) {
            return false;
        }
        double height = 0;
        for (PotteryDisc disc : discs) {
            if (!(disc.height() > 0 && disc.radius() >= 0 && disc.radius() <= MAX_RADIUS
                    && disc.innerRadius() >= 0 && disc.innerRadius() <= disc.radius())) {
                return false;
            }
            height += disc.height();
        }
        return height <= MAX_HEIGHT;
    }

    public List<PotteryDisc> discs() {
        return this.discs;
    }

    public String cacheKey() {
        StringBuilder key = new StringBuilder("pot");
        for (PotteryDisc disc : this.discs) {
            key.append("::").append(disc.radius()).append(':').append(disc.height());
        }
        return key.toString();
    }

    public Info classify() {
        int rim = -1;
        double maxRadius = 0;
        for (int index = 0; index < this.discs.size(); index++) {
            PotteryDisc disc = this.discs.get(index);
            if (!Double.isFinite(disc.height()) || !Double.isFinite(disc.radius()) || !Double.isFinite(disc.innerRadius())
                    || disc.height() <= 0 || disc.radius() < 0 || disc.innerRadius() < 0) {
                return new Info(PotteryType.LUMP, 0);
            }
            if (disc.radius() == 0 && disc.innerRadius() == 0) {
                continue;
            }
            if (disc.radius() - disc.innerRadius() < 0.5) {
                return new Info(PotteryType.LUMP_THIN, 0);
            }
            if (rim < 0) {
                rim = index;
            }
            maxRadius = Math.max(maxRadius, disc.radius());
        }
        if (rim < 0 || this.discs.get(rim).innerRadius() == 0) {
            return new Info(PotteryType.LUMP, 0);
        }

        double volume = 0;
        double openingRadius = this.discs.get(rim).innerRadius();
        for (int index = rim; index < this.discs.size(); index++) {
            PotteryDisc disc = this.discs.get(index);
            double inner = disc.innerRadius();
            if (inner == 0) {
                if (disc.radius() == 0) {
                    break;
                }
                PotteryType type = vesselType(openingRadius, maxRadius);
                return new Info(type, (int) Math.min(Integer.MAX_VALUE, Math.round(volume)));
            }
            // One profile unit represents one centimeter for the capacity estimate.
            volume += cavityArea(inner) * disc.height();
        }
        return new Info(PotteryType.LUMP_NO_BOTTOM, 0);
    }

    public static double cavityArea(double innerRadius) {
        return CAVITY_AREA_FACTOR * innerRadius * innerRadius;
    }

    /** One block pixel in the placed mesh, which uses 32 units per block. */
    public static final double PIXEL = 2;

    /**
     * Where a lid is drawn. {@code top} is the pot's top, {@code layer} is the top disc's height,
     * and {@code hole} is that disc's inner radius. The lid is not stored on the profile.
     */
    public record Cover(double top, double layer, double hole) {
    }

    /** The rim a lid can cover, or null when the profile has no open mouth. */
    public @Nullable Cover cover() {
        int rim = this.rimIndex();
        if (rim < 0) {
            return null;
        }
        double top = 0;
        for (PotteryDisc disc : this.discs) {
            top += disc.height();
        }
        PotteryDisc disc = this.discs.get(rim);
        return new Cover(top, disc.height(), disc.innerRadius());
    }

    /**
     * The lid plate: one pixel thick, and one pixel wider than the hole. Null when {@link #cover()} is null.
     */
    public @Nullable PotteryDisc lidDisc() {
        Cover cover = this.cover();
        if (cover == null) {
            return null;
        }
        return new PotteryDisc(PIXEL, cover.hole() + PIXEL / 2.0, 0);
    }

    /** The saved discs. A lid is drawn from {@link #cover()} and is not another profile disc. */
    public List<PotteryDisc> drawDiscs(boolean closed) {
        return this.discs();
    }

    private int rimIndex() {
        for (int index = 0; index < this.discs.size(); index++) {
            PotteryDisc disc = this.discs.get(index);
            if (!Double.isFinite(disc.height()) || !Double.isFinite(disc.radius()) || !Double.isFinite(disc.innerRadius())
                    || disc.height() <= 0 || disc.radius() < 0 || disc.innerRadius() < 0) {
                return -1;
            }
            if (disc.radius() == 0 && disc.innerRadius() == 0) {
                continue;
            }
            if (disc.radius() - disc.innerRadius() < 0.5 || disc.innerRadius() <= 0) {
                return -1;
            }
            return index;
        }
        return -1;
    }

    private static PotteryType vesselType(double openingRadius, double maxRadius) {
        if (openingRadius > 0.9 * maxRadius) {
            return PotteryType.POT;
        }
        if (openingRadius < 0.5 * maxRadius) {
            return PotteryType.JUG;
        }
        return PotteryType.JAR;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (PotteryDisc disc : this.discs) {
            CompoundTag discTag = new CompoundTag();
            discTag.putDouble("height", disc.height());
            discTag.putDouble("radius", disc.radius());
            discTag.putDouble("inner", disc.innerRadius());
            list.add(discTag);
        }
        tag.put("discs", list);
        return tag;
    }

    public static Pottery load(CompoundTag tag) {
        List<PotteryDisc> discs = new ArrayList<>();
        ListTag list = Nbt.list(tag, "discs");
        if (list.size() > MAX_DISCS) {
            return display();
        }
        for (int index = 0; index < list.size(); index++) {
            CompoundTag discTag = Nbt.compoundAt(list, index);
            double radius = Nbt.getDouble(discTag, "radius");
            double inner = Nbt.getDouble(discTag, "inner");
            // Older editors allowed the inner brush to cross the outer edge, producing a zero-width wall.
            if (inner > radius && inner <= MAX_RADIUS) {
                inner = radius;
            }
            discs.add(new PotteryDisc(Nbt.getDouble(discTag, "height"), radius, inner));
        }
        return validProfile(discs) ? new Pottery(discs) : display();
    }

    /** The pot a wheel forms: the installed template, or the plain vase when none is installed. */
    public static Pottery formed(Pottery template) {
        return template == null ? wheelShape() : template;
    }

    /**
     * The only lerp from a blank or a returned profile toward {@code target}.
     * Progress at least 1 returns {@code target}. Below 0 is 0.
     * The result keeps the target's disc count and heights.
     */
    public static Pottery shaped(@Nullable Pottery start, Pottery target, double progress) {
        if (progress >= 1) {
            return target;
        }
        if (progress < 0) {
            progress = 0;
        }
        double targetHeight = height(target);
        double startHeight = start == null ? 0 : height(start);
        List<PotteryDisc> formed = new ArrayList<>();
        double above = 0;
        for (PotteryDisc disc : target.discs) {
            double sampleRadius = BLANK_RADIUS;
            double sampleInner = 0;
            if (start != null) {
                double fraction = targetHeight == 0 ? 0 : (above + disc.height() / 2.0) / targetHeight;
                PotteryDisc sample = sample(start, fraction * startHeight, startHeight);
                sampleRadius = sample.radius();
                sampleInner = sample.innerRadius();
            }
            double radius = sampleRadius + (disc.radius() - sampleRadius) * progress;
            double inner = sampleInner + (disc.innerRadius() - sampleInner) * progress;
            radius = Math.min(MAX_RADIUS, Math.max(0, radius));
            inner = Math.min(radius, Math.max(0, inner));
            formed.add(new PotteryDisc(disc.height(), radius, inner));
            above += disc.height();
        }
        return new Pottery(formed);
    }

    private static double height(Pottery pottery) {
        double height = 0;
        for (PotteryDisc disc : pottery.discs) {
            height += disc.height();
        }
        return height;
    }

    /** Walks from the top. A point on a disc's end is the next disc. The total height keeps the last disc. */
    private static PotteryDisc sample(Pottery start, double point, double total) {
        List<PotteryDisc> discs = start.discs;
        if (point >= total) {
            return discs.get(discs.size() - 1);
        }
        double walked = 0;
        for (int index = 0; index < discs.size(); index++) {
            PotteryDisc disc = discs.get(index);
            double end = walked + disc.height();
            if (point < end || index == discs.size() - 1) {
                return disc;
            }
            walked = end;
        }
        return discs.get(discs.size() - 1);
    }

    public static Pottery wheelShape() {
        return new Pottery(List.of(
                new PotteryDisc(1, 2),
                new PotteryDisc(5, 3),
                new PotteryDisc(2, 2),
                new PotteryDisc(2, 1),
                new PotteryDisc(1, 2)
        ));
    }

    public static Pottery display() {
        return new Pottery(List.of(
                new PotteryDisc(1, 2),
                new PotteryDisc(5, 3),
                new PotteryDisc(2, 4),
                new PotteryDisc(2, 5),
                new PotteryDisc(1, 5.5)
        ));
    }

    public record Info(PotteryType type, int volume) {
    }
}
