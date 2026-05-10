package fr.dynamx.common.contentpack;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.DynamX;
import fr.dynamx.api.contentpack.DynamXAddon;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DynamX addons loader
 *
 * @see DynamXAddon
 *
 * TODO port:1.20.1 - Original referenced (not yet ported):
 *   - net.minecraftforge.fml.common.Loader / ModContainer / FMLCommonHandler -&gt; replaced
 *     by NeoForge's {@link ModList}. The legacy "set active mod container around addon
 *     init" trick has no direct NeoForge equivalent (NeoForge does not expose a mutable
 *     active mod container) - we simply invoke the addon init reflectively.
 *   - net.minecraftforge.fml.common.discovery.ASMDataTable / FMLConstructionEvent -&gt;
 *     replaced by {@link ModFileScanData} fetched via {@link ModList#getModFiles()}.
 *   - net.minecraftforge.fml.common.ProgressManager - removed; replaced by a log line.
 *   - fr.aym.acslib.api.services.mps.ModProtectionContainer / initMpsAddons - MPS hookup
 *     is paused in Phase 0; the {@code initMpsAddons} method is preserved but takes Object
 *     until MPS comes back online.
 *   The discovery loop reads the annotation's "sides" attribute reflectively because the
 *   ModFileScanData representation of enum values is API-stable (Type-encoded "L...Dist;").
 */
public class AddonLoader {
    /**
     * Loaded addons
     */
    @Getter
    private static final Map<String, AddonInfo> addons = new HashMap<>();

    /**
     * MPS event subscribers
     */
    private static final Map<String, Method> mpsInitSubscribers = new HashMap<>();

    /**
     * Discovers addons annotated with {@link DynamXAddon} across all loaded mod files.
     *
     * TODO port:1.20.1 - Original signature took {@code FMLConstructionEvent}. NeoForge
     * exposes annotation scan data per ModFile; the new method takes no argument.
     */
    public static void discoverAddons() {
        List<ModFileScanData.AnnotationData> annotations = collectAddonAnnotations();
        for (ModFileScanData.AnnotationData data : annotations) {
            if (!canRunOn(data.annotationData().get("sides"), FMLEnvironment.dist)) continue;
            String name = data.clazz().getClassName();
            try {
                Class<?> addon = Class.forName(data.clazz().getClassName());
                DynamXAddon an = addon.getAnnotation(DynamXAddon.class);
                if (an == null) continue;
                name = an.modid() + ":" + an.name();
                DynamX.LOGGER.debug("Found addon candidate {} of mod {}", an.name(), an.modid());
                boolean found = false;
                for (Method md : addon.getDeclaredMethods()) {
                    if (md.isAnnotationPresent(DynamXAddon.AddonEventSubscriber.class)) {
                        if (!Modifier.isStatic(md.getModifiers()))
                            throw new IllegalArgumentException("Addon's @AddonEventSubscriber init method must have static access !");
                        // TODO port:1.20.1 - Original supported a one-arg ModProtectionContainer
                        //   subscriber. MPS is paused (Phase 0); accept a one-arg Object subscriber
                        //   if its declared type ends with "ModProtectionContainer" so that addons
                        //   compiled against the legacy API still register, even though the MPS
                        //   bootstrap is not yet wired.
                        if (md.getParameterCount() == 1 && md.getParameterTypes()[0].getName().endsWith("ModProtectionContainer")) {
                            DynamX.LOGGER.debug("Found MPS init subscriber for addon {} of mod {}", an.name(), an.modid());
                            mpsInitSubscribers.put(an.modid(), md);
                            continue;
                        }
                        if (md.getParameterCount() != 0)
                            throw new IllegalArgumentException("Addon's @AddonEventSubscriber init method must have 0 parameters !");
                        getAddons().put(an.modid(), new AddonInfo(an.modid(), an.name(), an.version(), md, an.requiredOnClient()));
                        found = true;
                    }
                }
                if (!found)
                    throw new IllegalArgumentException("Addon class " + name + " (" + data.clazz().getClassName() + ") with not @AddonEventSubscriber init method");
            } catch (Exception e) {
                DynamXErrorManager.addError("DynamX initialization", DynamXErrorManager.INIT_ERRORS, "addon_load_error", ErrorLevel.FATAL, name, "Addon class: " + data.clazz().getClassName(), e, 900);
            }
        }
    }

    /**
     * Reads the {@code sides} attribute of a {@link DynamXAddon} annotation extracted from
     * {@link ModFileScanData}. The value is encoded as a {@code List<EnumHolder>} where each
     * holder exposes a {@link Type} descriptor and a value name (the Dist enum constant).
     *
     * TODO port:1.20.1 - Original used Forge's {@code ModAnnotation.EnumHolder}. NeoForge's
     * scan data uses an enum-like structure too; we reflectively access the {@code value()}
     * accessor so the code tolerates minor API shifts (Phase 0 risk).
     */
    @SuppressWarnings("unchecked")
    private static boolean canRunOn(Object addonSides, Dist current) {
        if (addonSides == null)
            return true; //default behavior
        try {
            for (Object holder : (Iterable<Object>) addonSides) {
                // Holder typically exposes a "value()" or "getValue()" returning the enum name.
                String value;
                try {
                    value = (String) holder.getClass().getMethod("value").invoke(holder);
                } catch (NoSuchMethodException nsme) {
                    value = (String) holder.getClass().getMethod("getValue").invoke(holder);
                }
                if (value != null && value.equalsIgnoreCase(current.name()))
                    return true;
            }
        } catch (Throwable t) {
            DynamX.LOGGER.warn("Failed to evaluate @DynamXAddon sides attribute", t);
            return true; //fail-open
        }
        return false;
    }

    /**
     * Initializes MPS-aware addons.
     *
     * TODO port:1.20.1 - Parameter relaxed to Object until {@link fr.aym.acslib.api.services.mps.ModProtectionContainer}
     *   wiring is back in Phase 0. Subscribers are still invoked; missing class loaders are logged.
     */
    public static void initMpsAddons(Object mpsContainer) {
        for (Map.Entry<String, Method> addon : mpsInitSubscribers.entrySet()) {
            try {
                addon.getValue().invoke(null, mpsContainer);
            } catch (Exception e) {
                DynamX.LOGGER.error("MPS addon {} cannot be initialized !", addon.getKey(), e);
                DynamXErrorManager.addError("DynamX initialization", DynamXErrorManager.INIT_ERRORS, "addon_init_error", ErrorLevel.FATAL, addon.getKey(), "Initializing mps dependencies", e);
            }
        }
        DynamX.LOGGER.info("Loaded MPS addons: {}", mpsInitSubscribers.keySet());
    }

    /**
     * Initializes all addons (discovered in init method)
     *
     * TODO port:1.20.1 - The legacy implementation temporarily set the active ModContainer
     *   around addon init so registry events targeted the right modid. NeoForge does not
     *   expose a mutable active mod container; addons must use the explicit registration
     *   API. We simply invoke the init method.
     */
    public static void initAddons() {
        DynamX.LOGGER.info("Loading DynamX addons ({} candidates)", getAddons().size());
        for (AddonInfo addon : getAddons().values()) {
            try {
                addon.initAddon();
            } catch (Exception e) {
                DynamX.LOGGER.error("Addon {} cannot be initialized !", addon.toString(), e);
                DynamXErrorManager.addError("DynamX initialization", DynamXErrorManager.INIT_ERRORS, "addon_init_error", ErrorLevel.FATAL, addon.getAddonName(), "Initializing the addon", e);
            }
        }
        DynamX.LOGGER.info("Loaded addons: {}", getAddons().values());
    }

    /**
     * @param addon The addon id as specified in its annotation (may also be the modid of the addon)
     * @return True if the addon is loaded
     */
    public static boolean isAddonLoaded(String addon) {
        return getAddons().containsKey(addon);
    }

    /**
     * Collects @DynamXAddon annotation data from all loaded mod files.
     *
     * TODO port:1.20.1 - Equivalent of FMLConstructionEvent.getASMHarvestedData().getAll(name).
     *   See {@link fr.dynamx.common.contentpack.loader.SubInfoTypesRegistry#collectAnnotations}
     *   for the same reflective pattern.
     */
    private static List<ModFileScanData.AnnotationData> collectAddonAnnotations() {
        List<ModFileScanData.AnnotationData> out = new ArrayList<>();
        Set<String> wanted = new HashSet<>();
        wanted.add("L" + DynamXAddon.class.getName().replace('.', '/') + ";");
        try {
            for (Object modFileInfo : ModList.get().getModFiles()) {
                Object modFile = modFileInfo.getClass().getMethod("getFile").invoke(modFileInfo);
                if (modFile == null) continue;
                Object scan = modFile.getClass().getMethod("getScanResult").invoke(modFile);
                if (!(scan instanceof ModFileScanData)) continue;
                for (ModFileScanData.AnnotationData annotation : ((ModFileScanData) scan).getAnnotations()) {
                    if (wanted.contains(annotation.annotationType().getDescriptor())) {
                        out.add(annotation);
                    }
                }
            }
        } catch (Throwable e) {
            DynamX.LOGGER.warn("Failed to enumerate ModFileScanData for @DynamXAddon discovery", e);
        }
        return out;
    }
}
