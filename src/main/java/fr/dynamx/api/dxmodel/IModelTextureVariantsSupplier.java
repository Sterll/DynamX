package fr.dynamx.api.dxmodel;

import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.client.renders.model.texture.TextureVariantData;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Matches a model object with its available textures.
 *
 * @see fr.dynamx.client.renders.model.renderer.ObjModelRenderer
 */
public interface IModelTextureVariantsSupplier extends INamedObject {
    default IModelTextureVariants getMainObjectVariants() {
        return getTextureVariantsFor(null);
    }

    @Nullable
    IModelTextureVariants getTextureVariantsFor(ObjObjectRenderer objObjectRenderer);

    default String getMainObjectVariantName(byte variantId) {
        return getMainObjectVariantNameOrDefault(variantId, "default");
    }

    default String getMainObjectVariantNameOrDefault(byte variantId, String notFoundVariantName) {
        IModelTextureVariants variants = getMainObjectVariants();
        TextureVariantData variant = variants != null ? variants.getVariant(variantId) : null;
        return variant != null ? variant.getName() : notFoundVariantName;
    }

    default boolean hasTextureVariants() {
        return false;
    }

    @Override
    String getPackName();

    default boolean canRenderPart(String partName) {
        return true;
    }

    byte getMaxVariantId();

    interface IModelTextureVariants {
        TextureVariantData getDefaultVariant();

        TextureVariantData getVariant(byte variantId);

        Map<Byte, TextureVariantData> getTextureVariants();
    }
}
