package fr.dynamx.client.renders.model.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.EnumDxModelFormats;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.client.renders.RenderFrame;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import org.joml.Vector4f;

/**
 * Abstract base class for all DynamX model renderers (GLTF/OBJ).
 */
public abstract class DxModelRenderer {

    @Getter
    protected final DxModelPath location;

    @Getter
    protected final IModelTextureVariantsSupplier textureVariants;
    @Getter
    @Setter
    protected Vector4f modelColor = new Vector4f(1, 1, 1, 1);

    @Getter
    private final EnumDxModelFormats format;

    public DxModelRenderer(DxModelPath location, IModelTextureVariantsSupplier textureVariants) {
        this.location = location;
        this.textureVariants = textureVariants;
        this.format = location != null ? location.getFormat() : EnumDxModelFormats.OBJ;
    }

    public void renderModel(boolean forceVanillaRender) {
        renderModel((byte) 0, forceVanillaRender);
    }

    public abstract void renderModel(byte textureDataId, boolean forceVanillaRender);

    public abstract boolean renderGroup(String group, byte textureDataId, boolean forceVanillaRender);

    public abstract boolean renderDefaultParts(byte textureDataId, boolean forceVanillaRender);

    public void uploadVAOs() {
    }

    public void clearVAOs() {
    }

    public void renderPreview(BlockObject<?> blockObjectInfo, Player player, BlockPos blockPos, boolean canPlace,
                              float orientation, float partialTicks, int textureNum,
                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        double px = player.xo + (player.getX() - player.xo) * partialTicks;
        double py = player.yo + (player.getY() - player.yo) * partialTicks;
        double pz = player.zo + (player.getZ() - player.zo) * partialTicks;

        poseStack.pushPose();
        poseStack.translate(-px + blockPos.getX() + 0.5, -py + blockPos.getY() + 1.5, -pz + blockPos.getZ() + 0.5);
        poseStack.mulPose(GlQuaternionPool.get(DynamXGeometry.eulerToQuaternion(
                blockObjectInfo.getRotation().z,
                (blockObjectInfo.getRotation().y + orientation * 22.5f) % 360,
                blockObjectInfo.getRotation().x)));
        com.jme3.math.Vector3f translation = blockObjectInfo.getTranslation();
        poseStack.translate(translation.x, translation.y, translation.z);
        com.jme3.math.Vector3f scale = blockObjectInfo.getScaleModifier();
        poseStack.scale(scale.x, scale.y, scale.z);

        Vector4f previousColor = new Vector4f(modelColor);
        setModelColor(new Vector4f(canPlace ? 0 : 1, canPlace ? 1 : 0, 0, 0.7f));

        RenderFrame.push(poseStack, bufferSource, packedLight);
        try {
            // TODO port:1.20.1 - alpha 0.7f is not honored: ObjObjectRenderer emits vertices with
            // hard-coded alpha=1 and uses RenderType.entityCutout. Switch to a translucent variant
            // once preview blending is wired (and add a GltfModelRenderer/DxAnimator counterpart).
            renderModel((byte) textureNum, true);
        } finally {
            RenderFrame.clear();
        }

        setModelColor(previousColor);
        poseStack.popPose();
    }

    public abstract boolean containsObjectOrNode(String name);

    public abstract boolean isEmpty();
}
