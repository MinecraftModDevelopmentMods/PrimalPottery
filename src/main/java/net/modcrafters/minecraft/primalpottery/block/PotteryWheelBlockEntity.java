package net.modcrafters.minecraft.primalpottery.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.modcrafters.minecraft.primalpottery.item.PotteryItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.modcrafters.minecraft.primalpottery.pottery.Nbt;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import org.jspecify.annotations.Nullable;

public class PotteryWheelBlockEntity extends BlockEntity {
    /** Collision height. The drawn wheel stays one block tall. */
    public static final double BLOCK_HEIGHT = 1.5;
    public static final int TABLE = 0;
    public static final int BIG_WHEEL = 1;
    public static final int SMALL_WHEEL = 2;
    public static final List<AABB> BOXES = List.of(
            box(0, 20, 0, 32, 24, 32),
            box(4, 5, 4, 28, 8, 28),
            box(9.5, 26, 9.5, 22.5, 28, 22.5)
    );

    private int spinTicks;
    private double angle;
    private boolean hasClay;
    private float clayProgress;
    private Pottery template;

    public PotteryWheelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WHEEL, pos, state);
    }

    public static AABB box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new AABB(
                minX / 32.0,
                minY / 32.0,
                minZ / 32.0,
                maxX / 32.0,
                maxY / 32.0,
                maxZ / 32.0
        );
    }

    public void spin() {
        if (this.level != null && !this.level.isClientSide()) {
            this.spinTicks += 24;
            this.sync();
        }
    }

    public void install(Pottery pottery) {
        this.template = pottery;
        this.sync();
    }

    public void clearTemplate() {
        this.template = null;
        this.sync();
    }

    public void startClay() {
        this.hasClay = true;
        this.clayProgress = 0;
        this.sync();
    }

    public void takeClay(Player player) {
        this.hasClay = false;
        this.clayProgress = 0;
        if (this.level != null && !this.level.isClientSide()) {
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            ItemEntity drop = new ItemEntity(this.level, x, y, z, PotteryItem.stack(this.formed()));
            this.level.addFreshEntity(drop);
        }
        this.sync();
    }

    public int spinTicks() {
        return this.spinTicks;
    }

    public boolean hasClay() {
        return this.hasClay;
    }

    public float clayProgress() {
        return this.clayProgress;
    }

    public @Nullable Pottery template() {
        return this.template;
    }

    public Pottery formed() {
        return Pottery.formed(this.template);
    }

    public float angle() {
        return (float) this.angle;
    }

    public void tick() {
        this.keepHead();
        if (this.spinTicks <= 0) {
            this.spinTicks = 0;
            return;
        }
        this.spinTicks--;
        this.angle += Math.PI / 6;
        while (this.angle > Math.PI * 2) {
            this.angle -= Math.PI * 2;
        }
        if (this.hasClay && this.clayProgress < 1) {
            this.clayProgress = Math.min(1, this.clayProgress + 0.01F);
        }
        // Clients animate between server snapshots. Persist the authoritative tick and settle all clients at rest.
        if (this.level != null && !this.level.isClientSide()) {
            if (this.spinTicks == 0) {
                this.sync();
            } else {
                this.setChanged();
            }
        }
    }

    //? if >=26.1 {
    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        output.store("wheel", CompoundTag.CODEC, this.saveWheel());
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        input.read("wheel", CompoundTag.CODEC).ifPresent(this::loadWheel);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("wheel", this.saveWheel());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.loadWheel(Nbt.compound(tag, "wheel"));
    }
    *///?}

    private CompoundTag saveWheel() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("spin_ticks", this.spinTicks);
        tag.putDouble("angle", this.angle);
        tag.putBoolean("clay", this.hasClay);
        tag.putFloat("progress", this.clayProgress);
        if (this.template != null) {
            tag.put("template", this.template.save());
        }
        return tag;
    }

    private void loadWheel(CompoundTag tag) {
        this.spinTicks = Math.max(0, Nbt.getInt(tag, "spin_ticks"));
        double savedAngle = Nbt.getDouble(tag, "angle");
        this.angle = Double.isFinite(savedAngle) ? savedAngle % (Math.PI * 2) : 0;
        this.hasClay = Nbt.getBoolean(tag, "clay");
        this.clayProgress = Nbt.getFloat(tag, "progress");
        CompoundTag template = Nbt.compound(tag, "template");
        this.template = template.isEmpty() ? null : Pottery.load(template);
    }

    private void keepHead() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        BlockState state = this.getBlockState();
        if (!state.is(ModBlocks.WHEEL) || state.getValue(PotteryWheelBlock.HALF) != net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
            return;
        }
        BlockPos above = this.worldPosition.above();
        if (this.level.getBlockState(above).isAir()) {
            this.level.setBlock(above, state.setValue(PotteryWheelBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
    }

    private void sync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }
}
