package fr.dynamx.api.events;

import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import lombok.Getter;
import lombok.Setter;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import javax.annotation.Nullable;

/**
 * Fired when creating an item for a pack object.
 *
 * TODO port:1.20.1 - Originally bounded as &lt;B extends ObjectInfo&lt;?&gt; &amp; ISubInfoTypeOwner&lt;?&gt;&gt;.
 *   ObjectInfo lives in fr.dynamx.common.contentpack.type (Phase 3b); the bound is relaxed
 *   here to ISubInfoTypeOwner only. Re-tighten once ObjectInfo is ported.
 * TODO port:1.20.1 - InfoList lives in fr.dynamx.common.contentpack.loader (Phase 3b);
 *   the loader field is typed as Object until then.
 *
 * @see CreatePackItemEvent
 */
public abstract class CreatePackItemEvent<B extends ISubInfoTypeOwner<?>, C extends IDynamXItem<B>> extends Event {
    /**
     * The loader of this object.
     */
    @Getter
    private final Object loader;
    /**
     * The ObjectInfo of the item to create
     */
    @Getter
    private final B objectInfo;
    /**
     * The item to use, set it to override the default behavior
     */
    @Getter
    @Setter
    @Nullable
    private C objectItem;

    public CreatePackItemEvent(Object loader, B objectInfo) {
        this.loader = loader;
        this.objectInfo = objectInfo;
    }

    /**
     * @return True if the spawn item has been replaced
     */
    public boolean isOverridden() {
        return objectItem != null;
    }

    public static class VehicleItem<B extends ISubInfoTypeOwner<?>, C extends IDynamXItem<B>> extends CreatePackItemEvent<B, C> implements ICancellableEvent {
        public VehicleItem(Object loader, B objectInfo) {
            super(loader, objectInfo);
        }
    }

    public static class SimpleItem<B extends ISubInfoTypeOwner<?>, C extends IDynamXItem<B>> extends CreatePackItemEvent<B, C> implements ICancellableEvent {
        public SimpleItem(Object loader, B objectInfo) {
            super(loader, objectInfo);
        }
    }

    public static class SimpleBlock<B extends ISubInfoTypeOwner<?>, C extends IDynamXItem<B>> extends CreatePackItemEvent<B, C> implements ICancellableEvent {
        public SimpleBlock(Object loader, B objectInfo) {
            super(loader, objectInfo);
        }
    }

    public static class PropsItem<B extends ISubInfoTypeOwner<?>, C extends IDynamXItem<B>> extends CreatePackItemEvent<B, C> implements ICancellableEvent {
        public PropsItem(Object loader, B objectInfo) {
            super(loader, objectInfo);
        }
    }
}
