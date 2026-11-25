package net.modcrafters.minecraft.primalpottery.pottery;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class Pottery {
    // The clay mesh uses regular octagons whose vertices lie on the disc radius.
    private static final double CAVITY_AREA_FACTOR = 2 * Math.sqrt(2);
    private final List<PotteryDisc> discs;

    public Pottery(List<PotteryDisc> discs) {
        this.discs = List.copyOf(discs);
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
            volume += CAVITY_AREA_FACTOR * inner * inner * disc.height();
        }
        return new Info(PotteryType.LUMP_NO_BOTTOM, 0);
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
        for (int index = 0; index < list.size(); index++) {
            CompoundTag discTag = Nbt.compoundAt(list, index);
            discs.add(new PotteryDisc(
                    Nbt.getDouble(discTag, "height"),
                    Nbt.getDouble(discTag, "radius"),
                    Nbt.getDouble(discTag, "inner")
            ));
        }
        return new Pottery(discs);
    }

    /** The pot a wheel forms: the installed template, or the plain vase when none is installed. */
    public static Pottery formed(Pottery template) {
        return template == null ? wheelShape() : template;
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
