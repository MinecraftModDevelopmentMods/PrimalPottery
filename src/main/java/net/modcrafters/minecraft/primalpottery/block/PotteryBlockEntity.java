package net.modcrafters.minecraft.primalpottery.block;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.modcrafters.minecraft.primalpottery.item.PotteryItem;
import net.modcrafters.minecraft.primalpottery.pottery.Nbt;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import org.jspecify.annotations.Nullable;

public class PotteryBlockEntity extends BlockEntity {
    public static final int SLOTS = 4;
    public static final double SIZE = 12;
    public static final double OFFSET = (32.0 - SIZE * 2.0) / 4.0;
    public static final List<AABB> BOXES = List.of(box(0), box(1), box(2), box(3));

    private final Pottery[] slots = new Pottery[SLOTS];

    public PotteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POTTERY, pos, state);
    }

    public static AABB box(int index) {
        boolean positiveX = index == 1 || index == 2;
        boolean positiveZ = index == 2 || index == 3;
        double minX = positiveX ? 32.0 - OFFSET - SIZE : OFFSET;
        double minZ = positiveZ ? 32.0 - OFFSET - SIZE : OFFSET;
        return new AABB(minX / 32.0, 0, minZ / 32.0, (minX + SIZE) / 32.0, SIZE / 32.0, (minZ + SIZE) / 32.0);
    }

    public @Nullable Pottery get(int index) {
        return this.slots[index];
    }

    public Pottery[] slots() {
        return Arrays.copyOf(this.slots, this.slots.length);
    }

    public boolean put(int index, Pottery pottery) {
        if (index < 0 || index >= this.slots.length || this.slots[index] != null) {
            return false;
        }
        Pottery[] next = Arrays.copyOf(this.slots, this.slots.length);
        next[index] = pottery;
        this.replace(next);
        return true;
    }

    public @Nullable Pottery take(int index) {
        if (this.slots[index] == null) {
            return null;
        }
        Pottery taken = this.slots[index];
        Pottery[] next = Arrays.copyOf(this.slots, this.slots.length);
        next[index] = null;
        this.replace(next);
        return taken;
    }

    public void dropAll() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        for (Pottery pottery : this.slots) {
            if (pottery == null) {
                continue;
            }
            this.level.addFreshEntity(new ItemEntity(
                    this.level,
                    this.worldPosition.getX() + 0.5,
                    this.worldPosition.getY() + 0.5,
                    this.worldPosition.getZ() + 0.5,
                    PotteryItem.stack(pottery)
            ));
        }
    }

    private void replace(Pottery[] next) {
        boolean any = false;
        for (int index = 0; index < next.length; index++) {
            this.slots[index] = next[index];
            any |= next[index] != null;
        }
        if (!any && this.level != null) {
            this.level.removeBlock(this.worldPosition, false);
            return;
        }
        this.sync();
    }

    //? if >=26.1 {
    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        output.store("pottery", CompoundTag.CODEC, this.saveSlots());
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        input.read("pottery", CompoundTag.CODEC).ifPresent(this::loadSlots);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("pottery", this.saveSlots());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.loadSlots(Nbt.compound(tag, "pottery"));
    }
    *///?}

    private CompoundTag saveSlots() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Pottery slot : this.slots) {
            CompoundTag entry = new CompoundTag();
            if (slot != null) {
                entry.put("pottery", slot.save());
            }
            list.add(entry);
        }
        tag.put("slots", list);
        return tag;
    }

    private void loadSlots(CompoundTag tag) {
        ListTag list = Nbt.list(tag, "slots");
        for (int index = 0; index < this.slots.length; index++) {
            this.slots[index] = null;
            if (index >= list.size()) {
                continue;
            }
            CompoundTag entry = Nbt.compoundAt(list, index);
            CompoundTag pottery = Nbt.compound(entry, "pottery");
            if (!pottery.isEmpty()) {
                this.slots[index] = Pottery.load(pottery);
            }
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
