package fr.dynamx.client.renders.scene.node;

import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.SubClassPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.joml.Matrix4f;

import java.util.List;

/**
 * A type of root node, corresponding to an entity
 *
 * <p>TODO port:1.20.1 - the static {@code context} fallback used by {@link #renderItemModel} relied
 * on {@code DynamXRenderUtils.getRenderBaseVehicle()} which isn't ported yet. The context is built
 * with a {@code null} renderer; wire the real one in once that helper lands.
 *
 * @param <A> The type of the pack info (the owner of the scene graph)
 */
@RequiredArgsConstructor
public class EntityNode<A extends IPhysicsPackInfo> extends AbstractItemNode<BaseRenderContext.EntityRenderContext, A> {
    // RenderBaseVehicle isn't statically reachable in Phase 7; lazily attach a render later.
    private static final BaseRenderContext.EntityRenderContext context = new BaseRenderContext.EntityRenderContext(null);

    /**
     * The children that are linked to the entity (ie that will be rendered with the entity
     * transformations)
     */

    @Getter
    private final List<SceneNode<BaseRenderContext.EntityRenderContext, A>> linkedChildren;
    /**
     * The children that are not linked to the entity (ie that will be rendered with the world
     * transformations)
     */
    @Getter
    private final List<SceneNode<BaseRenderContext.EntityRenderContext, A>> unlinkedChildren;

    /**
     * The transformation matrix of the node <br>
     * Stores the transformations of the node, and is used to render the node and its children <br>
     * Do not use GlStateManager to apply transformations, use this matrix instead
     */
    private final Matrix4f transform = new Matrix4f();

    @Override
    public void render(BaseRenderContext.EntityRenderContext context, A packInfo, Matrix4f parentTransform) {
        renderWithTransform(context, packInfo, transform.identity());
    }

    /**
     * Implementation of the render method, to allow the use of a modified transform matrix
     */
    protected void renderWithTransform(BaseRenderContext.EntityRenderContext context, A packInfo, Matrix4f transform) {
        Vector3fPool.openPool(SubClassPool.ENTITY_RENDER_NODE);
        QuaternionPool.openPool(SubClassPool.ENTITY_RENDER_NODE);
        GlQuaternionPool.openPool(SubClassPool.ENTITY_RENDER_NODE);

        ModularPhysicsEntity<?> entity = context.getEntity();
        org.joml.Quaternionf entityRotation = null;
        if (entity != null) {
            transform.translate(context.getRenderPosition());
            entityRotation = ClientDynamXUtils.computeInterpolatedJomlQuaternion(
                    entity.prevRenderRotation, entity.renderRotation, context.getPartialTicks());
            transform.rotate(entityRotation);
        }
        // Scale to the config scale value
        transform.scale(DynamXUtils.toVector3f(packInfo.getScaleModifier()));

        com.mojang.blaze3d.vertex.PoseStack pose = context.getPoseStack();
        if (pose != null) {
            pose.pushPose();
            // PoseStack.mulPoseMatrix(Matrix4f) only multiplies the pose matrix - the normal
            // matrix is left untouched, so per-vertex lighting is computed against the world's
            // normal frame instead of the entity's. That produces the "shattered facets" look.
            // Apply translate / mulPose(Quat) / scale individually so the normal matrix tracks
            // the rotation and scale correctly.
            org.joml.Vector3f renderPos = context.getRenderPosition();
            if (renderPos != null) {
                pose.translate(renderPos.x, renderPos.y, renderPos.z);
            }
            if (entityRotation != null) {
                pose.mulPose(entityRotation);
            }
            org.joml.Vector3f scale = DynamXUtils.toVector3f(packInfo.getScaleModifier());
            if (scale != null && (scale.x != 1f || scale.y != 1f || scale.z != 1f)) {
                pose.scale(scale.x, scale.y, scale.z);
            }
        }
        if (context.getRender() != null) {
            context.getRender().renderMainModel(context.getModel(), entity, context.getTextureId(), context.isUseVanillaRender());
        } else if (context.getModel() != null) {
            context.getModel().renderModel(context.getTextureId(), context.isUseVanillaRender());
        }
        transform.scale(1 / packInfo.getScaleModifier().x, 1 / packInfo.getScaleModifier().y, 1 / packInfo.getScaleModifier().z);

        // Render the linked children. They assume the PoseStack still carries the entity
        // translation/rotation/scale - popping before rendering them would drop every part
        // (wheels, doors, lights, ...) to world-origin with the wrong orientation, which
        // produces the "shattered geometry" look.
        linkedChildren.forEach(c -> c.render(context, packInfo, transform));

        // Render the unlinked children, if this is a static scene graph (not in the world)
        if (entity == null) {
            unlinkedChildren.forEach(c -> c.render(context, packInfo, transform));
        }
        // Render the unlinked children, if any
        if (entity != null && !unlinkedChildren.isEmpty()) {
            float interpX = (float) (entity.xOld + (entity.getX() - entity.xOld) * context.getPartialTicks());
            float interpY = (float) (entity.yOld + (entity.getY() - entity.yOld) * context.getPartialTicks());
            float interpZ = (float) (entity.zOld + (entity.getZ() - entity.zOld) * context.getPartialTicks());
            transform.translate(context.getRenderPosition().x - interpX,
                    context.getRenderPosition().y - interpY,
                    context.getRenderPosition().z - interpZ);
            unlinkedChildren.forEach(c -> c.render(context, packInfo, transform));
        }
        if (pose != null) pose.popPose();

        GlQuaternionPool.closePool();
        QuaternionPool.closePool();
        Vector3fPool.closePool();
    }

    @Override
    public void renderDebug(BaseRenderContext.EntityRenderContext context, A packInfo) {
        linkedChildren.forEach(c -> c.renderDebug(context, packInfo));
        unlinkedChildren.forEach(c -> c.renderDebug(context, packInfo));
    }

    @Override
    public SceneNode<BaseRenderContext.EntityRenderContext, A> getParent() {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }

    @Override
    public void setParent(SceneNode<BaseRenderContext.EntityRenderContext, A> parent) {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }

    @Override
    public void renderItemModel(BaseRenderContext.ItemRenderContext context, A packInfo, Matrix4f transform) {
        BaseRenderContext.EntityRenderContext entCtx = EntityNode.context
                .setRenderParams(0, 0, 0, context.getPartialTicks(), context.isUseVanillaRender());
        entCtx.setModelParams(null, context.getModel(), context.getTextureId());
        renderWithTransform(entCtx, packInfo, transform);
    }
}
