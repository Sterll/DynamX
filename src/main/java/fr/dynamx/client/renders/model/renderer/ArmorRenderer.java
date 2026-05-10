package fr.dynamx.client.renders.model.renderer;

import fr.dynamx.client.renders.model.ModelObjArmor;
import org.joml.Matrix4f;

/**
 * Single armor body-part renderer.
 * <p>
 * TODO port:1.20.1 - OBJ loader is being dropped; the entire ArmorRenderer flow needs to be rewritten
 * on top of {@link net.minecraft.client.model.HumanoidModel}/{@link net.minecraft.client.model.geom.ModelPart}
 * and the GLTF pipeline. This class no longer extends net.minecraft.client.model.ModelRenderer
 * (which was renamed to ModelPart in 1.20.1) — once the new pipeline is wired in, restore the
 * proper parent class.
 */
public class ArmorRenderer {
    private final ModelObjArmor model;
    private final DxModelRenderer objModel;
    private final ObjObjectRenderer objObjectRenderer;

    // Public mirrored fields from the legacy 1.12 ModelRenderer API so external callers compile.
    public boolean isHidden;
    public boolean showModel = true;
    public boolean mirror;
    public float rotationPointX;
    public float rotationPointY;
    public float rotationPointZ;
    public float rotateAngleX;
    public float rotateAngleY;
    public float rotateAngleZ;
    public float offsetX;
    public float offsetY;
    public float offsetZ;

    public ArmorRenderer(DxModelRenderer objModel, ModelObjArmor model, String partName) {
        this.model = model;
        this.objModel = objModel;
        // TODO port:1.20.1 - OBJ loader is being dropped; resolve via GLTF NodeModel lookup
        this.objObjectRenderer = null;
    }

    public void render(float scale) {
        // TODO port:1.20.1 - replace GlStateManager + ObjModelRenderer.renderGroup with PoseStack + GLTF render
    }

    public void render(Matrix4f transform, boolean forceVanillaRender) {
        // TODO port:1.20.1 - apply transform on PoseStack and route to GLTF renderer
    }

    public void renderWithRotation(float scale) {
        // TODO port:1.20.1 - rewrite with PoseStack rotation order (Y, X, Z) on top of GLTF renderer
    }

    public void postRender(float scale) {
        // TODO port:1.20.1 - post-render translation on PoseStack (no GlStateManager in 1.20.1)
    }
}
