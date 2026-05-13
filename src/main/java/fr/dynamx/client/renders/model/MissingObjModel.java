package fr.dynamx.client.renders.model;

import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.common.objloader.data.ObjObjectData;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Missing OBJ model indicating errors. Renders nothing — the {@code renderMissingModelFallback}
 * path in {@code RenderPhysicsEntity} handles the "no model" wireframe.
 */
public class MissingObjModel extends ObjModelRenderer {
    private static ObjObjectRenderer emptyPartRenderer;

    public MissingObjModel() {
        super(null, new ArrayList<>(), new HashMap<>(), null);
        ObjObjectRenderer objObjectRenderer = new ObjObjectRenderer(new ObjObjectData("missing")) {
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
        // no-op
    }

    @Override
    public ObjObjectRenderer getObjObjectRenderer(String groupName) {
        return emptyPartRenderer;
    }

    @Override
    public boolean renderGroup(String groupsName, byte textureDataId, boolean forceVanillaRender) {
        return true;
    }

    @Override
    public boolean renderDefaultParts(byte textureDataId, boolean forceVanillaRender) {
        return true;
    }

    @Override
    public void renderModel(byte textureDataId, boolean forceVanillaRender) {
        // no-op
    }

    @Override
    public boolean isEmpty() {
        return true;
    }
}
