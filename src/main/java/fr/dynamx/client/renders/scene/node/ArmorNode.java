package fr.dynamx.client.renders.scene.node;

import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;

import java.util.List;

/**
 * A type of root node, corresponding to an armor
 *
 * <p>TODO port:1.20.1 -
 * <ul>
 *   <li>{@code EntityEquipmentSlot} -> {@link EquipmentSlot}.</li>
 *   <li>{@code ItemCameraTransforms.TransformType} -> {@link ItemDisplayContext}.</li>
 *   <li>{@code packInfo.getObjArmor()} + {@code DynamXItemArmor.armorType} are part of the
 *       contentpack/items api (Phase 6/8); we leave the call sites stubbed because
 *       {@link fr.dynamx.client.renders.model.ModelObjArmor} is itself a heavy stub
 *       (the OBJ pipeline is being dropped).</li>
 *   <li>{@code context.getArmorModel().isSneak} -- replaced by {@code context.getEntity().isCrouching()}
 *       which we forward into {@code armorModel} via {@code setModelAttributes} once HumanoidModel
 *       rebinding is implemented.</li>
 * </ul>
 *
 * @param <A> The type of the pack info (the owner of the scene graph)
 */
@RequiredArgsConstructor
public class ArmorNode<A extends ArmorObject<?>> extends AbstractItemNode<BaseRenderContext.ArmorRenderContext, A> {
    /**
     * The children that are linked to the entity (ie that will be rendered with the entity
     * transformations)
     */
    @Getter
    private final List<SceneNode<BaseRenderContext.ArmorRenderContext, A>> linkedChildren;

    /**
     * The transformation matrix of the node <br>
     * Stores the transformations of the node, and is used to render the node and its children <br>
     * Do not use GlStateManager to apply transformations, use this matrix instead
     */
    private final Matrix4f transform = new Matrix4f();

    @Override
    public void render(BaseRenderContext.ArmorRenderContext context, A packInfo, Matrix4f parentTransform) {
        transform.identity();
        // TODO port:1.20.1 - was:
        //   context.getArmorModel().isSneak = context.getEntity() != null && context.getEntity().isSneaking();
        //   if (context.getArmorModel().isSneak) { transform.translate(0.0F, 0.2F, 0.0F); }
        if (context.getEntity() != null && context.getEntity().isCrouching()) {
            transform.translate(0.0F, 0.2F, 0.0F);
        }
        // TODO port:1.20.1 - ModelObjArmor#renderPart is stubbed in Phase 7 (OBJ loader is dropped).
        context.getArmorModel().renderPart(transform, context.getEquipmentSlot(), context.isUseVanillaRender());
        // Render the linked children
        if (!linkedChildren.isEmpty()) {
            linkedChildren.forEach(c -> c.render(context, packInfo, transform));
        }
        // TODO port:1.20.1 - DynamXRenderUtils.popGlAllAttribBits() removed in core profile
    }

    @Override
    public void renderItemModel(BaseRenderContext.ItemRenderContext context, A packInfo, Matrix4f transform) {
        // TODO port:1.20.1 - armor item render path. Was:
        //   EntityEquipmentSlot slot = ((DynamXItemArmor<?>) context.getStack().getItem()).armorType;
        //   packInfo.getObjArmor().setActivePart(slot, context.getTextureId());
        //   packInfo.getObjArmor().setModelAttributes(packInfo.getObjArmor());
        //   if (context.getRenderType() != ItemCameraTransforms.TransformType.GUI)
        //       transform.rotate((float) (Math.PI / 2), 1, 0, 0);
        //   switch (slot) { ... per-slot translation ... }
        //   transform.rotate((float) Math.PI, 0, 0, 1);
        //   packInfo.getObjArmor().renderPart(transform, slot, context.isUseVanillaRender());
        // Rewrite on top of HumanoidModel + GLTF pipeline once ModelObjArmor is rebuilt and
        // DynamXItemArmor exposes its EquipmentSlot.
        if (context.getRenderType() != ItemDisplayContext.GUI) {
            transform.rotate((float) (Math.PI / 2), new org.joml.Vector3f(1, 0, 0));
        }
        transform.rotate((float) Math.PI, new org.joml.Vector3f(0, 0, 1));
    }

    @Override
    public void renderDebug(BaseRenderContext.ArmorRenderContext context, A packInfo) {
    }

    @Override
    public SceneNode<BaseRenderContext.ArmorRenderContext, A> getParent() {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }

    @Override
    public void setParent(SceneNode<BaseRenderContext.ArmorRenderContext, A> parent) {
        throw new UnsupportedOperationException("This node is a root node, it can't have a parent");
    }
}
