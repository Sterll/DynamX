package fr.dynamx.api.contentpack.object;

/**
 * An IDynamXItem is something having an ObjectInfo. It is, for example, an item or a block
 *
 * @param <T> The type of the owner ObjectInfo
 *
 * TODO port:1.20.1 - The bound on T was originally ObjectInfo<?>; relaxed to Object until
 *   fr.dynamx.common.contentpack.type.ObjectInfo is ported (Phase 3b).
 */
public interface IDynamXItem<T> {
    /**
     * @return The ObjectInfo contained
     */
    T getInfo();

    /**
     * Updates the contained ObjectInfo, used for hot reload
     */
    void setInfo(T info);
}
