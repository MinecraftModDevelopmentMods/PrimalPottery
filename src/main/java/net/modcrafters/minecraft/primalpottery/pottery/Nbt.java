package net.modcrafters.minecraft.primalpottery.pottery;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class Nbt {
    private Nbt() {
    }

    public static double getDouble(CompoundTag tag, String key) {
        //? if >=26.1 {
        return tag.getDoubleOr(key, 0);
        //?} else
        //return tag.getDouble(key);
    }

    public static float getFloat(CompoundTag tag, String key) {
        //? if >=26.1 {
        return tag.getFloatOr(key, 0);
        //?} else
        //return tag.getFloat(key);
    }

    public static boolean getBoolean(CompoundTag tag, String key) {
        //? if >=26.1 {
        return tag.getBooleanOr(key, false);
        //?} else
        //return tag.getBoolean(key);
    }

    public static CompoundTag compound(CompoundTag tag, String key) {
        //? if >=26.1 {
        return tag.getCompoundOrEmpty(key);
        //?} else
        //return tag.getCompound(key);
    }

    public static ListTag list(CompoundTag tag, String key) {
        //? if >=26.1 {
        return tag.getListOrEmpty(key);
        //?} else
        //return tag.getList(key, 10);
    }

    public static CompoundTag compoundAt(ListTag list, int index) {
        //? if >=26.1 {
        return list.getCompoundOrEmpty(index);
        //?} else
        //return list.getCompound(index);
    }
}
