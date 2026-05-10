package fr.dynamx.common.contentpack.type;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO port:1.20.1 - Original implemented IModelTextureVariantsSupplier.IModelTextureVariants
 *   (fr.dynamx.api.dxmodel) and stored TextureVariantData (fr.dynamx.client.renders.model.texture)
 *   values. Both live in not-yet-ported packages (Phase 7). The class now uses Object placeholders;
 *   the texture variants map is keyed by Byte and stores Object values until those types are ported.
 */
@RegisteredSubInfoType(name = "MaterialVariants", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.WHEELS, SubInfoTypeRegistries.ARMORS, SubInfoTypeRegistries.BLOCKS,
        SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.PROPS})
public class MaterialVariantsInfo<T extends ISubInfoTypeOwner<T>> extends SubInfoType<T> {
    @Setter
    @Getter
    @PackFileProperty(configNames = "BaseMaterial", required = false, defaultValue = "Primary material configured in the model")
    private String baseMaterial = "Default";
    @PackFileProperty(configNames = "Variants", defaultValue = "\"DynamX1 DynamX2\"")
    private String[] texturesArray;
    @Getter
    private final Map<Byte, Object> variantsMap = new HashMap<>();

    public MaterialVariantsInfo(ISubInfoTypeOwner<T> owner) {
        super(owner);
    }

    /**
     * Backward compatibility with 3.3.0
     */
    @Deprecated
    public MaterialVariantsInfo(ISubInfoTypeOwner<T> owner, String[][] texturesArray) {
        super(owner);
        this.texturesArray = new String[texturesArray.length];
        for (int i = 0; i < texturesArray.length; i++) {
            String[] info = texturesArray[i];
            this.texturesArray[i] = info[0];
        }
    }

    @Override
    public String getName() {
        return "MaterialVariantsInfo";
    }

    @Override
    public void appendTo(T owner) {
        // TODO port:1.20.1 - Re-introduce TextureVariantData once Phase 7 is ported.
        //   Original logic stored TextureVariantData(baseMaterial, (byte) 0) at id 0 and
        //   TextureVariantData(info, id) at incremental ids.
        variantsMap.put((byte) 0, baseMaterial);
        byte id = 1;
        if (texturesArray != null) {
            for (String info : texturesArray) {
                variantsMap.put(id, info);
                id++;
            }
        }
        owner.addSubProperty(this);
    }

    /**
     * @return The default texture variant. TODO port:1.20.1 - typed as Object pending TextureVariantData port.
     */
    public Object getDefaultVariant() {
        return variantsMap.get((byte) 0);
    }

    /**
     * @return The texture variant with the given id, or the default one. TODO port:1.20.1 - typed as Object.
     */
    public Object getVariant(byte variantId) {
        return variantsMap.getOrDefault(variantId, getDefaultVariant());
    }

    /**
     * @return The texture variants map. TODO port:1.20.1 - values typed as Object.
     */
    public Map<Byte, Object> getTextureVariants() {
        return variantsMap;
    }

    /**
     * Adds a variant.
     *
     * TODO port:1.20.1 - variantData was TextureVariantData; relaxed to Object.
     *   Original used variantData.getId() to key the map. Caller must currently provide id.
     */
    public void addVariant(Object variantData, boolean allowOverride) {
        // TODO port:1.20.1 - Once TextureVariantData is ported, derive the id from variantData.getId().
        //   For now this stub keeps the API surface but cannot derive an id.
    }

    public boolean hasVariant(byte variantId) {
        return variantsMap.containsKey(variantId);
    }
}
