package fr.dynamx.client.renders.model.renderer;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraftforge.client.model.BakedModelWrapper;

/**
 * Wraps the baked inventory model of a DynamX item so that {@link #isCustomRenderer()} returns
 * {@code true}. That flag is what makes vanilla route the item through its
 * {@code IClientItemExtensions#getCustomRenderer()} (i.e. {@link DxItemModelLoader}) instead of
 * drawing the flat baked quads.
 *
 * <p>This reproduces the legacy behaviour of
 * {@code DxModelRegistry.getItemRenderer().registerItemModel(...)}, which rendered DX-model items
 * in full 3D in the inventory. Everything else (transforms, particle icon, overrides) is delegated
 * to the original model so the item keeps sane defaults.
 */
public class DxCustomRendererModel extends BakedModelWrapper<BakedModel> {

    public DxCustomRendererModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }
}
