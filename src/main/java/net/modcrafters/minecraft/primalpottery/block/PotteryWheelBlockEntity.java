package net.modcrafters.minecraft.primalpottery.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.modcrafters.minecraft.primalpottery.item.ModItems;
import net.modcrafters.minecraft.primalpottery.item.RawPotteryItem;
import net.modcrafters.minecraft.primalpottery.item.TemplateItem;
import net.modcrafters.minecraft.primalpottery.pottery.Nbt;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import org.jspecify.annotations.Nullable;

public class PotteryWheelBlockEntity extends BlockEntity {
    public static final int TABLE = 0;
    public static final int BIG_WHEEL = 1;
    public static final int SMALL_WHEEL = 2;
    public static final List<AABB> BOXES = List.of(
            box(0, 20, 0, 32, 24, 32),
            box(4, 5, 4, 28, 8, 28),
            box(9.5, 26, 9.5, 22.5, 28, 22.5)
    );

    private boolean checkUpperHalf = true;
    private int spinTicks;
    private double angle;
    private boolean hasClay;
    private float clayProgress;
    /** The one blueprint sitting in the wheel. Empty when none is installed. */
    private ItemStack installed = ItemStack.EMPTY;
    private Pottery start;

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
            if (this.hasClay && this.clayProgress >= 1) {
                return;
            }
            this.spinTicks += 24;
            this.sync();
        }
    }

    public void install(Pottery pottery) {
        ItemStack stack = new ItemStack(ModItems.BLUEPRINT);
        TemplateItem.write(stack, pottery);
        this.installed = stack;
        this.sync();
    }

    /** Puts one blueprint in the slot and returns whatever was there. */
    public ItemStack replaceTemplate(ItemStack stack) {
        ItemStack previous = this.installed;
        this.installed = stack;
        this.sync();
        return previous;
    }

    public ItemStack removeTemplate() {
        ItemStack previous = this.installed;
        this.installed = ItemStack.EMPTY;
        this.sync();
        return previous;
    }

    public void clearTemplate() {
        this.removeTemplate();
    }

    public ItemStack templateItem() {
        return this.installed;
    }

    public void startClay(@Nullable Pottery start) {
        this.hasClay = true;
        this.clayProgress = 0;
        this.start = start;
        this.sync();
    }

    public void takeClay(Player player) {
        boolean present = this.hasClay;
        ItemStack drop = present ? this.formingStack() : ItemStack.EMPTY;
        this.clearForming();
        if (present && this.level != null && !this.level.isClientSide()) {
            this.level.addFreshEntity(new ItemEntity(this.level, player.getX(), player.getY(), player.getZ(), drop));
        }
        this.sync();
    }

    /** The raw stack {@link #takeClay} would drop, then clears the clay. Empty when the wheel is bare. */
    public ItemStack removeForming() {
        if (!this.hasClay) {
            return ItemStack.EMPTY;
        }
        ItemStack drop = this.formingStack();
        this.clearForming();
        return drop;
    }

    private ItemStack formingStack() {
        Pottery shape = this.clayProgress >= 1
                ? this.formed()
                : Pottery.shaped(this.start, this.formed(), this.clayProgress);
        return RawPotteryItem.stack(shape);
    }

    private void clearForming() {
        this.hasClay = false;
        this.clayProgress = 0;
        this.start = null;
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
        return this.installed.isEmpty() ? null : TemplateItem.profile(this.installed);
    }

    public @Nullable Pottery start() {
        return this.start;
    }

    public Pottery formed() {
        return Pottery.formed(this.template());
    }

    public float angle() {
        return (float) this.angle;
    }

    // Wheels saved by the single-block version may still need their upper half.
    private void restoreUpperHalf() {
        if (this.level == null || this.level.isClientSide()) {
            this.checkUpperHalf = this.level == null;
            return;
        }
        BlockPos above = this.worldPosition.above();
        BlockState upper = this.getBlockState().setValue(PotteryWheelBlock.HALF, PotteryWheelBlock.UPPER);
        BlockState existing = this.level.getBlockState(above);
        if (existing == upper || this.level.isOutsideBuildHeight(above)) {
            this.checkUpperHalf = false;
        } else if (existing.isAir()) {
            this.checkUpperHalf = !this.level.setBlock(above, upper, Block.UPDATE_ALL);
        }
    }

    public void tick() {
        if (this.checkUpperHalf) {
            this.restoreUpperHalf();
        }
        if (this.hasClay && this.clayProgress >= 1 && this.spinTicks > 0) {
            this.spinTicks = 0;
            if (this.level != null && !this.level.isClientSide()) {
                this.sync();
            }
            return;
        }
        if (this.spinTicks <= 0) {
            this.spinTicks = 0;
            return;
        }
        this.spinTicks--;
        this.angle += Math.PI / 6;
        while (this.angle > Math.PI * 2) {
            this.angle -= Math.PI * 2;
        }
        boolean finished = false;
        if (this.hasClay && this.clayProgress < 1) {
            this.clayProgress = Math.min(1, this.clayProgress + 0.01F);
            if (this.clayProgress >= 1) {
                this.clayProgress = 1;
                this.spinTicks = 0;
                finished = true;
            }
        }
        // Clients animate between server snapshots. Persist the authoritative tick and settle all clients at rest.
        if (this.level != null && !this.level.isClientSide()) {
            if (finished || this.spinTicks == 0) {
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
        this.saveTemplate(tag);
        if (this.start != null) {
            tag.put("start", this.start.save());
        }
        return tag;
    }

    private void loadWheel(CompoundTag tag) {
        this.spinTicks = Math.max(0, Nbt.getInt(tag, "spin_ticks"));
        double savedAngle = Nbt.getDouble(tag, "angle");
        this.angle = Double.isFinite(savedAngle) ? savedAngle % (Math.PI * 2) : 0;
        this.hasClay = Nbt.getBoolean(tag, "clay");
        this.clayProgress = Nbt.getFloat(tag, "progress");
        this.loadTemplate(tag);
        CompoundTag start = Nbt.compound(tag, "start");
        this.start = start.isEmpty() ? null : Pottery.load(start);
    }

    private void saveTemplate(CompoundTag tag) {
        if (this.installed.isEmpty()) {
            return;
        }
        tag.putBoolean("has_template", true);
        Pottery saved = TemplateItem.read(this.installed);
        if (saved != null) {
            tag.put("template", saved.save());
        }
    }

    private void loadTemplate(CompoundTag tag) {
        CompoundTag saved = Nbt.compound(tag, "template");
        if (saved.isEmpty() && !Nbt.getBoolean(tag, "has_template")) {
            this.installed = ItemStack.EMPTY;
            return;
        }
        ItemStack stack = new ItemStack(ModItems.BLUEPRINT);
        if (!saved.isEmpty()) {
            TemplateItem.write(stack, Pottery.load(saved));
        }
        this.installed = stack;
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
