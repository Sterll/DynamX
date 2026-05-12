package fr.dynamx.client.renders.model.renderer;

import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A render-able OBJ model.
 * <p>
 * TODO port:1.20.1 - OBJ loader is being dropped. This class is kept as a stub to preserve
 * external references; the GLTF pipeline ({@link GltfModelRenderer}) is now the sole supported
 * format and should be used instead. All real rendering logic has been removed.
 */
public class ObjModelRenderer extends DxModelRenderer {

    @Getter
    private final List<ObjObjectRenderer> objObjects;
    @Getter
    private final Map<String, Object> materials; // TODO port:1.20.1 - was Map<String, Material>; OBJ loader dropped
    public boolean hasNoneMaterials;

    protected ObjModelRenderer(DxModelPath location, List<ObjObjectRenderer> objObjects, Map<String, Object> materials, @Nullable IModelTextureVariantsSupplier textureVariants) {
        super(location, textureVariants);
        this.objObjects = objObjects;
        this.materials = materials;
        this.hasNoneMaterials = false;
    }

    public static ObjModelRenderer loadObjModel(DxModelPath objModelPath, @Nullable IModelTextureVariantsSupplier textureVariants) {
        // TODO port:1.20.1 - OBJ loader is being dropped
        return null;
    }

    @Override
    public void uploadVAOs() {
        // TODO port:1.20.1 - OBJ loader is being dropped
    }

    @Override
    public void clearVAOs() {
        // TODO port:1.20.1 - OBJ loader is being dropped
    }

    public void renderGroup(ObjObjectRenderer obj, byte textureDataId, boolean forceVanillaRender) {
        // TODO port:1.20.1 - OBJ loader is being dropped
    }

    @Override
    public boolean renderGroup(String group, byte textureDataId, boolean forceVanillaRender) {
        // TODO port:1.20.1 - OBJ loader is being dropped
        return false;
    }

    @Override
    public boolean renderDefaultParts(byte textureDataId, boolean forceVanillaRender) {
        // TODO port:1.20.1 - OBJ loader is being dropped
        return false;
    }

    @Override
    public void renderModel(byte textureDataId, boolean forceVanillaRender) {
        // TODO port:1.20.1 - OBJ loader is being dropped
    }

    public ObjObjectRenderer getObjObjectRenderer(String groupName) {
        return objObjects == null ? null
                : objObjects.stream()
                .filter(o -> groupName.equalsIgnoreCase(String.valueOf(o.getObjObjectData())))
                .findFirst().orElse(null);
    }

    @Override
    public boolean containsObjectOrNode(String name) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return objObjects == null || objObjects.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> emptyMaterials() {
        return Collections.emptyMap();
    }
}
