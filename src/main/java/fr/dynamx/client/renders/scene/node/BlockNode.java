package fr.dynamx.client.renders.scene.node;

import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.model.renderer.GltfModelRenderer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * A type of root node, corresponding to a block
 *
 * <p>TODO port:1.20.1 -
 * <ul>
 *   <li>{@code GlStateManager.pushMatrix / multMatrix(ClientDynamXUtils.getMatrixBuffer(transform)) / popMatrix}
 *       must be replaced by {@code PoseStack#pushPose / mulPoseMatrix(transform) / popPose} once
 *       a PoseStack is threaded through {@link BaseRenderContext.BlockRenderContext}.</li>
 *   <li>{@code DynamXContext.getDxModelRegistry().getModel(...)} for the DxModelPath -
 *       {@code DynamXContext} isn't ported yet; we resolve the model via the tile entity field.</li>
 *   <li>Debug rendering used {@code RenderGlobal.drawBoundingBox(...)} which is now
 *       {@code LevelRenderer.renderLineBox(PoseStack, VertexConsumer, ...)}.</li>
 *   <li>{@code packInfo.getPartsByType(PartStorage.class)} stays once {@code PartStorage} is ported
 *       (Phase 6); typed as raw to keep this layer compiling.</li>
 * </ul>
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
        if (context.getTileEntity() != null && context.getTileEntity().getBlockType() instanceof DynamXBlock) {
            transform.identity();
            Vector3fPool.openPool();
            QuaternionPool.openPool();
            GlQuaternionPool.openPool();
            TEDynamXBlock te = context.getTileEntity();
            applyTransform(te, context.getRenderPosition());

            // Rendering the model
            // TODO port:1.20.1 - was:
            //   DxModelRenderer model = DynamXContext.getDxModelRegistry().getModel(te.getPackInfo().getModel());
            // DynamXContext isn't ported yet; the model is fetched directly via the model registry stub
            // (resolved by the GLTF render pipeline later in this phase).
            DxModelRenderer model = context.getModel();
            if (model instanceof GltfModelRenderer) {
                // TODO port:1.20.1 - te.getAnimator() / setModelAnimations() typed as Object because
                // DxAnimator is heavily stubbed in this phase.
                // te.getAnimator().update((GltfModelRenderer) model, context.getPartialTicks());
                // te.getAnimator().setModelAnimations(((GltfModelRenderer) model).animations);
            }
            // Scale of the block object info scale modifier
            transform.scale(DynamXUtils.toVector3f(packInfo.getScaleModifier()));
            // TODO port:1.20.1 - was:
            //   GlStateManager.pushMatrix();
            //   GlStateManager.multMatrix(ClientDynamXUtils.getMatrixBuffer(transform));
            //   model.renderDefaultParts(context.getTextureId(), context.isUseVanillaRender());
            //   GlStateManager.popMatrix();
            if (model != null) {
                model.renderDefaultParts(context.getTextureId(), context.isUseVanillaRender());
            }
            // Render the linked children
            transform.scale(1 / packInfo.getScaleModifier().x, 1 / packInfo.getScaleModifier().y, 1 / packInfo.getScaleModifier().z);
            linkedChildren.forEach(c -> c.render(context, packInfo, transform));

            GlQuaternionPool.closePool();
            QuaternionPool.closePool();
            Vector3fPool.closePool();
            // TODO port:1.20.1 - DynamXRenderUtils.popGlAllAttribBits() removed in core profile
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
        // TODO port:1.20.1 - debug bounding-box rendering used GlStateManager + RenderGlobal.drawBoundingBox.
        // Rewrite on top of PoseStack + LevelRenderer.renderLineBox(...) once PoseStack/MultiBufferSource
        // are added to the render context. Storage/seat debug boxes (PartStorage) also need the same.
        linkedChildren.forEach(c -> c.renderDebug(context, packInfo));
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
