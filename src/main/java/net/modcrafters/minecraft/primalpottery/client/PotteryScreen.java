package net.modcrafters.minecraft.primalpottery.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.modcrafters.minecraft.primalpottery.block.PotteryBlockEntity;
import net.modcrafters.minecraft.primalpottery.item.PotteryItem;
import net.modcrafters.minecraft.primalpottery.network.LidSync;
import net.modcrafters.minecraft.primalpottery.network.PlacedLidSync;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import net.modcrafters.minecraft.primalpottery.pottery.VesselContents;
import org.jspecify.annotations.Nullable;
//? if >=26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if <26.1
//import net.minecraft.client.gui.GuiGraphicsExtractor;

/** What a held pot contains, and a button that puts or takes its lid. */
public final class PotteryScreen extends Screen {
    private static final int IMAGE_WIDTH = 120;
    private static final int IMAGE_HEIGHT = 90;
    private static final int PANEL = 0xF0201814;

    private final boolean mainHand;
    private final @Nullable BlockPos placed;
    private final int slot;
    private final Pottery pottery;
    private final VesselContents contents;
    private final @Nullable String dye;
    private boolean closed;
    private Button lidButton;
    private int left;
    private int top;

    public PotteryScreen(boolean mainHand, Pottery pottery, VesselContents contents, boolean closed, @Nullable String dye) {
        this(mainHand, null, 0, pottery, contents, closed, dye);
    }

    public PotteryScreen(BlockPos pos, int slot, Pottery pottery, VesselContents contents, boolean closed, @Nullable String dye) {
        this(false, pos, slot, pottery, contents, closed, dye);
    }

    private PotteryScreen(
            boolean mainHand, @Nullable BlockPos placed, int slot, Pottery pottery, VesselContents contents, boolean closed, @Nullable String dye
    ) {
        //? if >=26.1 {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("Pottery"));
        //?} else
        //super(Component.literal("Pottery"));
        this.mainHand = mainHand;
        this.placed = placed == null ? null : placed.immutable();
        this.slot = slot;
        this.pottery = pottery;
        this.contents = contents;
        this.dye = dye;
        this.closed = closed;
    }

    public static void open(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        show(new PotteryScreen(
                hand == InteractionHand.MAIN_HAND,
                PotteryItem.read(stack),
                PotteryItem.contents(stack),
                PotteryItem.lid(stack),
                PotteryItem.dye(stack)
        ));
    }

    public static void open(Level level, BlockPos pos, int slot) {
        if (slot < 0 || slot >= PotteryBlockEntity.SLOTS) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof PotteryBlockEntity pottery)) {
            return;
        }
        Pottery pot = pottery.get(slot);
        if (pot == null) {
            return;
        }
        show(new PotteryScreen(pos, slot, pot, pottery.contents(slot), pottery.lid(slot), pottery.dye(slot)));
    }

    private static void show(PotteryScreen screen) {
        //? if >=26.1 {
        Minecraft.getInstance().setScreenAndShow(screen);
        //?} else
        //Minecraft.getInstance().setScreen(screen);
    }

    @Override
    protected void init() {
        this.layout();
        this.lidButton = this.addRenderableWidget(Button.builder(Component.literal(this.label()), button -> this.pressLid())
                .bounds(this.left + 10, this.top + 66, 100, 20)
                .build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    //? if >=26.1 {
    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.layout();
        this.paint(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.drawContents(graphics);
    }
    //?} else {
    /*@Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.layout();
        super.render(graphics, mouseX, mouseY, partialTick);
        this.drawContents(graphics);
    }

    @Override
    public void renderBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        this.paint(graphics);
    }
    *///?}

    private void pressLid() {
        if (this.closed) {
            this.closed = false;
            this.lidButton.setMessage(Component.literal(this.label()));
            this.send(false);
            return;
        }
        if (this.pottery.lidDisc() == null) {
            return;
        }
        this.closed = true;
        this.lidButton.setMessage(Component.literal(this.label()));
        this.send(true);
    }

    private void send(boolean closed) {
        if (this.placed != null) {
            PlacedLidSync.send(this.placed, this.slot, closed);
            return;
        }
        LidSync.send(this.mainHand, closed);
    }

    private String label() {
        return this.closed ? "Take lid" : "Put lid";
    }

    private void paint(GuiGraphicsExtractor graphics) {
        graphics.fill(this.left, this.top, this.left + IMAGE_WIDTH, this.top + IMAGE_HEIGHT, PANEL);
    }

    private void drawContents(GuiGraphicsExtractor graphics) {
        PotteryPreview.draw(graphics, this.pottery, this.closed, this.dye, this.left + 8, this.top + 4, this.left + 112, this.top + 36);
        if (!this.pottery.classify().type().isVessel()) {
            return;
        }
        int x = this.left + 8;
        int y = this.top + 38;
        int line = this.font.lineHeight;
        List<Component> lines = new ArrayList<>();
        PotteryItem.contentsTooltip(this.pottery, this.contents, lines::add);
        for (int index = 0; index < lines.size(); index++) {
            graphics.text(this.font, lines.get(index), x, y + line * index, -1);
        }
    }

    private void layout() {
        this.left = (this.width - IMAGE_WIDTH) / 2;
        this.top = (this.height - IMAGE_HEIGHT) / 2;
    }
}
