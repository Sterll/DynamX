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

        Vector3f entityPos = entity.physicsPosition;
        for (EnumRagdollBodyPart partKey : EnumRagdollBodyPart.values()) {
            SynchronizedRigidBodyTransform sync = transforms.get((byte) partKey.ordinal());
            if (sync == null) {
                continue;
            }
            RigidBodyTransform prev = sync.getPrevTransform();
            RigidBodyTransform curr = sync.getTransform();

            Vector3f prevPos = prev.getPosition();
            Vector3f currPos = curr.getPosition();
            float ix = lerp(prevPos.x, currPos.x, partialTicks) - entityPos.x;
            float iy = lerp(prevPos.y, currPos.y, partialTicks) - entityPos.y;
            float iz = lerp(prevPos.z, currPos.z, partialTicks) - entityPos.z;

            Quaternionf q = ClientDynamXUtils.computeInterpolatedJomlQuaternion(
                    prev.getRotation(), curr.getRotation(), partialTicks);

            ModelPart part = selectPart(partKey);
            if (part == null) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(ix, iy, iz);
            poseStack.mulPose(q);
            // The body parts are authored at scale 0.0625 (one pixel = 1/16 block) and offset so
            // that the model's pivot is at the joint. Flip Y to match vanilla model orientation.
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            applyPartPivot(poseStack, partKey);

            ModelPart.Cube[] backup = null; // no-op placeholder for future per-bodypart tinting
            part.xRot = 0;
            part.yRot = 0;
            part.zRot = 0;
            part.x = 0;
            part.y = 0;
            part.z = 0;
            part.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
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
     * Vanilla player model parts are pivoted at the joint (top of arm, top of leg, etc.). Each
     * body part's physics transform is centered on its rigid body, so we shift the pose so the
     * model part's joint aligns with the centre of the box.
     */
    private static void applyPartPivot(PoseStack poseStack, EnumRagdollBodyPart key) {
        switch (key) {
            case HEAD -> poseStack.translate(0.0F, 0.25F, 0.0F);
            case CHEST -> poseStack.translate(0.0F, 0.375F, 0.0F);
            case RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG -> poseStack.translate(0.0F, 0.375F, 0.0F);
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
