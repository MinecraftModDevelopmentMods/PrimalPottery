package net.modcrafters.minecraft.primalpottery.client;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.modcrafters.minecraft.primalpottery.Ids;
import net.modcrafters.minecraft.primalpottery.item.TemplateItem;
import net.modcrafters.minecraft.primalpottery.network.TemplateSync;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryDisc;
//? if >=26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if >=26.1
import net.minecraft.client.input.MouseButtonEvent;
//? if <26.1
//import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The pottery template: drag the profile, then read what kind of pot it is. */
public final class TemplateScreen extends Screen {
    private static final Identifier TEXTURE = Ids.of("textures/gui/main.png");
    private static final int IMAGE_WIDTH = 180;
    private static final int IMAGE_HEIGHT = 170;
    private static final int PROFILE_X = 10;
    private static final int PROFILE_Y = 10;
    private static final int PROFILE_WIDTH = 100;
    private static final int PROFILE_HEIGHT = 120;
    private static final int BAR_COLOR = 0x60500000;

    private final boolean mainHand;
    private final List<PotteryDisc> originalDiscs;
    private final List<PotteryDisc> discs;
    private int left;
    private int top;

    public TemplateScreen(boolean mainHand, List<PotteryDisc> discs) {
        //? if >=26.1 {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("Pottery"));
        //?} else
        //super(Component.literal("Pottery"));
        this.mainHand = mainHand;
        this.originalDiscs = List.copyOf(discs);
        this.discs = new ArrayList<>(discs);
    }

    public static void open(Player player, InteractionHand hand) {
        List<PotteryDisc> discs = TemplateItem.profile(player.getItemInHand(hand)).discs();
        TemplateScreen screen = new TemplateScreen(hand == InteractionHand.MAIN_HAND, discs);
        //? if >=26.1 {
        Minecraft.getInstance().setScreenAndShow(screen);
        //?} else
        //Minecraft.getInstance().setScreen(screen);
    }

