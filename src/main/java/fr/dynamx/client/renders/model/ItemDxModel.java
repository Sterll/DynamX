package fr.dynamx.client.renders.model;

import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import lombok.Getter;
import lombok.Setter;

/**
 * DynamX item model wrapper.
 * <p>
 * TODO port:1.20.1 - The 1.12 Forge {@code IModel}/{@code IBakedModel} pipeline is gone in 1.20.1.
 * Item rendering should be driven by {@link net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer}
 * (BEWLR) or by datagen-generated {@link BakedModel}s. The GUI baked model is loaded lazily via the
 * 1.20.1 ModelManager when the item is first rendered in-GUI.
 */
@Getter
public class ItemDxModel {
    private final ResourceLocation location;
    @Setter
    private IModelPackObject owner;

    /**
     * Baked GUI model resolved against the 1.20.1 {@link net.minecraft.client.resources.model.ModelManager}.
     * Populated lazily by the item renderer the first time it needs the GUI variant.
     */
    @Setter
    private BakedModel guiBaked;

    public ItemDxModel(ResourceLocation location, IModelPackObject owner) {
        this.location = location;
        this.owner = owner;
    }
}
