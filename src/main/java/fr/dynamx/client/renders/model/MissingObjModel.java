package fr.dynamx.client.renders.model;

import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Missing OBJ model indicating errors.
 * <p>
 * TODO port:1.20.1 - OBJ loader is being dropped. This class is kept as a stub so that callers
 * compile; the proper missing-model fallback must be reauthored on top of the 1.20.1 BakedModel
 * fallback (or simply rendered with the {@code ModelManager#getMissingModel()} flow).
 */
public class MissingObjModel extends ObjModelRenderer {
    private static ObjObjectRenderer emptyPartRenderer;

    public MissingObjModel() {
        super(null, new ArrayList<>(), new HashMap<>(), null);
        // TODO port:1.20.1 - OBJ loader is being dropped
        ObjObjectRenderer objObjectRenderer = new ObjObjectRenderer(null) {
            @Override
            public void render(ObjModelRenderer model, byte textureVariantID, boolean forceVanillaRender) {
                MissingObjModel.this.renderModel(textureVariantID, forceVanillaRender);
            }
        };
        getObjObjects().add(objObjectRenderer);
        emptyPartRenderer = objObjectRenderer;
    }

    public static ObjObjectRenderer getEmptyPart() {
        return emptyPartRenderer;
    }

    @Override
    public void renderGroup(ObjObjectRenderer group, byte textureDataId, boolean forceVanillaRender) {
        renderModel(textureDataId, forceVanillaRender);
    }

    @Override
    public ObjObjectRenderer getObjObjectRenderer(String groupName) {
        return emptyPartRenderer;
    }

    @Override
    public boolean renderGroup(String groupsName, byte textureDataId, boolean forceVanillaRender) {
        renderModel(textureDataId, forceVanillaRender);
        return true;
    }

    @Override
    public boolean renderDefaultParts(byte textureDataId, boolean forceVanillaRender) {
        renderModel(textureDataId, forceVanillaRender);
        return true;
    }

    @Override
    public void renderModel(byte textureDataId, boolean forceVanillaRender) {
        // TODO port:1.20.1 - render a red "Error" placeholder via PoseStack/MultiBufferSource +
        // Minecraft.getInstance().font.draw(...) and a debug AABB outline RenderType
    }
}
