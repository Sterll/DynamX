package fr.dynamx.client.renders.scene;

import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.ItemDxModel;
import fr.dynamx.client.renders.model.ModelObjArmor;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Base class for the render context of a scene node.
 * <p>
 * TODO port:1.20.1 - PoseStack + MultiBufferSource + packedLight should be added to this context so
 * that scene nodes can hand them down to the GLTF renderer (the legacy code relied on the global
 * GlStateManager matrix stack which is gone in 1.20.1).
 *
 * @see IRenderContext
 * @see SceneNode
 */
@Getter
public abstract class BaseRenderContext implements IRenderContext {
    private DxModelRenderer model;
    private byte textureId;
    private float partialTicks;
    private boolean useVanillaRender;

    protected BaseRenderContext setModelParams(@Nonnull DxModelRenderer model, byte textureId) {
        this.model = model;
        this.textureId = textureId;
        return this;
    }

    protected BaseRenderContext setRenderParams(float partialTicks, boolean useVanillaRender) {
        this.partialTicks = partialTicks;
        this.useVanillaRender = useVanillaRender;
        return this;
    }

    @Getter
    @RequiredArgsConstructor
    public static class EntityRenderContext extends BaseRenderContext {
        private final RenderPhysicsEntity<?> render;
        private final Vector3f renderPosition = new Vector3f();
        @Nullable
        private ModularPhysicsEntity<?> entity;

        public EntityRenderContext setModelParams(@Nullable ModularPhysicsEntity<?> entity, @Nonnull DxModelRenderer model, byte textureId) {
            this.entity = entity;
            return (EntityRenderContext) super.setModelParams(model, textureId);
        }

        @Override
        public BaseRenderContext setModelParams(@Nonnull DxModelRenderer model, byte textureId) {
            return setModelParams(null, model, textureId);
        }

        public EntityRenderContext setRenderParams(double x, double y, double z, float partialTicks, boolean useVanillaRender) {
            renderPosition.set((float) x, (float) y, (float) z);
            return (EntityRenderContext) super.setRenderParams(partialTicks, useVanillaRender);
        }
    }

    @Getter
    public static class BlockRenderContext extends BaseRenderContext {
        private final Vector3f renderPosition = new Vector3f();
        @Nullable
        private TEDynamXBlock tileEntity;

        public BlockRenderContext setModelParams(@Nullable TEDynamXBlock tileEntity, @Nonnull DxModelRenderer model, byte textureId) {
            this.tileEntity = tileEntity;
            return (BlockRenderContext) super.setModelParams(model, textureId);
        }

        @Override
        public BaseRenderContext setModelParams(@Nonnull DxModelRenderer model, byte textureId) {
            return setModelParams(null, model, textureId);
        }

        public BlockRenderContext setRenderParams(double x, double y, double z, float partialTicks, boolean useVanillaRender) {
            renderPosition.set((float) x, (float) y, (float) z);
            return (BlockRenderContext) super.setRenderParams(partialTicks, useVanillaRender);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class ItemRenderContext extends BaseRenderContext {
        private ItemDxModel itemModel;
        /**
         * TODO port:1.20.1 - was ItemCameraTransforms.TransformType in 1.12; replaced by ItemDisplayContext in 1.20.1.
         */
        private ItemDisplayContext renderType;
        private ItemStack stack;

        public ItemRenderContext setModelParams(@Nonnull ItemDxModel itemModel, @Nonnull ItemStack stack, @Nonnull DxModelRenderer model, byte textureId) {
            this.itemModel = itemModel;
            this.stack = stack;
            return (ItemRenderContext) super.setModelParams(model, textureId);
        }

        public ItemRenderContext setRenderParams(@Nonnull ItemDisplayContext renderType, float partialTicks, boolean useVanillaRender) {
            this.renderType = renderType;
            return (ItemRenderContext) super.setRenderParams(partialTicks, useVanillaRender);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class ArmorRenderContext extends BaseRenderContext {
        private final ModelObjArmor armorModel;
        @Nullable
        private LivingEntity entity;
        private EquipmentSlot equipmentSlot;

        public ArmorRenderContext setModelParams(@Nullable LivingEntity entity, @Nonnull EquipmentSlot equipmentSlot, @Nonnull DxModelRenderer model, byte textureId) {
            this.entity = entity;
            this.equipmentSlot = equipmentSlot;
            setRenderParams(1, true);
            return (ArmorRenderContext) super.setModelParams(model, textureId);
        }
    }
}
