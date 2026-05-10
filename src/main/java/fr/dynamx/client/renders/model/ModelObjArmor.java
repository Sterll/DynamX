package fr.dynamx.client.renders.model;

import fr.dynamx.client.renders.model.renderer.ArmorRenderer;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import lombok.Getter;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Matrix4f;

/**
 * 1.12-era OBJ-armor model that extended {@code ModelBiped}.
 * <p>
 * TODO port:1.20.1 - OBJ loader is being dropped; armor rendering must be rebuilt on top of
 * {@link net.minecraft.client.model.HumanoidModel} + {@link net.minecraft.client.model.geom.ModelPart}
 * + the GLTF pipeline. This class is preserved as a stub so external callers
 * ({@link fr.dynamx.client.renders.scene.BaseRenderContext.ArmorRenderContext}, scene graph) still compile.
 */
public class ModelObjArmor {
    private static final Matrix4f tempTransform = new Matrix4f();
    private final Object armorObject;     // TODO port:1.20.1 - was ArmorObject<?>
    private final DxModelRenderer model;
    private final BaseRenderContext.ArmorRenderContext renderContext = new BaseRenderContext.ArmorRenderContext(this);
    private ArmorRenderer head;
    private ArmorRenderer body;
    private ArmorRenderer[] arms;
    private ArmorRenderer[] legs;
    private ArmorRenderer[] foot;

    @Getter
    private EquipmentSlot activePart;
    @Getter
    private byte activeTextureId;

    public ModelObjArmor(Object armorObject, DxModelRenderer model) {
        this.armorObject = armorObject;
        this.model = model;
        // TODO port:1.20.1 - OBJ loader is being dropped; head/body/arms/legs/foot were wired from
        // ArmorObject getters and bound to the parent ModelBiped's bipedHead/bipedBody/... fields.
    }

    public void setActivePart(EquipmentSlot activePart, byte textureId) {
        this.activePart = activePart;
        this.activeTextureId = textureId;
    }

    public void renderPart(Matrix4f transform, EquipmentSlot part, boolean forceVanillaRender) {
        // TODO port:1.20.1 - rebuild against HumanoidModel + GLTF renderer
    }

    public void renderHead(float scale) {
        if (head != null) head.render(scale);
    }

    public void renderChest(float scale) {
        if (body != null) body.render(scale);
    }

    public void renderLeftArm(float scale) {
        if (arms != null) arms[0].render(scale);
    }

    public void renderRightArm(float scale) {
        if (arms != null) arms[1].render(scale);
    }

    public void renderLeftLeg(float scale) {
        if (legs != null) legs[0].render(scale);
    }

    public void renderRightLeg(float scale) {
        if (legs != null) legs[1].render(scale);
    }
}
