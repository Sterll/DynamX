package fr.dynamx.client.renders;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.dynamx.common.entities.SeatEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for invisible seat entities (only used for debug rendering of their AABB).
 *
 * <p>TODO port:1.20.1 -
 * <ul>
 *   <li>{@code extends Render<SeatEntity>} (constructor took {@code RenderManager}) -&gt;
 *       {@code extends EntityRenderer<SeatEntity>} (takes {@code EntityRendererProvider.Context}).</li>
 *   <li>{@code doRender(...)} -&gt; {@code render(SeatEntity, float, float, PoseStack, MultiBufferSource, int)}.</li>
 *   <li>{@code RenderGlobal.drawBoundingBox(...)} -&gt; {@code LevelRenderer.renderLineBox(PoseStack, VertexConsumer, ...)}.</li>
 *   <li>{@code entity.world.getTileEntity(entity.getPosition())} -&gt;
 *       {@code entity.level().getBlockEntity(entity.blockPosition())}.</li>
 *   <li>{@code GlStateManager.disableLighting / disableDepth / disableTexture2D} - folded into the
 *       {@code RenderType.lines()} which already disables texturing and uses {@code DepthFunc::Always}.</li>
 *   <li>{@code DynamXContext.getCollisionHandler().rotateBB(...)} - DynamXContext isn't ported yet (Phase 5).</li>
 *   <li>{@code ClientDebugSystem.enableDebugDrawing} - handlers/ClientDebugSystem isn't ported yet.</li>
 *   <li>{@code getEntityTexture} -&gt; {@code getTextureLocation}.</li>
 * </ul>
 */
public class RenderSeatEntity extends EntityRenderer<SeatEntity> {
    public RenderSeatEntity(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SeatEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // TODO port:1.20.1 - Was a debug-only render. The 1.12 path:
        //   if (!ClientDebugSystem.enableDebugDrawing || !DynamXDebugOptions.SEATS_AND_STORAGE.isActive()) return;
        //   pushMatrix + translate(x,y,z);
        //   TEDynamXBlock block = (TEDynamXBlock) entity.world.getTileEntity(entity.getPosition());
        //   if (block == null) draw red unit AABB.
        //   else iterate block.getPackInfo().getPartsByType(PartBlockSeat.class), rotate+offset
        //        each PartBlockSeat's getBox(...) by block.getCollidableRotation() and draw it green.
        // Rewrite on top of LevelRenderer.renderLineBox once the dependencies above are ported.
    }

    @Override
    public ResourceLocation getTextureLocation(SeatEntity entity) {
        return null;
    }
}
