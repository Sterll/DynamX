package fr.dynamx.client.renders.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.dynamx.client.renders.RenderFrame;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import lombok.Getter;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

/**
 * 1.20.1 port of the 1.12 {@code ModelObjArmor}.
 * <p>
 * The legacy class extended {@code ModelBiped} and held one {@code ArmorRenderer} per body part.
 * In 1.20.1 armor models are {@link HumanoidModel}s that the Forge armor pipeline feeds with a
 * {@link PoseStack} + {@link VertexConsumer} via {@link #renderToBuffer}. Per-slot visibility is
 * driven by Forge's vanilla {@code HumanoidArmorLayer} which calls
 * {@link HumanoidModel#setAllVisible(boolean)} before rendering each slot, so we keep a single
 * model instance per pack and let the slot/textureId fields drive whatever is currently drawn.
 * <p>
 * TODO port:1.20.1 - the actual per-part DxModel rendering must be wired through the
 * {@link fr.dynamx.client.renders.scene.node.ArmorNode} scene graph once
 * {@link fr.dynamx.api.events.client.BuildSceneGraphEvent.BuildArmorScene} produces a real
 * pipeline (currently the ArmorNode body and OBJ/GLTF renderers are stubbed). For now this class
 * provides the vanilla skeleton plus stubs so {@link BaseRenderContext.ArmorRenderContext} and
 * call-sites in the scene graph keep compiling.
 */
public class ModelObjArmor extends HumanoidModel<LivingEntity> {

    /**
     * Builds an empty {@link LayerDefinition} matching the vanilla humanoid armor layout.
     * <p>
     * We rely on {@link HumanoidModel#createMesh(CubeDeformation, float)} for the part hierarchy so
     * that the {@link ModelPart} fields ({@code head}, {@code body}, {@code rightArm}, ...) inherited
     * from {@link HumanoidModel} stay bound to the canonical names. This lets Forge's
     * {@code HumanoidArmorLayer} drive {@link #setAllVisible(boolean)} per slot exactly like
     * vanilla armor models.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Getter
    private final ArmorObject<?> armorObject;
    @Getter
    private final DxModelRenderer model;
    @Getter
    private final BaseRenderContext.ArmorRenderContext renderContext = new BaseRenderContext.ArmorRenderContext(this);

    @Getter
    private EquipmentSlot activePart;
    @Getter
    private byte activeTextureId;

    public ModelObjArmor(ArmorObject<?> armorObject, DxModelRenderer model, ModelPart root) {
        super(root);
        this.armorObject = armorObject;
        this.model = model;
    }

    public void setActivePart(EquipmentSlot activePart, byte textureId) {
        this.activePart = activePart;
        this.activeTextureId = textureId;
    }

    /**
     * Vanilla armor render entry point. Called by {@code HumanoidArmorLayer} after the part-visibility
     * for the current slot has been configured and the parent humanoid model angles have been copied
     * into us via {@code ForgeHooksClient.copyModelProperties}.
     * <p>
     * TODO port:1.20.1 - delegate to {@code armorObject.getSceneGraph().render(...)} once
     * {@link fr.dynamx.client.renders.scene.node.ArmorNode} is unstubbed. For now we fall back to the
     * vanilla biped skeleton so the wiring is verifiable in-game even without GLTF/DxAnimator parts.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float r, float g, float b, float a) {
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        SceneNode<?, ?> sceneGraph = armorObject == null ? null : armorObject.getSceneGraph();
        if (sceneGraph == null || model == null || activePart == null) {
            return;
        }
        MultiBufferSource singleBuffer = renderType -> buffer;
        renderContext.setModelParams(null, activePart, model, activeTextureId);
        renderContext.setPoseStack(poseStack);
        renderContext.setBufferSource(singleBuffer);
        renderContext.setPackedLight(packedLight);
        RenderFrame.push(poseStack, singleBuffer, packedLight);
        try {
            ((SceneNode) sceneGraph).render(renderContext, armorObject, new Matrix4f());
        } finally {
            RenderFrame.clear();
        }
    }

    /**
     * Called by {@link fr.dynamx.client.renders.scene.node.ArmorNode}. The slot dispatch lived on the
     * legacy {@code ArmorRenderer} side; in 1.20.1 each per-part renderer will be a child scene node,
     * so this method is kept as a stub until the scene graph is unstubbed.
     */
    public void renderPart(Matrix4f transform, EquipmentSlot part, boolean forceVanillaRender) {
        // TODO port:1.20.1 - rebuild against HumanoidModel + GLTF / DxAnimator pipeline.
    }

    public void renderHead(float scale) {
        // TODO port:1.20.1
    }

    public void renderChest(float scale) {
        // TODO port:1.20.1
    }

    public void renderLeftArm(float scale) {
        // TODO port:1.20.1
    }

    public void renderRightArm(float scale) {
        // TODO port:1.20.1
    }

    public void renderLeftLeg(float scale) {
        // TODO port:1.20.1
    }

    public void renderRightLeg(float scale) {
        // TODO port:1.20.1
    }
}
