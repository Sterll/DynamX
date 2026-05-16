package fr.dynamx.client.renders.model.renderer;

import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.EnumDxModelFormats;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import org.joml.Vector4f;

/**
 * Abstract base class for all DynamX model renderers (GLTF/OBJ).
 * <p>
 * TODO port:1.20.1 - The render preview path used GlStateManager + immediate-mode matrix stacks
 * which are gone in 1.20.1. Restore once {@code BaseRenderContext.BlockRenderContext} exposes a
 * {@code PoseStack}/{@code MultiBufferSource} that the preview can hook into.
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

    public void renderPreview(BlockObject<?> blockObjectInfo, Player player, BlockPos blockPos, boolean canPlace, float orientation, float partialTicks, int textureNum) {
        // TODO port:1.20.1 - rewrite preview rendering on top of PoseStack/MultiBufferSource + RenderType.
        // The legacy implementation used GlStateManager.pushMatrix/translate/rotate/scale + GL_ALL_ATTRIB_BITS push/pop.
    }

    public abstract boolean containsObjectOrNode(String name);

    public abstract boolean isEmpty();
}
