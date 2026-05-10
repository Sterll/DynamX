package fr.dynamx.client.renders.model.renderer;

import fr.dynamx.client.renders.model.ItemDxModel;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Loader/renderer for DynamX item models.
 * <p>
 * TODO port:1.20.1 - The legacy pipeline relied on:
 * <ul>
 *   <li>{@code TileEntityItemStackRenderer} (now {@link BlockEntityWithoutLevelRenderer})</li>
 *   <li>{@code ICustomModelLoader} + {@code IModel} (removed; replaced by datagen-driven JSON/baked models or BEWLR)</li>
 *   <li>{@code ItemCameraTransforms.TransformType} (now {@link ItemDisplayContext})</li>
 *   <li>{@code ModelLoader.setCustomModelResourceLocation} (gone; use Item properties or BEWLR override)</li>
 * </ul>
 * The class is preserved as a stub; the real implementation must be reauthored against the 1.20.1
 * BEWLR + scene-graph rendering pipeline.
 */
public class DxItemModelLoader extends BlockEntityWithoutLevelRenderer {
    private final Map<ResourceLocation, Object> REGISTRY = new HashMap<>(); // TODO port:1.20.1 - IResourcesOwner
    private final Map<Item, Map<Byte, ItemDxModel>> ITEM_TO_MODEL = new HashMap<>();
    private final BaseRenderContext.ItemRenderContext renderContext = new BaseRenderContext.ItemRenderContext();

    public static ItemDisplayContext renderType;

    public DxItemModelLoader(BlockEntityRenderDispatcherShim dispatcher, ModelManager modelManager) {
        super(null, null);
    }

    public DxItemModelLoader() {
        super(null, null);
    }

    public void onResourceManagerReload(Object resourceManager) {
        // TODO port:1.20.1 - reload hook (ResourceManagerReloadListener) for the new pipeline
    }

    public boolean accepts(ResourceLocation modelLocation) {
        return REGISTRY.containsKey(modelLocation);
    }

    public Object loadModel(ResourceLocation modelLocation) {
        // TODO port:1.20.1 - rebuild model loading on top of BakedModel/UnbakedModel APIs
        return null;
    }

    public void renderByItem(ItemStack stack, float partialTicks) {
        // TODO port:1.20.1 - rewrite to use PoseStack/MultiBufferSource and scene-graph AbstractItemNode.renderAsItemNode
    }

    public void registerItemModel(Object item, int meta, ResourceLocation location) {
        // TODO port:1.20.1 - 1.20.1 uses ModelEvent.RegisterAdditional + Item property overrides
    }

    public ItemDxModel getModel(Item of, byte meta) {
        Map<Byte, ItemDxModel> byMeta = ITEM_TO_MODEL.get(of);
        return byMeta == null ? null : byMeta.get(meta);
    }

    public void refreshItemInfos() {
        // TODO port:1.20.1 - reload model registrations after pack reload
    }

    /**
     * Placeholder to keep a constructor signature; the real BEWLR constructor in 1.20.1 takes
     * the dispatchers from {@link net.minecraft.client.Minecraft#getEntityRenderDispatcher()}.
     */
    public interface BlockEntityRenderDispatcherShim {
    }
}
