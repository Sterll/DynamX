package fr.dynamx.client.renders.model;

import net.minecraft.resources.ResourceLocation;

/**
 * DynamX item model wrapper.
 * <p>
 * TODO port:1.20.1 - The 1.12 Forge {@code IModel}/{@code IBakedModel} pipeline is gone in 1.20.1.
 * Item rendering should be driven by {@link net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer}
 * (BEWLR) or by datagen-generated {@link net.minecraft.client.resources.model.BakedModel}s. This class
 * is preserved as a simple data holder.
 */
public class ItemDxModel {
    private final ResourceLocation location;
    private Object owner; // TODO port:1.20.1 - retype to IModelPackObject once api is fully ported

    private Object gui;       // TODO port:1.20.1 - was Forge IModel
    private Object guiBaked;  // TODO port:1.20.1 - was Forge IBakedModel

    public ItemDxModel(ResourceLocation location, Object owner) {
        this.location = location;
        this.owner = owner;
        // TODO port:1.20.1 - load alternate GUI model via the 1.20.1 ModelManager / ModelResourceLocation
    }

    public void setOwner(Object owner) {
        this.owner = owner;
    }

    public Object getOwner() {
        return owner;
    }

    public Object getGuiBaked() {
        return guiBaked;
    }

    public ResourceLocation getLocation() {
        return location;
    }
}
