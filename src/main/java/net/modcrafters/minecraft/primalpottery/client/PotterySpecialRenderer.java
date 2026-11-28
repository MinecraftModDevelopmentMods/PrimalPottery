package net.modcrafters.minecraft.primalpottery.client;

//? if >=26.1 {
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemStack;
import net.modcrafters.minecraft.primalpottery.item.ModItems;
import net.modcrafters.minecraft.primalpottery.item.PotteryItem;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryDisc;
import net.modcrafters.minecraft.primalpottery.pottery.VesselContents;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/** Draws the stack's thrown profile. The base model only supplies the block display transform. */
public final class PotterySpecialRenderer implements SpecialModelRenderer<PotterySpecialRenderer.Icon> {
    /**
     * The shelf centers the bounds in the opening. This height reaches from the
     * middle of the block down to the plank, so the base rests on the wood.
     */
    private static final float SHELF_HEIGHT = 2.0F;
    /** Extra size on a shelf. The mesh grows from its base, so it stays on the plank. */
    private static final float SHELF_SCALE = 1.5F;
    private final boolean onShelf;

    public PotterySpecialRenderer(boolean onShelf) {
        this.onShelf = onShelf;
    }

    @Override
    public void submit(
            @Nullable Icon icon,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            int overlay,
            boolean foil,
            int outlineColor
    ) {
        Icon drawn = icon == null ? Icon.fallback() : icon;
        PotteryItemMesh.Prepared prepared = PotteryItemMesh.prepare(drawn.discs(), drawn.closed(), drawn.raw(), drawn.dye());
        float scale = prepared.scale() * (this.onShelf ? SHELF_SCALE : 1.0F);
        poseStack.pushPose();
        // Mesh sits on the origin. A block item is centered at 0.5, and the display transform subtracts that.
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.scale(scale, scale, scale);
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entitySolid(Sprites.blocks()),
                (pose, buffer) -> Sprites.draw(prepared.vessel(), prepared.lid(), buffer, pose, light, prepared.texture(), prepared.color())
        );
        drawFluid(drawn, poseStack, collector, light);
        poseStack.popPose();
    }

    /** Lava shares the solid pass. Water and other fluids use the translucent pass. */
    private static void drawFluid(Icon icon, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        if (icon.contents().isEmpty()) {
            return;
        }
        TextureAtlasSprite sprite = Sprites.fluid(icon.contents().fluid());
        if (sprite == null) {
            return;
        }
        Mesh body = new Mesh();
        body.cavity(0, 0, 0, icon.discs(), icon.contents().amount());
        if (body.quads().isEmpty()) {
            return;
        }
        boolean solid = Sprites.fluidSolid(icon.contents().fluid());
        int color = Sprites.fluidColor(icon.contents().fluid());
        collector.submitCustomGeometry(
                poseStack,
                solid ? RenderTypes.entitySolid(Sprites.blocks()) : RenderTypes.entityTranslucent(Sprites.blocks()),
                (pose, buffer) -> Sprites.drawFluid(body, buffer, pose, light, sprite, color)
        );
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        float half = 10.0F / 32.0F;
        float top = this.onShelf ? SHELF_HEIGHT : 12.5F / 16.0F;
        float min = 0.5F - half;
        float max = 0.5F + half;
        output.accept(new Vector3f(min, 0.0F, min));
        output.accept(new Vector3f(max, 0.0F, min));
        output.accept(new Vector3f(min, 0.0F, max));
        output.accept(new Vector3f(max, 0.0F, max));
        output.accept(new Vector3f(min, top, min));
        output.accept(new Vector3f(max, top, min));
        output.accept(new Vector3f(min, top, max));
        output.accept(new Vector3f(max, top, max));
    }

    @Override
    public Icon extractArgument(ItemStack stack) {
        boolean raw = stack.is(ModItems.RAW_POTTERY);
        Pottery pottery = PotteryItem.read(stack);
        VesselContents contents = raw ? VesselContents.EMPTY : PotteryItem.contents(stack);
        return new Icon(List.copyOf(pottery.discs()), !raw && PotteryItem.lid(stack), raw, PotteryItem.dye(stack), contents);
    }

    public record Icon(List<PotteryDisc> discs, boolean closed, boolean raw, @Nullable String dye, VesselContents contents) {
        static Icon fallback() {
            return new Icon(Pottery.display().discs(), false, false, null, VesselContents.EMPTY);
        }
    }

    public record Unbaked(boolean onShelf) implements SpecialModelRenderer.Unbaked<Icon> {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("on_shelf", false).forGetter(Unbaked::onShelf)
        ).apply(instance, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<Icon> bake(SpecialModelRenderer.BakingContext context) {
            return new PotterySpecialRenderer(this.onShelf);
        }
    }
}
//?} else {
/*final class PotterySpecialRenderer {
    private PotterySpecialRenderer() {
    }
}
*///?}
