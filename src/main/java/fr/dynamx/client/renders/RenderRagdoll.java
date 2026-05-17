package fr.dynamx.client.renders;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.common.physics.entities.EnumRagdollBodyPart;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.common.physics.utils.SynchronizedRigidBodyTransform;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Renders a ragdoll entity as a player-shaped 6-part dummy. Each body part follows its physics
 * rigid body. The renderer pulls the prev/current rigid-body transforms from the entity and
 * interpolates them by partialTicks before drawing each body part of a vanilla {@link PlayerModel}.
 */
public class RenderRagdoll<T extends RagdollEntity> extends RenderPhysicsEntity<T> {
    protected final BaseRenderContext.EntityRenderContext context = new BaseRenderContext.EntityRenderContext(this);

    private final PlayerModel<?> playerModel;

    public RenderRagdoll(EntityRendererProvider.Context ctx) {
        super(ctx);
        ModelPart root = ctx.bakeLayer(ModelLayers.PLAYER);
        this.playerModel = new PlayerModel<>(root, false);
    }

    @Override
    @Nullable
    public BaseRenderContext.EntityRenderContext getRenderContext(T entity) {
        return context;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        entity.wasRendered = true;
        Map<Byte, SynchronizedRigidBodyTransform> transforms = entity.getTransforms();
        if (transforms == null || transforms.isEmpty()) {
            return;
        }

        ResourceLocation skinLocation = resolveSkin(entity);
        VertexConsumer consumer = bufferSource.getBuffer(playerModel.renderType(skinLocation));

        GlQuaternionPool.openPool();
        QuaternionPool.openPool();
        Vector3fPool.openPool();
        try {
        double interpX = entity.xOld + (entity.getX() - entity.xOld) * partialTicks;
        double interpY = entity.yOld + (entity.getY() - entity.yOld) * partialTicks;
        double interpZ = entity.zOld + (entity.getZ() - entity.zOld) * partialTicks;
        for (EnumRagdollBodyPart partKey : EnumRagdollBodyPart.values()) {
            SynchronizedRigidBodyTransform sync = transforms.get((byte) partKey.ordinal());
            if (sync == null) {
                continue;
            }
            RigidBodyTransform prev = sync.getPrevTransform();
            RigidBodyTransform curr = sync.getTransform();

            Vector3f prevPos = prev.getPosition();
            Vector3f currPos = curr.getPosition();
            // Rigid body world position - interpolated entity world position
            // (the PoseStack is already pre-translated to the interpolated entity position).
            float ix = (float) (lerp(prevPos.x, currPos.x, partialTicks) - interpX);
            float iy = (float) (lerp(prevPos.y, currPos.y, partialTicks) - interpY);
            float iz = (float) (lerp(prevPos.z, currPos.z, partialTicks) - interpZ);

            Quaternionf q = ClientDynamXUtils.computeInterpolatedJomlQuaternion(
                    prev.getRotation(), curr.getRotation(), partialTicks);

            ModelPart part = selectPart(partKey);
            if (part == null) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(ix, iy, iz);
            poseStack.mulPose(q);
            // Legacy 1.12: GlStateManager.rotate(180, 1, 0, 0) — flip on X so the model points
            // right-way-up (PlayerModel is authored upside-down in modelspace).
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));
            // Per-part pivot offsets in world units, taken verbatim from the 1.12 renderer.
            applyPartPivot(poseStack, partKey);
            // PlayerModel cubes are authored in pixel units; convert to world units.
            poseStack.scale(0.0625F, 0.0625F, 0.0625F);
            part.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        } finally {
            Vector3fPool.closePool();
            QuaternionPool.closePool();
            GlQuaternionPool.closePool();
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    @Nullable
    private ModelPart selectPart(EnumRagdollBodyPart key) {
        return switch (key) {
            case HEAD -> playerModel.head;
            case CHEST -> playerModel.body;
            case RIGHT_ARM -> playerModel.rightArm;
            case LEFT_ARM -> playerModel.leftArm;
            case RIGHT_LEG -> playerModel.rightLeg;
            case LEFT_LEG -> playerModel.leftLeg;
        };
    }

    /**
     * Per-part pivot offsets in world units. Values taken verbatim from the 1.12 RenderRagdoll so
     * each body part aligns with its rigid-body center after the 180° X flip.
     */
    private static void applyPartPivot(PoseStack poseStack, EnumRagdollBodyPart key) {
        switch (key) {
            case HEAD -> poseStack.translate(0.0F, 0.25F, 0.0F);
            case CHEST -> poseStack.translate(0.0F, -0.375F, 0.0F);
            case RIGHT_ARM -> poseStack.translate(0.369F, -0.375F, 0.0F);
            case LEFT_ARM -> poseStack.translate(-0.369F, -0.375F, 0.0F);
            case RIGHT_LEG -> poseStack.translate(0.125F, -1.125F, 0.0F);
            case LEFT_LEG -> poseStack.translate(-0.1F, -1.125F, 0.0F);
        }
    }

    private ResourceLocation resolveSkin(T entity) {
        String skin = entity.getSkin();
        if (skin != null && !skin.isEmpty()) {
            try {
                return new ResourceLocation(skin);
            } catch (Exception ignored) {
                // Fall through to default.
            }
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return resolveSkin(entity);
    }

    @Override
    public void renderEntity(T entity, BaseRenderContext.EntityRenderContext context) {
        // Unused — render(...) is overridden directly.
    }

    @Override
    public void renderEntityDebug(T entity, BaseRenderContext.EntityRenderContext context) {
    }
}
