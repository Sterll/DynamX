package fr.dynamx.client.renders.model.renderer;

import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.objloader.data.Material;
import fr.dynamx.common.objloader.data.ObjModelData;
import fr.dynamx.common.objloader.data.ObjObjectData;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A render-able OBJ model. Wraps {@link ObjModelData} and a list of {@link ObjObjectRenderer}s
 * that push their triangles to the active {@link com.mojang.blaze3d.vertex.VertexConsumer}.
 */
public class ObjModelRenderer extends DxModelRenderer {

    @Getter
    private final List<ObjObjectRenderer> objObjects;
    @Getter
    private final Map<String, Material> materials;
    public boolean hasNoneMaterials;

    protected ObjModelRenderer(DxModelPath location, List<ObjObjectRenderer> objObjects, Map<String, Material> materials, @Nullable IModelTextureVariantsSupplier textureVariants) {
        super(location, textureVariants);
        this.objObjects = objObjects;
        this.materials = materials;
        this.hasNoneMaterials = materials == null || materials.isEmpty();
    }

    public static ObjModelRenderer loadObjModel(DxModelPath objModelPath, @Nullable IModelTextureVariantsSupplier textureVariants) {
        try {
            List<ObjObjectRenderer> objObjects = new ArrayList<>();
            ObjModelData objModelData = (ObjModelData) DynamXContext.getDxModelDataFromCache(objModelPath);
            if (objModelData == null) return null;
            objModelData.getObjObjects().forEach(d -> objObjects.add(new ObjObjectRenderer(d)));
            return new ObjModelRenderer(objModelPath, objObjects, objModelData.getMaterials(), textureVariants);
        } catch (Exception e) {
            DynamXMain.log.error("Failed to load OBJ model " + objModelPath, e);
            return null;
        }
    }

    @Override
    public void uploadVAOs() {
        objObjects.forEach(ObjObjectRenderer::uploadVAO);
    }

    @Override
    public void clearVAOs() {
        objObjects.forEach(ObjObjectRenderer::clearVAO);
    }

    public void renderGroup(ObjObjectRenderer obj, byte textureDataId, boolean forceVanillaRender) {
        if (obj == null) return;
        if (obj.getObjObjectData() != null && "main".equalsIgnoreCase(obj.getObjObjectData().getName())) {
            return;
        }
        obj.render(this, textureDataId, forceVanillaRender);
    }

    @Override
    public boolean renderGroup(String group, byte textureDataId, boolean forceVanillaRender) {
        ObjObjectRenderer r = getObjObjectRenderer(group);
        if (r == null) return false;
        renderGroup(r, textureDataId, forceVanillaRender);
        return true;
    }

    @Override
    public boolean renderDefaultParts(byte textureDataId, boolean forceVanillaRender) {
        // Render every object that isn't a part-marker. The IModelTextureVariantsSupplier
        // gating (canRenderPart) is not wired in the minimal port — supplier may be null.
        boolean drawn = false;
        for (ObjObjectRenderer object : objObjects) {
            if (textureVariants == null || canRenderPart(object)) {
                renderGroup(object, textureDataId, forceVanillaRender);
                drawn = true;
            }
        }
        return drawn;
    }

    private boolean canRenderPart(ObjObjectRenderer object) {
        try {
            return textureVariants.canRenderPart(object.getObjObjectData().getName());
        } catch (Throwable t) {
            return true;
        }
    }

    @Override
    public void renderModel(byte textureDataId, boolean forceVanillaRender) {
        for (ObjObjectRenderer object : objObjects) {
            object.setObjectColor(modelColor);
            renderGroup(object, textureDataId, forceVanillaRender);
        }
    }

    public ObjObjectRenderer getObjObjectRenderer(String groupName) {
        return objObjects.stream()
                .filter(o -> o.getObjObjectData() != null && o.getObjObjectData().getName().equalsIgnoreCase(groupName))
                .findFirst().orElse(null);
    }

    @Override
    public boolean containsObjectOrNode(String name) {
        return objObjects.stream()
                .anyMatch(o -> o.getObjObjectData() != null && o.getObjObjectData().getName().equalsIgnoreCase(name));
    }

    @Override
    public boolean isEmpty() {
        return objObjects.isEmpty();
    }
}
