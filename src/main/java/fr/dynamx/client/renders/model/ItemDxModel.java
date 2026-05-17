package fr.dynamx.client.renders.model;

import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;

/**
 * Lightweight wrapper around an item's pack model location, used by the BEWLR pipeline
 * ({@link fr.dynamx.client.renders.model.renderer.DxItemModelLoader}) to feed the scene graph.
 */
@Getter
public class ItemDxModel {
    private final ResourceLocation location;
    @Setter
    private IModelPackObject owner;

    public ItemDxModel(ResourceLocation location, IModelPackObject owner) {
        this.location = location;
        this.owner = owner;
    }
}