    @Override
    protected void init() {
        this.layout();
        this.addRenderableWidget(Button.builder(Component.literal("Simplify 2"), button -> this.simplify(2))
                .bounds(this.left + 118, this.top + 88, 53, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Simplify 3"), button -> this.simplify(3))
                .bounds(this.left + 118, this.top + 111, 53, 20)
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
    //?}

    @Override
    public void removed() {
        if (!this.discs.equals(this.originalDiscs)) {
            TemplateSync.send(this.mainHand, this.discs);
        }
        super.removed();
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.layout();
        this.paint(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.drawPreview(graphics);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.sculpt(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.sculpt(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }
    //?} else {
    /*@Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.layout();
        super.render(graphics, mouseX, mouseY, partialTick);
        this.drawPreview(graphics);
    }

    @Override
    public void renderBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        this.paint(graphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.sculpt(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (this.sculpt(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }
    *///?}

    private void drawPreview(GuiGraphicsExtractor graphics) {
        // Interior of the preview recess in main.png.
        PotteryPreview.draw(graphics, this.discs, this.left + 119, this.top + 10, this.left + 170, this.top + 80);
    }

    private void paint(GuiGraphicsExtractor graphics) {
        this.blitPanel(graphics);
        int rows = this.discs.size();
        int x = this.left + PROFILE_X;
        int y = this.top + PROFILE_Y;
        for (int row = 0; row < rows; row++) {
            PotteryDisc disc = this.discs.get(row);
            int inner = barWidth(disc.innerRadius());
            int outer = barWidth(disc.radius());
            if (inner < outer) {
                graphics.fill(x + inner, y + rowEdge(row, rows), x + outer, y + rowEdge(row + 1, rows), BAR_COLOR);
            }
        }
        Pottery.Info info = new Pottery(this.discs).classify();
        graphics.text(this.font, info.type().color() + info.type().label(), this.left + 10, this.top + 135, -1);
        if (info.type().isVessel()) {
            graphics.text(this.font, Component.translatable("tooltip.primalpottery.capacity", info.volume()),
                    this.left + 10, this.top + 135 + this.font.lineHeight, -1);
        }
        this.drawHints(graphics);
    }

    private void drawHints(GuiGraphicsExtractor graphics) {
        Component left = Component.translatable("gui.primalpottery.template.left");
        Component right = Component.translatable("gui.primalpottery.template.right");
        int y = this.top + IMAGE_HEIGHT + 4;
        graphics.text(this.font, left, this.left + (IMAGE_WIDTH - this.font.width(left)) / 2, y, 0xFFE0E0E0);
        graphics.text(this.font, right, this.left + (IMAGE_WIDTH - this.font.width(right)) / 2, y + this.font.lineHeight, 0xFFE0E0E0);
    }

    private boolean sculpt(double mouseX, double mouseY, int button) {
        int x = (int) Math.floor(mouseX - this.left - PROFILE_X);
        int y = (int) Math.floor(mouseY - this.top - PROFILE_Y);
        if (x < 0 || x >= PROFILE_WIDTH || y < 0 || y >= PROFILE_HEIGHT || this.discs.isEmpty()
                || (button != InputConstants.MOUSE_BUTTON_LEFT && button != InputConstants.MOUSE_BUTTON_RIGHT)) {
            return false;
        }
        double value = x * Pottery.MAX_RADIUS / (PROFILE_WIDTH - 1);
        int row = y * this.discs.size() / PROFILE_HEIGHT;
        PotteryDisc disc = this.discs.get(row);
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            this.discs.set(row, new PotteryDisc(disc.height(), value, Math.min(value, disc.innerRadius())));
        } else {
            this.discs.set(row, new PotteryDisc(disc.height(), disc.radius(), Math.min(disc.radius(), value)));
        }
        return true;
    }

    private void simplify(int group) {
        for (int index = 0; index + group <= this.discs.size(); index += group) {
            double radius = 0;
            double inner = 0;
            double height = this.discs.get(index).height();
            for (int offset = 0; offset < group; offset++) {
                PotteryDisc disc = this.discs.get(index + offset);
                radius += disc.radius();
                inner += disc.innerRadius();
            }
            radius /= group;
            inner /= group;
            for (int offset = 0; offset < group; offset++) {
                this.discs.set(index + offset, new PotteryDisc(height, radius, inner));
            }
        }
    }

    private void blitPanel(GuiGraphicsExtractor graphics) {
        //? if >=26.1 {
        var texture = Minecraft.getInstance().getTextureManager().getTexture(TEXTURE);
        var sampler = com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().getSampler(
                com.mojang.renderpearl.api.textures.AddressMode.CLAMP_TO_EDGE,
                com.mojang.renderpearl.api.textures.AddressMode.CLAMP_TO_EDGE,
                com.mojang.renderpearl.api.textures.FilterMode.NEAREST,
                com.mojang.renderpearl.api.textures.FilterMode.NEAREST,
                false
        );
        graphics.blit(
                texture.getTextureView(),
                sampler,
                this.left,
                this.top,
                this.left + IMAGE_WIDTH,
                this.top + IMAGE_HEIGHT,
                0.0F,
                IMAGE_WIDTH / 256.0F,
                0.0F,
                IMAGE_HEIGHT / 256.0F
        );
        //?} else {
        /*var texture = Minecraft.getInstance().getTextureManager().getTexture(TEXTURE);
        texture.setFilter(false, false);
        graphics.blit(TEXTURE, this.left, this.top, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        *///?}
    }

    private void layout() {
        this.left = (this.width - IMAGE_WIDTH) / 2;
        this.top = (this.height - IMAGE_HEIGHT) / 2;
    }

    private static int barWidth(double radius) {
        return Math.max(0, Math.min(PROFILE_WIDTH, (int) (radius * PROFILE_WIDTH / Pottery.MAX_RADIUS)));
    }

    private static int rowEdge(int row, int rows) {
        return (row * PROFILE_HEIGHT + rows - 1) / rows;
    }
}
