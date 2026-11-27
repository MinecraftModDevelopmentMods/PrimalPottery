package net.modcrafters.minecraft.primalpottery.client;

//? if >=26.1 {
final class PotteryItemRenderer {
    private PotteryItemRenderer() {
    }
}
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import org.joml.Vector3f;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class PotteryItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ItemTransforms BLOCK = new ItemTransforms(
            transform(75, 45, 0, 0, 2.5F, 0, 0.375F, 0.375F, 0.375F),
            transform(75, 45, 0, 0, 2.5F, 0, 0.375F, 0.375F, 0.375F),
            transform(0, 225, 0, 0, 0, 0, 0.40F, 0.40F, 0.40F),
            transform(0, 45, 0, 0, 0, 0, 0.40F, 0.40F, 0.40F),
            ItemTransform.NO_TRANSFORM,
            transform(30, 225, 0, 0, 0, 0, 0.625F, 0.625F, 0.625F),
            transform(0, 0, 0, 0, 3, 0, 0.25F, 0.25F, 0.25F),
            transform(0, 0, 0, 0, 0, 0, 0.5F, 0.5F, 0.5F)
    );

    public PotteryItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(
            ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay
    ) {
        boolean left = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        BLOCK.getTransform(context).apply(left, pose);
        pose.translate(0.0F, -0.5F, 0.0F);
        var buffer = buffers.getBuffer(RenderType.entitySolid(Sprites.blocks()));
        PotteryItemMesh.draw(stack, pose, buffer, light, context == ItemDisplayContext.GUI);
        pose.popPose();
    }

    private static ItemTransform transform(
            float rx, float ry, float rz, float tx, float ty, float tz, float sx, float sy, float sz
    ) {
        return new ItemTransform(
                new Vector3f(rx, ry, rz),
                new Vector3f(tx / 16.0F, ty / 16.0F, tz / 16.0F),
                new Vector3f(sx, sy, sz)
        );
    }
}
*///?}
