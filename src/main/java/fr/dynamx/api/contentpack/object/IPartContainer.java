package fr.dynamx.api.contentpack.object;

import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface IPartContainer<T extends ISubInfoTypeOwner<T>> extends ISubInfoTypeOwner<T> {

    /**
     * @param clazz The class of the parts to return
     * @param <A>   The type of the parts to return
     * @return All the parts of the given type
     *
     * TODO port:1.20.1 - Relaxed type bound from {@code <A extends BasePart<T>>} to
     * {@code <A extends BasePart<?>>} so call sites holding a {@code IPartContainer<?>} wildcard
     * reference can still query parts by class without forcing a capture-conversion mismatch.
     */
    @SuppressWarnings("unchecked")
    default <A extends BasePart<?>> List<A> getPartsByType(Class<A> clazz) {
        return (List<A>) this.getAllParts().stream().filter(p -> clazz.isAssignableFrom(p.getClass())).collect(Collectors.toList());
    }

    List<BasePart<T>> getAllParts();

    /**
     * @param clazz The class of the part to return
     * @param <A>   The type of the part to return
     * @return The part with the given type and the given id (wheel index for example), or null
     */
    default <A extends BasePart<?>> A getPartByTypeAndId(Class<A> clazz, byte id) {
        return getPartsByType(clazz).stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }

    /**
     * Adds a {@link BasePart} to this object
     */
    void addPart(BasePart<T> tBasePart);

    default <U extends InteractivePart<?, ?>> List<U> getInteractiveParts() {
        return Collections.emptyList();
    }
}
