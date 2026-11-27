package net.modcrafters.minecraft.primalpottery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.modcrafters.minecraft.primalpottery.item.ModItems;
import net.modcrafters.minecraft.primalpottery.item.PotteryItem;
import net.modcrafters.minecraft.primalpottery.pottery.Pottery;
import net.modcrafters.minecraft.primalpottery.pottery.PotteryDisc;
import org.jspecify.annotations.Nullable;

/**
 * The thrown profile, scaled to fill an item slot the way the generic vase does.
 * Mesh units are 32 per block. The pot stays centered, with its base on the ground.
 */
public final class PotteryItemMesh {
    private static final double SLOT_WIDTH = 10.0 / 16.0;
    private static final double SLOT_HEIGHT = 12.5 / 16.0;

    private PotteryItemMesh() {
    }

    public static Prepared prepare(ItemStack stack) {
        boolean raw = stack.is(ModItems.RAW_POTTERY);
        Pottery pottery = PotteryItem.read(stack);
        boolean closed = !raw && PotteryItem.lid(stack);
        return prepare(pottery.discs(), closed, raw, PotteryItem.dye(stack));
    }

    public static Prepared prepare(List<PotteryDisc> discs, boolean closed, boolean raw, @Nullable String dye) {
        Mesh vessel = new Mesh();
        vessel.vessel(0, 0, 0, discs, Mesh.Texture.CLAY);
        double height = 0;
        double radius = 0;
        for (PotteryDisc disc : discs) {
            height += disc.height();
            radius = Math.max(radius, disc.radius());
        }
        Mesh lid = null;
        if (closed) {
            Pottery.Cover cover = new Pottery(discs).cover();
            if (cover != null) {
                lid = new Mesh();
                lid.cover(0, 0, 0, cover, Mesh.Texture.CLAY);
                height += cover.layer() + Pottery.PIXEL * 2;
                radius = Math.max(radius, cover.hole() + Pottery.PIXEL / 2.0);
            }
        }
        vessel.fitSprite();
        if (lid != null) {
            lid.fitSprite();
        }
        Identifier texture = raw && PotteryItem.dyeColor(dye) == null ? Sprites.rawTexture() : Sprites.clayTexture(dye);
        return new Prepared(vessel, lid, scale(radius, height), texture, Sprites.dyeTint(dye));
    }

    /** Mesh units to block units, keeping the profile inside a 10 by 12.5 pixel icon. */
    public static float scale(double radius, double height) {
        double diameter = Math.max(radius * 2.0, 1.0e-3);
        double tall = Math.max(height, 1.0e-3);
        return (float) Math.min(SLOT_WIDTH / diameter, SLOT_HEIGHT / tall);
    }

    public static void draw(ItemStack stack, PoseStack pose, VertexConsumer buffer, int light, boolean shade) {
        prepare(stack).draw(pose, buffer, light, shade);
    }

    public record Prepared(@Nullable Mesh vessel, @Nullable Mesh lid, float scale, Identifier texture, int color) {
        public void draw(PoseStack pose, VertexConsumer buffer, int light, boolean shade) {
            pose.pushPose();
            pose.scale(this.scale, this.scale, this.scale);
            Sprites.draw(this.vessel, this.lid, buffer, pose.last(), light, this.texture, this.color, shade);
            pose.popPose();
        }
    }
}
