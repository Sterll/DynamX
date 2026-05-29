package fr.dynamx.client.handlers;

import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.client.renders.model.renderer.DxCustomRendererModel;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

/**
 * Wires DynamX items that own a valid DX model to their 3D inventory renderer.
 *
 * <p>The content packs ship a {@code builtin/generated} (flat icon) model for every item, but the
 * {@code _default} variants point at an icon texture that does not exist - in 1.12 those items were
 * drawn in 3D by the custom item renderer instead (see legacy
 * {@code DynamXItemRegistry.registerModel}: {@code if getDxModel().isModelValid() -> registerItemModel}).
 *
 * <p>The 1.20.1 equivalent is to flag the item's baked inventory model as a custom-renderer model so
 * vanilla dispatches it to {@link fr.dynamx.client.renders.model.renderer.DxItemModelLoader} (the
 * BEWLR already returned by every item's {@code IClientItemExtensions}). We do that here by wrapping
 * the freshly baked model in {@link DxCustomRendererModel} during {@link ModelEvent.ModifyBakingResult}.
 */
@Mod.EventBusSubscriber(modid = DynamXConstants.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DynamXItemModelHandler {

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        int wrapped = 0;
        for (IResourcesOwner owner : DynamXItemRegistry.getItems()) {
            Item item = owner.getItem();
            if (item == null) {
                continue;
            }
            // Legacy condition: only DX-model items with a valid (non-json) model render in 3D.
            IModelPackObject dxModel = owner.getDxModel();
            if (dxModel == null || !dxModel.isModelValid()) {
                continue;
            }
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null) {
                continue;
            }
            ModelResourceLocation mrl = new ModelResourceLocation(id, "inventory");
            BakedModel original = models.get(mrl);
            if (original != null && !(original instanceof DxCustomRendererModel)) {
                models.put(mrl, new DxCustomRendererModel(original));
                wrapped++;
            }
        }
        if (wrapped > 0) {
            org.apache.logging.log4j.LogManager.getLogger("DynamX")
                    .info("[Models] Flagged {} DX-model item(s) for 3D inventory rendering", wrapped);
        }
    }
}
