package fr.dynamx.client.renders;

import com.jme3.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.parts.PartBlockSeat;
import fr.dynamx.common.entities.SeatEntity;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * Renderer pour les SeatEntity (entites invisibles n'apparaissant qu'en debug).
 * Dessine l'AABB de chaque PartBlockSeat porte par le TEDynamXBlock co-localise.
 *
 * <p>Pipeline 1.20.1 : RenderType.lines() encapsule la desactivation texture/lighting et
 * LevelRenderer.renderLineBox gere l'emission via PoseStack + VertexConsumer. Plus aucun
 * GlStateManager.
 */
public class RenderSeatEntity extends EntityRenderer<SeatEntity> {
    public RenderSeatEntity(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void render(SeatEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!ClientDebugSystem.enableDebugDrawing || !DynamXDebugOptions.SEATS_AND_STORAGE.isActive()) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        poseStack.pushPose();

        BlockEntity blockEntity = entity.level().getBlockEntity(entity.blockPosition());
        if (!(blockEntity instanceof TEDynamXBlock block) || block.getPackInfo() == null) {
            // Pas de pack info : AABB rouge unitaire pour signaler le siege.
            LevelRenderer.renderLineBox(poseStack, consumer,
                    -0.5, -0.5, -0.5, 0.5, 0.5, 0.5,
                    1.0f, 0.0f, 0.0f, 1.0f);
            poseStack.popPose();
            return;
        }

        Vector3fPool.openPool();
        QuaternionPool.openPool();
        try {
            MutableBoundingBox out = new MutableBoundingBox();
            List<PartBlockSeat> seats = block.getPackInfo().getPartsByType(PartBlockSeat.class);
            for (PartBlockSeat seat : seats) {
                seat.getBox(out);
                MutableBoundingBox rotated = DynamXContext.getCollisionHandler()
                        .rotateBB(Vector3fPool.get(), out, block.getCollidableRotation());
                Vector3f partPos = DynamXGeometry.rotateVectorByQuaternion(seat.getPosition(), block.getCollidableRotation());
                partPos.addLocal(block.getRelativeTranslation());
                rotated.offset(partPos);

                LevelRenderer.renderLineBox(poseStack, consumer,
                        rotated.minX, rotated.minY, rotated.minZ,
                        rotated.maxX, rotated.maxY, rotated.maxZ,
                        0.0f, 1.0f, 0.0f, 1.0f);
            }
        } finally {
            QuaternionPool.closePool();
            Vector3fPool.closePool();
        }

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SeatEntity entity) {
        return null;
    }
}
