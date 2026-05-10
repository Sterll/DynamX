package fr.dynamx.client.renders.scene.node;

import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.events.client.DynamXRenderItemEvent;
import fr.dynamx.client.renders.model.ItemDxModel;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.IRenderContext;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.SubClassPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * A {@link SceneNode} that can be rendered as an item, with an
 * {@link fr.dynamx.client.renders.scene.BaseRenderContext.ItemRenderContext} <br>
 * The item render method can be customized by overriding
 * {@link #renderItemModel(BaseRenderContext.ItemRenderContext, IModelPackObject, Matrix4f)}
 *
 * <p>TODO port:1.20.1 -
 * <ul>
 *   <li>{@code ItemCameraTransforms.TransformType} -> {@link ItemDisplayContext}.</li>
 *   <li>{@code Minecraft.getMinecraft().getRenderItem().renderItem(stack, model.getGuiBaked())}
 *       must be replaced by {@code Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GUI, packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, level, 0)}.</li>
 *   <li>{@code MinecraftForge.EVENT_BUS.post(...)} -> {@code MinecraftForge.EVENT_BUS.post(...)}.</li>
 *   <li>{@code GlStateManager.pushMatrix()/translate/multMatrix/popMatrix} -> {@code PoseStack#pushPose / translate / mulPoseMatrix / popPose}.</li>
 *   <li>{@code ClientDynamXUtils.getMatrixBuffer(transform)} is no longer needed - JOML matrices are
 *       fed directly to {@code PoseStack}/render systems.</li>
 *   <li>{@code packInfo.getViewTransformsInfo / applyItemTransforms / getItemScale} stays as-is on
 *       the pack-info side (Phase 6/8 ports those modelpack types).</li>
 * </ul>
 *
 * @param <C> The "base" type of the render context (when the node isn't rendered as an item)
 * @param <A> The type of the pack info (the owner of the scene graph)
 */
public abstract class AbstractItemNode<C extends IRenderContext, A extends IModelPackObject> implements SceneNode<C, A> {
    /**
     * The transformation matrix of this item node <br>
     * Stores the transformations of the item node, and is used to render the node and its children <br>
     * Do not use GlStateManager to apply transformations, use this matrix instead
     */
    private final Matrix4f transform = new Matrix4f();

    /**
     * Renders this node as an item with an
     * {@link fr.dynamx.client.renders.scene.BaseRenderContext.ItemRenderContext} <br>
     * You normally don't need to override this method,
     * {@link #renderItemModel(BaseRenderContext.ItemRenderContext, IModelPackObject, Matrix4f)} is
     * here for that
     *
     * @param context  The context of the render call
     * @param packInfo The pack info of the scene graph
     */
    public void renderAsItemNode(BaseRenderContext.ItemRenderContext context, A packInfo) {
        ItemStack stack = context.getStack();
        ItemDxModel model = context.getItemModel();
        ItemDisplayContext renderType = context.getRenderType();
        if (packInfo.get3DItemRenderLocation() == Enum3DRenderLocation.NONE
                || (renderType == ItemDisplayContext.GUI && packInfo.get3DItemRenderLocation() == Enum3DRenderLocation.WORLD)) {
            // TODO port:1.20.1 - GUI fallback: must call
            //   Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GUI,
            //       packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, level, 0);
            // The 1.12 path was:
            //   GlStateManager.pushMatrix();
            //   GlStateManager.translate(0.5F, 0.5F, 0.5F);
            //   Minecraft.getMinecraft().getRenderItem().renderItem(stack, model.getGuiBaked());
            //   GlStateManager.popMatrix();
        } else {
            transform.identity();
            Vector3fPool.openPool(SubClassPool.ITEM_RENDER_NODE);
            QuaternionPool.openPool(SubClassPool.ITEM_RENDER_NODE);
            GlQuaternionPool.openPool(SubClassPool.ITEM_RENDER_NODE);
            // TODO port:1.20.1 - was MinecraftForge.EVENT_BUS.post(...) ; use MinecraftForge.EVENT_BUS.post
            DynamXRenderItemEvent transformEvent = new DynamXRenderItemEvent(context, this, DynamXRenderItemEvent.EventStage.TRANSFORM);
            // if (!MinecraftForge.EVENT_BUS.post(transformEvent).isCanceled()) {
            //     packInfo.applyItemTransforms(renderType, stack, model, transform);
            //     ViewTransformsInfo transformsInfo = packInfo.getViewTransformsInfo(renderType);
            //     if (transformsInfo != null) {
            //         transform.mul(transformsInfo.getTransformMatrix());
            //     } else {
            //         float scale = packInfo.getItemScale();
            //         transform.scale(scale, scale, scale);
            //     }
            // }
            DynamXRenderItemEvent renderEvent = new DynamXRenderItemEvent(context, this, DynamXRenderItemEvent.EventStage.RENDER);
            // if (!MinecraftForge.EVENT_BUS.post(renderEvent).isCanceled()) {
            renderItemModel(context, packInfo, transform);
            // }
            GlQuaternionPool.closePool();
            QuaternionPool.closePool();
            Vector3fPool.closePool();
            // TODO port:1.20.1 - DynamXRenderUtils.popGlAllAttribBits() removed in core profile
        }
    }

    /**
     * Renders the item model with the given context, pack info and transformation matrix <br>
     * Fired by {@link #renderAsItemNode(BaseRenderContext.ItemRenderContext, IModelPackObject)} <br>
     * This method doesn't render the linked children by default, you have to do it manually <br>
     * <strong>Use the matrix to apply your transforms, NOT open gl</strong>
     *
     * @param context   The context of the render call
     * @param packInfo  The pack info of the scene graph
     * @param transform The transformation matrix
     */
    public void renderItemModel(BaseRenderContext.ItemRenderContext context, A packInfo, Matrix4f transform) {
        // TODO port:1.20.1 - was:
        //   GlStateManager.pushMatrix();
        //   GlStateManager.multMatrix(ClientDynamXUtils.getMatrixBuffer(transform));
        //   context.getModel().renderModel(context.getTextureId(), context.getRenderType() == ItemCameraTransforms.TransformType.GUI);
        //   GlStateManager.popMatrix();
        // Re-author on top of PoseStack#mulPoseMatrix + GLTF render pipeline.
        if (context.getModel() != null) {
            context.getModel().renderModel(context.getTextureId(),
                    context.getRenderType() == ItemDisplayContext.GUI);
        }
    }
}
