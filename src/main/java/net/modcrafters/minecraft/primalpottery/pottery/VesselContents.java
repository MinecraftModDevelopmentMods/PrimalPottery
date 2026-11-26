package net.modcrafters.minecraft.primalpottery.pottery;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/** A vessel is empty, or it holds one source fluid and an amount in millilitres. */
public record VesselContents(Fluid fluid, int amount) {
    public static final VesselContents EMPTY = new VesselContents(Fluids.EMPTY, 0);

    public enum Choice {
        FILL,
        PLACE,
        REFUSE
    }

    /**
     * Facts for {@link #choose}. The source id is what a fill would store. It does not
     * decide the choice: a fluid other than water or lava still fills.
     */
    public record Facts(
            boolean player,
            boolean vessel,
            boolean empty,
            int volume,
            boolean povFluid,
            String sourceId,
            boolean interact,
            boolean canPlace,
            boolean clickedFluid,
            boolean clickedSource,
            boolean sneaking,
            boolean holding
    ) {
    }

    public boolean isEmpty() {
        return this.fluid == Fluids.EMPTY || this.amount <= 0;
    }

    /** Light of this fluid's registered source block. An empty vessel emits nothing. */
    public int emission() {
        if (this.isEmpty()) {
            return 0;
        }
        int light = this.fluid.defaultFluidState().createLegacyBlock().getLightEmission();
        if (light < 0) {
            return 0;
        }
        return Math.min(light, 15);
    }

    /** Millilitres of {@code held} that fit in a pot storing {@code stored} with capacity {@code volume}. */
    public static int poured(VesselContents held, VesselContents stored, int volume) {
        if (held.isEmpty() || volume <= 0) {
            return 0;
        }
        if (!stored.isEmpty() && stored.fluid() != held.fluid()) {
            return 0;
        }
        int room = volume - (stored.isEmpty() ? 0 : stored.amount());
        if (room <= 0) {
            return 0;
        }
        return Math.min(held.amount(), room);
    }

    /** Water uses the same blue as the world mesh. Lava and every other fluid stay uncolored. */
    public static int tooltipColor(Fluid fluid) {
        return fluid == Fluids.WATER ? 0x3F76E4 : -1;
    }

    public static VesselContents of(Fluid fluid, int amount) {
        Fluid source = source(fluid);
        if (source == Fluids.EMPTY || amount <= 0) {
            return EMPTY;
        }
        return new VesselContents(source, amount);
    }

    /** Flowing water and lava become their source. Any other fluid stays itself. */
    public static Fluid source(Fluid fluid) {
        if (fluid instanceof FlowingFluid flowing) {
            return flowing.getSource();
        }
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    /** True when the block is that fluid's own block. A waterlogged block and a bubble column are not. */
    public static boolean ownsFluidBlock(BlockState state, FluidState fluid) {
        if (fluid.isEmpty()) {
            return false;
        }
        Block legacy = fluid.createLegacyBlock().getBlock();
        //? if >=26.1 {
        return state.getBlock() == legacy;
        //?} else
        //return state.is(legacy);
    }

    public static Choice choose(Facts facts) {
        if (facts.player && facts.vessel && facts.empty && facts.volume > 0 && facts.povFluid) {
            return facts.interact ? Choice.FILL : Choice.REFUSE;
        }
        boolean sneakPlace = facts.sneaking && facts.vessel && facts.holding && facts.clickedSource;
        if (facts.canPlace && facts.clickedFluid && !sneakPlace) {
            return Choice.REFUSE;
        }
        return Choice.PLACE;
    }

    public void write(CompoundTag tag) {
        if (this.isEmpty()) {
            tag.remove("fluid");
            tag.remove("amount");
            return;
        }
        Identifier id = BuiltInRegistries.FLUID.getKey(this.fluid);
        if (id == null) {
            tag.remove("fluid");
            tag.remove("amount");
            return;
        }
        tag.putString("fluid", id.toString());
        tag.putInt("amount", this.amount);
    }

    public static VesselContents read(CompoundTag tag) {
        String id = Nbt.getString(tag, "fluid");
        int amount = Nbt.getInt(tag, "amount");
        if (id.isBlank() || amount <= 0) {
            return EMPTY;
        }
        Identifier parsed = Identifier.tryParse(id);
        if (parsed == null || !BuiltInRegistries.FLUID.containsKey(parsed)) {
            return EMPTY;
        }
        //? if >=26.1 {
        Fluid fluid = source(BuiltInRegistries.FLUID.getValue(parsed));
        //?} else
        //Fluid fluid = source(BuiltInRegistries.FLUID.get(parsed));
        if (fluid == Fluids.EMPTY) {
            return EMPTY;
        }
        return new VesselContents(fluid, amount);
    }
}
