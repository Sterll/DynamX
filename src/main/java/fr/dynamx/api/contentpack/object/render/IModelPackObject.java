package fr.dynamx.api.contentpack.object.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import javax.annotation.Nullable;

/**
 * An object that can be rendered as an item or in the world.
 *
 * TODO port:1.20.1 - This interface originally extended fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
 *   that interface is in the dxmodel package (already in api/dxmodel). Re-add the extension once we confirm
 *   the package layout. For now we drop the extension to keep the type self-contained.
 */
public interface IModelPackObject {
    /**
     * @return The model location of this object
     */
    @OnlyIn(Dist.CLIENT)
    ResourceLocation getModel();

    /**
     * @return True if this object has a model
     */
    default boolean isModelValid() {
        return getModel() != null && !getModel().getPath().toLowerCase().contains("disable_rendering");
    }

    /**
     * @return True if the model returned by {@link #getModel()} should be loaded by the model registry
     */
    default boolean shouldRegisterModel() {
        return isModelValid() && !getModel().getPath().endsWith("json");
    }

    /**
     * @param viewType The item view type
     * @return The transforms info for the given view type
     *
     * TODO port:1.20.1 - ViewTransformsInfo lives in fr.dynamx.common.contentpack.type (Phase 3b);
     *   typed as Object until that package is ported. Caller of viewType is also typed as Object
     *   because ItemCameraTransforms.TransformType -&gt; ItemDisplayContext in 1.20.1.
     */
    @OnlyIn(Dist.CLIENT)
    default Object getViewTransformsInfo(Object viewType) {
        return null;
    }

    /**
     * @return The default scale applied to the item when getViewTransformsInfo returns null
     */
    @OnlyIn(Dist.CLIENT)
    default float getItemScale() {
        return 1;
    }

    /**
     * @return The 3D render location of this item
     */
    @OnlyIn(Dist.CLIENT)
    default Enum3DRenderLocation get3DItemRenderLocation() {
        return Enum3DRenderLocation.ALL;
    }

    /**
     * @return A text shown on the item in guis
     */
    @Nullable
    @OnlyIn(Dist.CLIENT)
    default String getItemIcon() {
        return null;
    }

    /**
     * Applies item transforms to the model.
     *
     * TODO port:1.20.1 - ItemDxModel is in client/renders/model (Phase 7); ItemCameraTransforms.TransformType
     *   is replaced by ItemDisplayContext. The whole body referenced GlStateManager / FontRenderer which
     *   no longer exist as static APIs in 1.20.1; we now expect the renderer to be re-implemented using
     *   PoseStack / MultiBufferSource in Phase 7. Method body removed for now.
     *
     * @param renderType The render type (first person, third person, ..). Typed as Object pending ItemDisplayContext mapping.
     * @param stack      The stack that is being rendered
     * @param model      The model of the item. Typed as Object pending ItemDxModel port.
     * @param transform  The matrix to apply the transforms to
     */
    @OnlyIn(Dist.CLIENT)
    default void applyItemTransforms(Object renderType, ItemStack stack, Object model, Matrix4f transform) {
        // TODO port:1.20.1 - Reimplement using PoseStack / ItemDisplayContext in Phase 7.
    }

    /**
     * @return The scene graph of this object <br>
     * <strong>Should implement AbstractItemNode if this object has an item</strong>
     *
     * TODO port:1.20.1 - SceneNode lives in fr.dynamx.client.renders.scene.node (Phase 7);
     *   typed as Object until that package is ported.
     */
    Object getSceneGraph();
}
