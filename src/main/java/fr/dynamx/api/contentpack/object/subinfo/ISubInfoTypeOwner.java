package fr.dynamx.api.contentpack.object.subinfo;

import fr.dynamx.api.contentpack.object.INamedObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Owner of {@link ISubInfoType}s <br>
 * See {@link SubInfoTypeOwner} for an example of owner
 *
 * @param <T> The type of the implementing class
 */
public interface ISubInfoTypeOwner<T extends ISubInfoTypeOwner<?>> extends INamedObject {
    /**
     * Adds an {@link ISubInfoType}
     */
    void addSubProperty(ISubInfoType<T> property);

    /**
     * @return The list of owned {@link ISubInfoType}s
     */
    List<ISubInfoType<T>> getSubProperties();

    /**
     * @return The configured properties <strong>before</strong> parsing this object <br>
     * The list is modified by the object loader and then contains all the loaded properties
     *
     * TODO port:1.20.1 - PackFilePropertyData lives in fr.dynamx.common.contentpack.loader
     *   (Phase 3b); typed as Object until that package is ported.
     */
    default List<Object> getInitiallyConfiguredProperties() {
        return new ArrayList<>();
    }

    /**
     * @return The ISubInfoType matching to the given clazz, or null
     */
    // TODO port:1.20.1 - Relaxed type bound (was {@code A extends ISubInfoType<T>}) so call sites
    // holding a wildcard reference can still query by class without capture-conversion failure.
    @Nullable
    @SuppressWarnings("unchecked")
    default <A extends ISubInfoType<?>> A getSubPropertyByType(Class<A> clazz) {
        return (A) this.getSubProperties().stream().filter(p -> clazz.equals(p.getClass())).findFirst().orElseGet(() -> null);
    }

    /**
     * If null, the object loader will use the last used registry (usually the default one)
     *
     * @return The sub info types registry for this object
     *
     * TODO port:1.20.1 - SubInfoTypesRegistry lives in fr.dynamx.common.contentpack.loader
     *   (Phase 3b); typed as Object until that package is ported.
     */
    default Object getSubInfoTypesRegistry() {
        return null;
    }
}
