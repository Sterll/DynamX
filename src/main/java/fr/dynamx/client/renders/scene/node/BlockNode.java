package fr.dynamx.client.renders.scene.node;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.model.renderer.GltfModelRenderer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.parts.PartStorage;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * A type of root node, corresponding to a block
 *
 * <p>TODO port:1.20.1 - the legacy code looked up the model via
 * {@code DynamXContext.getDxModelRegistry().getModel(packInfo.getModel())}. {@code DynamXContext}
 * isn't ported yet so the model is read directly from the context (set by the BE renderer). Once
 * DynamXContext lands the lookup can be restored here.
 *
 * @param <A> The type of the pack info (the owner of the scene graph)
 */
@RequiredArgsConstructor
public class BlockNode<A extends BlockObject<?>> extends AbstractItemNode<BaseRenderContext.BlockRenderContext, A> {
    /**
     * The children that are linked to the entity (ie that will be rendered with the entity
     * transformations)
     */
    @Getter
    private final List<SceneNode<BaseRenderContext.BlockRenderContext, A>> linkedChildren;

    /**
     * The transformation matrix of the node <br>
     * Stores the transformations of the node, and is used to render the node and its children <br>
     * Do not use GlStateManager to apply transformations, use this matrix instead
     */
    private final Matrix4f transform = new Matrix4f();

    @Override
    public void render(BaseRenderContext.BlockRenderContext context, A packInfo, Matrix4f parentTransform) {
        if (context.getTileEntity() != null && context.getTileEntity().getBlockState().getBlock() instanceof DynamXBlock) {
            transform.identity();
            Vector3fPool.openPool();
            QuaternionPool.openPool();
            GlQuaternionPool.openPool();
            TEDynamXBlock te = context.getTileEntity();
            applyTransform(te, context.getRenderPosition());

            DxModelRenderer model = context.getModel();
            if (model instanceof GltfModelRenderer) {
                // TODO port:1.20.1 - te.getAnimator() / setModelAnimations() depend on DxAnimator
                // which is heavily stubbed in this phase.
                // te.getAnimator().update((GltfModelRenderer) model, context.getPartialTicks());
                // te.getAnimator().setModelAnimations(((GltfModelRenderer) model).animations);
            }
            // Scale of the block object info scale modifier
            transform.scale(DynamXUtils.toVector3f(packInfo.getScaleModifier()));

            PoseStack pose = context.getPoseStack();
            if (pose != null) {
                pose.pushPose();
                pose.mulPoseMatrix(transform);
            }
            if (model != null) {
                model.renderDefaultParts(context.getTextureId(), context.isUseVanillaRender());
            }
            if (pose != null) pose.popPose();

            // Render the linked children
            transform.scale(1 / packInfo.getScaleModifier().x, 1 / packInfo.getScaleModifier().y, 1 / packInfo.getScaleModifier().z);
            linkedChildren.forEach(c -> c.render(context, packInfo, transform));

            GlQuaternionPool.closePool();
            QuaternionPool.closePool();
            Vector3fPool.closePool();
        }
    }

    public void applyTransform(TEDynamXBlock te, Vector3f renderPos) {
        // Translate to block render pos and add the config translate value
        transform.translate((renderPos.x + 0.5f + te.getRelativeTranslation().x),
                (renderPos.y + 0.5f + te.getRelativeTranslation().y),
                (renderPos.z + 0.5f + te.getRelativeTranslation().z));
        // Rotate to the config rotation value
        transform.rotate(DynamXUtils.toQuaternion(te.getCollidableRotation()));
        // Translate of the block object info translation
        if (te.getRelativeScale().x > 0 && te.getRelativeScale().y > 0 && te.getRelativeScale().z > 0) {
            transform.translate(-0.5f, 0.5f, -0.5f);
            // Scale to the config scale value
            transform.scale((te.getRelativeScale().x != 0 ? te.getRelativeScale().x : 1),
                    (te.getRelativeScale().y != 0 ? te.getRelativeScale().y : 1),
                    (te.getRelativeScale().z != 0 ? te.getRelativeScale().z : 1));
            transform.translate(0.5f, 0.5f, 0.5f);
        } else {
            // Backward-compatibility: old blocks were having 0, 0, 0 as default scale
            transform.translate(0, 1, 0);
        }
        transform.translate(DynamXUtils.toVector3f(te.getPackInfo().getTranslation()));
    }

    @Override
    public void renderDebug(BaseRenderContext.BlockRenderContext context, A packInfo) {
        TEDynamXBlock te = context.getTileEntity();
        if (te == null) {
            return;
        }
        PoseStack pose = context.getPoseStack();
        MultiBufferSource buffers = context.getBufferSource();
        if (pose == null || buffers == null) {
            linkedChildren.forEach(c -> c.renderDebug(context, packInfo));
            return;
        }
        transform.identity();
        Vector3fPool.openPool();
        QuaternionPool.openPool();
        GlQuaternionPool.openPool();
        pose.pushPose();
        applyTransform(te, context.getRenderPosition());
        pose.mulPoseMatrix(transform);

        if (DynamXDebugOptions.PLAYER_TO_OBJECT_COLLISION_DEBUG.isActive()) {
            pose.pushPose();
            // PartShape positions already contain the block translation; remove it so the boxes
            // line up with the legacy 1.12 rendering.
            pose.translate(-0.5f - packInfo.getTranslation().x,
                    -1.5f - packInfo.getTranslation().y,
                    -0.5f - packInfo.getTranslation().z);
            for (IShapeInfo partShape : te.getUnrotatedCollisionBoxes()) {
                DynamXRenderUtils.drawBoundingBox(pose, buffers,
                        Vector3fPool.get(
                                partShape.getPosition().x - partShape.getSize().x,
                                partShape.getPosition().y - partShape.getSize().y,
                                partShape.getPosition().z - partShape.getSize().z),
                        Vector3fPool.get(
                                partShape.getPosition().x + partShape.getSize().x,
                                partShape.getPosition().y + partShape.getSize().y,
                                partShape.getPosition().z + partShape.getSize().z),
                        0, 1, 1, 1);
            }
            pose.popPose();
        }
        if (DynamXDebugOptions.SEATS_AND_STORAGE.isActive()) {
            MutableBoundingBox box = new MutableBoundingBox();
            for (PartStorage<?> storage : packInfo.getPartsByType(PartStorage.class)) {
                storage.getBox(box);
                box.offset(storage.getPosition());
                DynamXRenderUtils.drawBoundingBox(pose, buffers,
                        Vector3fPool.get((float) box.minX, (float) box.minY, (float) box.minZ),
                        Vector3fPool.get((float) box.maxX, (float) box.maxY, (float) box.maxZ),
                        1, 0.7f, 0, 1);
            }
        }
        linkedChildren.forEach(c -> c.renderDebug(context, packInfo));
        pose.popPose();
        GlQuaternionPool.closePool();
        QuaternionPool.closePool();
        Vector3fPool.closePool();
    }

    @Override
    public SceneNode<BaseRenderContext.BlockRenderContext, A> getParent() {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }

    @Override
    public void setParent(SceneNode<BaseRenderContext.BlockRenderContext, A> parent) {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }
}
