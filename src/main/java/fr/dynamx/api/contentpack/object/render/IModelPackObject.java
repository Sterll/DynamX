package fr.dynamx.api.contentpack.object.render;

import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.client.renders.model.ItemDxModel;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.contentpack.type.ViewTransformsInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import javax.annotation.Nullable;

/**
 * An object that can be rendered as an item or in the world.
 */
public interface IModelPackObject extends fr.dynamx.api.contentpack.object.INamedObject, IModelTextureVariantsSupplier {
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
     * @param viewType The item display context (first person, GUI, ...)
     * @return The transforms info for the given view type
     */
    @OnlyIn(Dist.CLIENT)
    default ViewTransformsInfo getViewTransformsInfo(ItemDisplayContext viewType) {
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
     * <p>TODO port:1.20.1 - The full GlStateManager / FontRenderer logic from 1.12 is gone; the
     * renderer now expects PoseStack / MultiBufferSource handling done in
     * {@link fr.dynamx.client.renders.scene.node.AbstractItemNode}.
     *
     * @param renderType The render type (first person, third person, ..)
     * @param stack      The stack that is being rendered
     * @param model      The model of the item
     * @param transform  The matrix to apply the transforms to
     */
    @OnlyIn(Dist.CLIENT)
    default void applyItemTransforms(ItemDisplayContext renderType, ItemStack stack, ItemDxModel model, Matrix4f transform) {
        // TODO port:1.20.1 - Reimplement using PoseStack / ItemDisplayContext in Phase 7.
    }

    /**
     * @return The scene graph of this object <br>
     * <strong>Should implement AbstractItemNode if this object has an item</strong>
     */
    SceneNode<?, ?> getSceneGraph();
}
