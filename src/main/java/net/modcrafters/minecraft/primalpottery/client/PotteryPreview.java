package net.modcrafters.minecraft.primalpottery.client;

import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryDisc;
//? if <26.1 {
/*import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
*///?}
//? if >=26.1 {
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.jspecify.annotations.Nullable;
//?}

/** Draws the spinning clay inside the template screen's preview recess. */
public final class PotteryPreview {
    private static final float TILT = 20;
    private static final float MAX_SCALE = 4;

    private PotteryPreview() {
    }

    static void draw(GuiGraphicsExtractor graphics, List<PotteryDisc> discs, int x0, int y0, int x1, int y1) {
        Model model = model(discs, x1 - x0, y1 - y0);
        if (model.mesh().quads().isEmpty()) {
            return;
        }
        float spin = (System.currentTimeMillis() / 20) % 360;
        //? if >=26.1 {
        SINK.submit(graphics, new State(model, spin, x0, y0, x1, y1));
        //?} else {
        /*PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate((x0 + x1) / 2.0F, (y0 + y1) / 2.0F, 100);
        pose.scale(model.scale(), -model.scale(), model.scale());
        orient(pose, model, spin);
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        RenderType type = RenderType.entitySolid(Sprites.blocks());
        Sprites.draw(model.mesh(), buffers.getBuffer(type), pose.last(), 0xF000F0);
        buffers.endBatch(type);
        pose.popPose();
        *///?}
    }

    static Model model(List<PotteryDisc> discs, int width, int height) {
        Mesh mesh = new Mesh();
        mesh.vessel(0, 0, 0, discs, Mesh.Texture.CLAY);
        mesh.fitSprite();
        double vesselHeight = 0;
        double radius = 0;
        for (PotteryDisc disc : discs) {
            vesselHeight += disc.height();
            radius = Math.max(radius, disc.radius());
        }
        // Leave a two-pixel margin on each side at every spin angle.
        double tilt = Math.toRadians(TILT);
        double projectedHeight = vesselHeight * Math.cos(tilt) + 2 * radius * Math.sin(tilt);
        double fit = Math.min((width - 4) / (2 * radius), (height - 4) / projectedHeight);
        return new Model(mesh, (float) (vesselHeight / 2), (float) Math.min(MAX_SCALE, fit));
    }

    static void orient(PoseStack pose, Model model, float spin) {
        //? if >=26.1 {
        // Picture-in-picture flips Z. Turn the Y-up mesh into GUI space with an X rotation.
        pose.rotateDegrees(Axis.XP, 180 + TILT);
        pose.rotateDegrees(Axis.YP, -spin);
        //?} else {
        /*pose.mulPose(Axis.XP.rotationDegrees(TILT));
        pose.mulPose(Axis.YP.rotationDegrees(-spin));
        *///?}
        pose.translate(0, -model.centerY(), 0);
    }

    record Model(Mesh mesh, float centerY, float scale) {
    }

    //? if >=26.1 {
    public static Sink SINK = (graphics, state) -> {
    };

    @FunctionalInterface
    public interface Sink {
        void submit(GuiGraphicsExtractor graphics, State state);
    }

    public record State(
            Model model,
            float spin,
            int x0,
            int y0,
            int x1,
            int y1,
            float scale,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds
    ) implements PictureInPictureRenderState {
        private State(Model model, float spin, int x0, int y0, int x1, int y1) {
            this(
                    model,
                    spin,
                    x0,
                    y0,
                    x1,
                    y1,
                    model.scale(),
                    null,
                    PictureInPictureRenderState.getBounds(x0, y0, x1, y1, null)
            );
        }
    }

    public static final class Renderer extends PictureInPictureRenderer<State> {
        @Override
        public Class<State> getRenderStateClass() {
            return State.class;
        }

        @Override
        protected void renderToTexture(State state, PoseStack pose, SubmitNodeCollector collector) {
            Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
            orient(pose, state.model(), state.spin());
            collector.submitCustomGeometry(
                    pose,
                    RenderTypes.entitySolid(Sprites.blocks()),
                    (entry, buffer) -> Sprites.draw(state.model().mesh(), buffer, entry, 0xF000F0)
            );
        }

        @Override
        protected float getTranslateY(int height, int guiScale) {
            return height / 2.0F;
        }

        @Override
        protected String getTextureLabel() {
            return "pottery template";
        }
    }
    //?}
}
