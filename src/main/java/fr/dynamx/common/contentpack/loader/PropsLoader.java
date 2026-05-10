package fr.dynamx.common.contentpack.loader;

import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.common.contentpack.type.ObjectInfo;
import fr.dynamx.common.contentpack.type.objects.PropObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loader of props. This loader is different from {@link ObjectLoader} because props aren't loaded from files, but from their block owner.
 *
 * @param <T> The objects class
 * @see ObjectInfo
 *
 * TODO port:1.20.1 - Original returned ItemProps&lt;?&gt; from getItem(). ItemProps is Phase 6 -
 *   the return type has been relaxed to Object to avoid pulling in the unported class.
 */
public class PropsLoader<T extends PropObject<?>> extends InfoList<T> {
    /**
     * All {@link IDynamXItem}s associated with our objects
     */
    public final List<IDynamXItem<T>> owners = new ArrayList<>();
    /**
     * Builtin java objects added by mods, register once and remembered for hot reloads
     */
    protected final List<T> builtinObjects = new ArrayList<>();

    public PropsLoader() {
        super(new SubInfoTypesRegistry<>());
    }

    @Override
    public String getName() {
        return "props";
    }

    @Override
    public void clear(boolean hot) {
        super.clear(hot);
        //DO NOT CLEAR OWNERS, ITEMS ARE REUSED !
        for (T b : builtinObjects) {
            loadItems(b, hot);
        }
    }

    /**
     * Registers a builtin object, ie added from mods <br>
     * The object will have the same properties as if it was added in a pack,
     * and it is automatically reused when packs are reloaded <br> <br>
     * NOTE : Should be called during addons initialization
     *
     * @param modName The name of the mod adding this object
     * @param object  The builtin object to add
     * @throws IllegalStateException If you call this after the start of packs loading (see ContentPackLoader.isPackLoadingStarted)
     * @see fr.dynamx.api.contentpack.DynamXAddon
     */
    public void addBuiltinObject(String modName, T object) {
        if (ContentPackLoader.isPackLoadingStarted())
            throw new IllegalStateException("You should register your builtin objects before packs loading. Use the addon init callback.");
        builtinObjects.add(object);
        if (DynamXObjectLoaders.PACKS.findPackLocations(modName).isEmpty())
            DynamXObjectLoaders.PACKS.loadItems(PackInfo.forAddon(modName), false);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void postLoad(boolean hot) {
        // We pass Collections.emptyList() as the list of builtin objects, because, only for props, we still need to create the corresponding items
        InfoList.updateItems((InfoList) this, (List) owners, Collections.emptyList(), hot);
    }

    /**
     * @return Maps a built info with the right item, if initialized, or returns null
     *
     * TODO port:1.20.1 - Returned ItemProps&lt;?&gt; in the legacy code. Relaxed to Object
     *   until the items pipeline is back online (Phase 6).
     */
    public Object getItem(T from) {
        return from.getItems().length == 1 ? from.getItems()[0] : null;
    }
}
