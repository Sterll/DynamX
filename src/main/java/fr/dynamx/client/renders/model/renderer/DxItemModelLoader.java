package fr.dynamx.client.renders.model.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.api.events.client.DynamXRenderItemEvent;
import fr.dynamx.client.renders.RenderFrame;
import fr.dynamx.client.renders.model.ItemDxModel;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.AbstractItemNode;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.DynamXContext;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import java.util.HashMap;
import java.util.Map;

/**
 * BEWLR entry point for DynamX items. Plugged in through
 * {@code IClientItemExtensions#getCustomRenderer()} on every {@code DynamXItem} /
 * {@code DynamXItemBlock} / {@code DynamXItemArmor}.
 * <p>
 * Resolves the item's {@link ItemDxModel} lazily from {@link IResourcesOwner#getDxModel()}, sets up
 * an {@link BaseRenderContext.ItemRenderContext}, publishes the active {@link PoseStack} +
 * {@link MultiBufferSource} into the thread-local {@link RenderFrame}, then dispatches to the scene
 * graph via {@link AbstractItemNode#renderAsItemNode}.
 */
public class DxItemModelLoader extends BlockEntityWithoutLevelRenderer {
    public static final DxItemModelLoader INSTANCE = new DxItemModelLoader();

    private final Map<Item, Map<Byte, ItemDxModel>> itemToModel = new HashMap<>();
    private final BaseRenderContext.ItemRenderContext renderContext = new BaseRenderContext.ItemRenderContext();

    public static ItemDisplayContext renderType;

    public DxItemModelLoader() {
        super(null, null);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemDxModel model = resolveModel(stack.getItem());
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
        renderContext.setPoseStack(poseStack);
        renderContext.setBufferSource(bufferSource);
        renderContext.setPackedLight(packedLight);

        RenderFrame.push(poseStack, bufferSource, packedLight);
        try {
            if (!MinecraftForge.EVENT_BUS.post(new DynamXRenderItemEvent(renderContext, (AbstractItemNode<?, ?>) sceneGraph, DynamXRenderItemEvent.EventStage.PRE))) {
                ((AbstractItemNode<?, IModelPackObject>) sceneGraph).renderAsItemNode(renderContext, model.getOwner());
                MinecraftForge.EVENT_BUS.post(new DynamXRenderItemEvent(renderContext, (AbstractItemNode<?, ?>) sceneGraph, DynamXRenderItemEvent.EventStage.POST));
            }
        } finally {
            RenderFrame.clear();
        }
    }

    private ItemDxModel resolveModel(Item item) {
        Map<Byte, ItemDxModel> byMeta = itemToModel.computeIfAbsent(item, k -> new HashMap<>());
        ItemDxModel model = byMeta.get((byte) 0);
        if (model != null) {
            return model;
        }
        IResourcesOwner owner = IResourcesOwner.of(item);
        IModelPackObject pack = owner.getDxModel();
        if (pack == null) {
            return null;
        }
        ResourceLocation location = pack.getModel();
        if (location == null) {
            return null;
        }
        model = new ItemDxModel(location, pack);
        byMeta.put((byte) 0, model);
        return model;
    }

    public ItemDxModel getModel(Item of, byte meta) {
        Map<Byte, ItemDxModel> byMeta = itemToModel.get(of);
        return byMeta == null ? null : byMeta.get(meta);
    }

    public void refreshItemInfos() {
        itemToModel.clear();
    }
}
