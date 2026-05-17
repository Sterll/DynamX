package fr.dynamx.client.renders.model.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.api.events.client.DynamXRenderItemEvent;
import fr.dynamx.client.renders.model.ItemDxModel;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.AbstractItemNode;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.DynamXContext;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

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
    public static final DxItemModelLoader INSTANCE = new DxItemModelLoader();

    private final Map<ResourceLocation, IResourcesOwner> REGISTRY = new HashMap<>();
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

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Map<Byte, ItemDxModel> byMeta = ITEM_TO_MODEL.get(stack.getItem());
        if (byMeta == null) {
            // TODO port:1.20.1 - fall back to vanilla missing model render once we wire ItemRenderer access
            return;
        }
        ItemDxModel model = byMeta.get((byte) 0);
        if (model == null || model.getOwner() == null) {
            return;
        }
        DxModelRenderer modelRenderer = DynamXContext.getDxModelRegistry().getModel(model.getOwner().getModel());
        if (modelRenderer == null) {
            return;
        }
        SceneNode<?, ?> sceneGraph = model.getOwner().getSceneGraph();
        if (!(sceneGraph instanceof AbstractItemNode)) {
            return;
        }
        renderType = displayContext;
        renderContext.setModelParams(model, stack, modelRenderer, (byte) 0)
                .setRenderParams(displayContext, 0f, true);
        if (!MinecraftForge.EVENT_BUS.post(new DynamXRenderItemEvent(renderContext, (AbstractItemNode<?, ?>) sceneGraph, DynamXRenderItemEvent.EventStage.PRE))) {
            ((AbstractItemNode<?, IModelPackObject>) sceneGraph).renderAsItemNode(renderContext, model.getOwner());
            MinecraftForge.EVENT_BUS.post(new DynamXRenderItemEvent(renderContext, (AbstractItemNode<?, ?>) sceneGraph, DynamXRenderItemEvent.EventStage.POST));
        }
    }

    public ItemDxModel getModel(Item of, byte meta) {
        Map<Byte, ItemDxModel> byMeta = ITEM_TO_MODEL.get(of);
        return byMeta == null ? null : byMeta.get(meta);
    }

    /**
     * Registers an item -> {@link ItemDxModel} mapping for the given meta variant.
     * <p>
     * TODO port:1.20.1 - In 1.20.1 the actual rendering glue (BEWLR override on the Item, or
     * ModelEvent.RegisterAdditional for a JSON baked model) still has to be wired alongside the
     * scene-graph BEWLR rewrite; this method only maintains the lookup table for now.
     */
    public void registerItemModel(Item item, int meta, ResourceLocation location) {
        IResourcesOwner owner = REGISTRY.get(location);
        fr.dynamx.api.contentpack.object.render.IModelPackObject pack =
                owner instanceof fr.dynamx.api.contentpack.object.render.IModelPackObject
                        ? (fr.dynamx.api.contentpack.object.render.IModelPackObject) owner
                        : null;
        ITEM_TO_MODEL.computeIfAbsent(item, k -> new HashMap<>())
                .put((byte) meta, new ItemDxModel(location, pack));
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
