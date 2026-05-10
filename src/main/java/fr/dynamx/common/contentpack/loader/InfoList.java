package fr.dynamx.common.contentpack.loader;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.DynamX;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.common.contentpack.type.ObjectInfo;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import net.minecraftforge.fml.loading.FMLEnvironment;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * List of {@link ISubInfoTypeOwner}s with utility methods to load them
 *
 * @param <T> The objects class
 * @see INamedObject
 * @see ObjectLoader
 *
 * TODO port:1.20.1 - Original referenced (not yet ported):
 *   - fr.dynamx.common.DynamXMain (logger + resourcesDirectory) - replaced by DynamX.LOGGER;
 *     resourcesDirectory accesses are stubbed (translations cannot be written until the
 *     resources dir is wired in Phase 2).
 *   - fr.dynamx.common.items.DynamXItemRegistry + DynamXReflection.getCreativeTabName - both
 *     belong to Phase 6 (items registry rework: 1.20.1 uses Holder/CreativeModeTabs registry).
 *   - fr.dynamx.utils.client.ContentPackUtils.addMissingLangTranslation - not yet ported.
 *   - net.minecraft.creativetab.CreativeTabs -&gt; net.minecraft.world.item.CreativeModeTab
 *     (entirely different registry-based API in 1.20.1).
 *   - net.minecraftforge.fml.common.ProgressManager - removed in NeoForge; logging-only fallback.
 *   - net.minecraftforge.fml.common.FMLCommonHandler.getSide().isClient() -&gt;
 *     FMLEnvironment.dist.isClient().
 *   The updateItems creative-tab + lang-translation block is stubbed; item creation itself is
 *   delegated to ObjectInfo.createItems (which is Phase 6 stubbed at the moment).
 */
@Getter
public abstract class InfoList<T extends ISubInfoTypeOwner<?>> {
    /**
     * Loaded objects, identified by their full name
     */
    protected final Map<String, T> infos = new HashMap<>();
    /**
     * The default SubInfoTypesRegistry for this object (can be overridden by ISubInfoTypeOwners)
     */
    protected final SubInfoTypesRegistry<T> defaultSubInfoTypesRegistry;

    /**
     * @param defaultSubInfoTypesRegistry The default SubInfoTypesRegistry for this object (can be overridden by ISubInfoTypeOwners)
     */
    public InfoList(@Nullable SubInfoTypesRegistry<T> defaultSubInfoTypesRegistry) {
        this.defaultSubInfoTypesRegistry = defaultSubInfoTypesRegistry;
    }

    /**
     * @return The name of the listed objects, used for logging and errors
     */
    public abstract String getName();

    /**
     * Clears infos, used for hot reload
     *
     * @param hot If it's a hot reload
     */
    public void clear(boolean hot) {
        infos.clear();
    }

    /**
     * Puts the info into infos map, and updates other references to these objects (in {@link IDynamXItem}s for example)
     */
    public void loadItems(T info, boolean hot) {
        info.onComplete(hot);
        infos.put(info.getFullName(), info);
    }

    /**
     * Post-loads the objects (shape generation...), and creates the IInfoOwners
     *
     * @param hot True if it's a hot reload
     */
    public abstract void postLoad(boolean hot);

    /**
     * @return The info from the info's full name, or null
     */
    @Nullable
    public T findInfo(String infoFullName) {
        return infos.get(infoFullName);
    }

    public boolean hasSubInfoTypesRegistry() {
        return defaultSubInfoTypesRegistry != null;
    }

    /**
     * Creates and update {@link IDynamXItem}s depending on the loading phase (hot or not).
     *
     * @param infoList
     * @param owners
     * @param builtinObjects
     * @param hot
     * @param <T>
     * @param <C>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends ObjectInfo<?> & ISubInfoTypeOwner<?>, C extends IDynamXItem<?>> void updateItems(InfoList<T> infoList, List<IDynamXItem<T>> owners, List<T> builtinObjects, boolean hot) {
        Map<String, T> infos = infoList.getInfos();
        // TODO port:1.20.1 - ProgressManager was removed from NeoForge; replace by SLF4J logging
        //   until a progress UI replacement is wired (NeoForge has ModLoadingStage but not a
        //   user-facing progress bar API for pack loading).
        DynamX.LOGGER.info("Post-loading {} ({} entries)", infoList.getName(), infos.size());
        for (T info : infos.values()) {
            try {
                if (!info.postLoad(hot)) {
                    continue;
                }
            } catch (Exception e) {
                DynamXErrorManager.addError(info.getPackName(), DynamXErrorManager.PACKS_ERRORS, "complete_object_error", ErrorLevel.FATAL, info.getName(), null, e);
                continue;
            }
            if (!hot) {
                boolean client = FMLEnvironment.dist.isClient();
                Object[] tabItem = new Object[1];
                if (info instanceof AbstractItemObject) {
                    String creativeTabName = ((AbstractItemObject<?, ?>) info).getCreativeTabName();
                    if (creativeTabName != null && !creativeTabName.equalsIgnoreCase("None")) {
                        // TODO port:1.20.1 - Original built a CreativeTabs subclass with createIcon
                        //   and registered it through DynamXItemRegistry.creativeTabs.
                        //   1.20.1 replaces CreativeTabs with the CreativeModeTab.Builder pattern
                        //   registered via the CREATIVE_MODE_TAB registry; this requires Phase 6
                        //   (DynamXItemRegistry rework). Stubbed with a debug log.
                        DynamX.LOGGER.debug("Creative tab '{}' requested by {} - registration deferred to Phase 6", creativeTabName, info.getFullName());
                    }
                }
                if (!builtinObjects.contains(info)) {
                    C[] obj = (C[]) ((ObjectInfo<T>) info).createItems(infoList);
                    if (obj != null && obj.length > 0) {
                        tabItem[0] = obj[0];
                    }
                    if (obj != null) {
                        for (C ob : obj) {
                            owners.add((IDynamXItem<T>) ob);
                            if (client) {
                                if (ob instanceof IResourcesOwner && ((IResourcesOwner) ob).createTranslation()) {
                                    for (int metadata = 0; metadata < ((IResourcesOwner) ob).getMaxMeta(); metadata++) {
                                        // TODO port:1.20.1 - ContentPackUtils.addMissingLangTranslation +
                                        //   DynamXMain.resourcesDirectory are not yet ported. Skip silently
                                        //   until the i18n / resources pipeline is back online.
                                        String translationKey = info.getTranslationKey((IDynamXItem) ob, metadata) + ".name";
                                        String translationValue = info.getTranslatedName((IDynamXItem) ob, metadata);
                                        DynamX.LOGGER.trace("Translation pending: {} -> {}", translationKey, translationValue);
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (!builtinObjects.contains(info)) { //Refresh infos objects contained in created info owners
                boolean found = false;
                for (IDynamXItem<T> owner : owners) {
                    if (owner.getInfo().getFullName().equalsIgnoreCase(info.getFullName())) {
                        if (!found) {
                            T oldInfo = owner.getInfo();
                            info.setItems(oldInfo.getItems());
                            found = true;
                        }
                        owner.setInfo(info);
                        //Don't break, multiple items can have the same infos
                    }
                }
                if (!found) {
                    DynamX.LOGGER.error("Cannot hotswap {} in {}", info.getFullName(), owners);
                }
            }
        }
    }
}
