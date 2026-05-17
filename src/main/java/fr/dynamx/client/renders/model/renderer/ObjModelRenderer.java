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

    private static final java.util.Set<String> DUMPED_DEFAULTS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Override
    public boolean renderDefaultParts(byte textureDataId, boolean forceVanillaRender) {
        // Render every object that isn't a part-marker. The IModelTextureVariantsSupplier
        // gating (canRenderPart) is not wired in the minimal port — supplier may be null.
        boolean drawn = false;
        String key = String.valueOf(getLocation());
        boolean dump = DUMPED_DEFAULTS.add(key);
        java.util.List<String[]> rows = dump ? new java.util.ArrayList<>() : null;
        for (ObjObjectRenderer object : objObjects) {
            boolean allowed = textureVariants == null || canRenderPart(object);
            if (dump) {
                rows.add(new String[]{
                        object.getObjObjectData() != null ? object.getObjObjectData().getName() : "null",
                        allowed ? "RENDER" : "skip",
                        allowed ? "+" : "-"
                });
            }
            if (allowed) {
                renderGroup(object, textureDataId, forceVanillaRender);
                drawn = true;
            }
        }
        if (dump) {
            org.apache.logging.log4j.Logger lg = org.apache.logging.log4j.LogManager.getLogger("DynamX-ObjDump");
            lg.info("");
            lg.info("+==============================================================================+");
            lg.info(String.format("| [ renderDefaultParts ] %-54s |", truncate(key, 54)));
            lg.info(String.format("|   supplier: %-65s|", truncate(String.valueOf(textureVariants), 65)));
            lg.info("+==============================================================================+");
            for (String[] r : rows) {
                lg.info(String.format("|   %s  %-40s  %-30s |", r[2], truncate(r[0], 40), r[1]));
            }
            lg.info("+==============================================================================+");
            lg.info("");
        }
        return drawn;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max - 3) + "...";
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
