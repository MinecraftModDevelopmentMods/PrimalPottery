package net.modcrafters.minecraft.primalpottery.client;

//? if >=26.1 {
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import net.modcrafters.minecraft.primalpottery.item.ModItems;
import net.modcrafters.minecraft.primalpottery.item.PotteryItem;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryDisc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/** Draws the stack's thrown profile. The base model only supplies the block display transform. */
public final class PotterySpecialRenderer implements SpecialModelRenderer<PotterySpecialRenderer.Icon> {
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
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.5F, 0.0F);
        poseStack.scale(prepared.scale(), prepared.scale(), prepared.scale());
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entitySolid(Sprites.blocks()),
                (pose, buffer) -> Sprites.draw(prepared.vessel(), prepared.lid(), buffer, pose, light, prepared.texture(), prepared.color())
        );
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        float side = 0.4F;
        float top = 0.35F;
        output.accept(new Vector3f(-side, -0.5F, -side));
        output.accept(new Vector3f(side, -0.5F, -side));
        output.accept(new Vector3f(-side, -0.5F, side));
        output.accept(new Vector3f(side, -0.5F, side));
        output.accept(new Vector3f(-side, top, -side));
        output.accept(new Vector3f(side, top, -side));
        output.accept(new Vector3f(-side, top, side));
        output.accept(new Vector3f(side, top, side));
    }

    @Override
    public Icon extractArgument(ItemStack stack) {
        boolean raw = stack.is(ModItems.RAW_POTTERY);
        Pottery pottery = PotteryItem.read(stack);
        return new Icon(List.copyOf(pottery.discs()), !raw && PotteryItem.lid(stack), raw, PotteryItem.dye(stack));
    }

    public record Icon(List<PotteryDisc> discs, boolean closed, boolean raw, @Nullable String dye) {
        static Icon fallback() {
            return new Icon(Pottery.display().discs(), false, false, null);
        }
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Icon> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<Icon> bake(SpecialModelRenderer.BakingContext context) {
            return new PotterySpecialRenderer();
        }
    }
}
//?} else {
/*final class PotterySpecialRenderer {
    private PotterySpecialRenderer() {
    }
}
*///?}
