package fr.dynamx.client.renders;

import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.common.entities.RagdollEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import javax.annotation.Nullable;

/**
 * Renderer for ragdoll entities (a 6-body-part dummy following the physics engine).
 *
 * <p>TODO port:1.20.1 - This is entirely stubbed. The 1.12 implementation depended on:
 * <ul>
 *   <li>{@code ModelPlayer} / {@code ModelRenderer} (1.12) -&gt; {@code PlayerModel<AbstractClientPlayer>}
 *       / {@code ModelPart} in 1.20.1. The body-part field names changed
 *       ({@code bipedHead -> head}, {@code bipedBody -> body}, {@code bipedRightArm -> rightArm}, etc.).</li>
 *   <li>{@code AbstractClientPlayer#getLocationSkin / getSkinType} -&gt;
 *       {@code AbstractClientPlayer#getSkinTextureLocation()} / {@code getModelName()}.</li>
 *   <li>{@code entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD)} -&gt;
 *       {@code entity.getItemBySlot(EquipmentSlot.HEAD)}.</li>
 *   <li>{@code DynamXItemArmor#getInfo().getObjArmor()} - OBJ armor pipeline is being dropped; must
 *       be rebuilt on top of {@link fr.dynamx.client.renders.model.ModelObjArmor} (currently a stub).</li>
 *   <li>{@code GlStateManager.translate / rotate / pushMatrix / popMatrix} -&gt; {@code PoseStack}.</li>
 *   <li>{@code model.render(0.0625f)} (ModelRenderer scale) -&gt; the model is now rendered via
 *       {@code ModelPart#render(PoseStack, VertexConsumer, packedLight, packedOverlay)}.</li>
 *   <li>{@code entity.prevPosX / posX} -&gt; {@code entity.xOld / getX()}.</li>
 * </ul>
 *
 * @param <T> The ragdoll entity type
 */
public class RenderRagdoll<T extends RagdollEntity> extends RenderPhysicsEntity<T> {
    protected final BaseRenderContext.EntityRenderContext context = new BaseRenderContext.EntityRenderContext(this);

    public RenderRagdoll(EntityRendererProvider.Context context) {
        super(context);
        // TODO port:1.20.1 - ModelPlayer + skin lookup needs the EntityRendererProvider.Context's
        // modelSet for HumanoidModel.createMesh(...) -> bake LayerDefinitions then ModelPart.
        // TODO port:1.20.1 - was: MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(RagdollEntity.class, this));
    }

    @Override
    @Nullable
    public BaseRenderContext.EntityRenderContext getRenderContext(T entity) {
        return context;
    }

    @Override
    public void renderEntity(T entity, BaseRenderContext.EntityRenderContext context) {
        // TODO port:1.20.1 - Stubbed; per-bodypart interpolated translate+rotate + armor overlay
        // must be rewritten on top of PoseStack + ModelPart + the new GLTF armor pipeline. The 1.12
        // logic was: iterate EnumRagdollBodyPart, look up the live + previous transform from
        // entity.getTransforms(), interpolate position/rotation, push a PoseStack pose, optionally
        // draw a DynamXItemArmor overlay matching the slot, then render the matching biped body
        // part via ModelRenderer#render(0.0625f).
    }

    @Override
    public void renderEntityDebug(T entity, BaseRenderContext.EntityRenderContext context) {
    }
}
