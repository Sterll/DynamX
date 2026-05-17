package fr.dynamx.api.events;

import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.common.contentpack.loader.InfoList;
import fr.dynamx.common.contentpack.type.ObjectInfo;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;

/**
 * Fired when creating an item for a pack object.
 *
 * @see CreatePackItemEvent
 */
public abstract class CreatePackItemEvent<B extends ObjectInfo<?> & ISubInfoTypeOwner<?>, C extends IDynamXItem<B>> extends Event {
    /**
     * The loader of this object.
     */
    @Getter
    private final InfoList<B> loader;
    /**
     * The ObjectInfo of the item to create.
     */
    @Getter
    private final B objectInfo;
    /**
     * The item to use, set it to override the default behavior.
     */
    @Getter
    @Setter
    @Nullable
    private C objectItem;

    public CreatePackItemEvent(InfoList<B> loader, B objectInfo) {
        this.loader = loader;
        this.objectInfo = objectInfo;
    }

    /**
     * @return True if the spawn item has been replaced
     */
    public boolean isOverridden() {
        return objectItem != null;
    }

    /**
     * Called by the ContentPackSystem when creating the item of a {@link ModularVehicleInfo}.
     */
    @Cancelable
    public static class VehicleItem<B extends ObjectInfo<?> & ISubInfoTypeOwner<?>, C extends IDynamXItem<B>> extends CreatePackItemEvent<B, C> {
        public VehicleItem(InfoList<B> loader, B objectInfo) {
            super(loader, objectInfo);
        }
    }

    /**
     * Called by the ContentPackSystem when creating the item of a {@link AbstractItemObject}.
     */
    @Cancelable
    public static class SimpleItem<B extends ObjectInfo<?> & ISubInfoTypeOwner<?>, C extends IDynamXItem<B>> extends CreatePackItemEvent<B, C> {
        public SimpleItem(InfoList<B> loader, B objectInfo) {
            super(loader, objectInfo);
        }
    }

    /**
     * Called by the ContentPackSystem when creating the block of a {@link fr.dynamx.common.contentpack.type.objects.BlockObject}.
     */
    @Cancelable
    public static class SimpleBlock<B extends ObjectInfo<?> & ISubInfoTypeOwner<?>, C extends IDynamXItem<B>> extends CreatePackItemEvent<B, C> {
        public SimpleBlock(InfoList<B> loader, B objectInfo) {
            super(loader, objectInfo);
        }
    }

    /**
     * Called by the ContentPackSystem when creating the item of a {@link PropObject}.
     */
    @Cancelable
    public static class PropsItem<B extends ObjectInfo<?> & ISubInfoTypeOwner<?>, C extends IDynamXItem<B>> extends CreatePackItemEvent<B, C> {
        public PropsItem(InfoList<B> loader, B objectInfo) {
            super(loader, objectInfo);
        }
    }
}
