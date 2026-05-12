package fr.dynamx.common.contentpack.loader;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.DynamX;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeEntry;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry for ModularVehicles sub info categories, such as shapes, wheels, seats, trailer attach or steering wheel <br>
 * You can create your custom registries
 *
 * @see SubInfoTypeEntry
 * @see ISubInfoTypeOwner
 * @see fr.dynamx.api.contentpack.object.subinfo.ISubInfoType
 *
 * TODO port:1.20.1 - Original referenced (not yet ported):
 *   - net.minecraftforge.fml.common.discovery.ASMDataTable - removed in NeoForge; replaced by
 *     {@link ModFileScanData} (gathered from {@link ModList}).
 *   - net.minecraftforge.fml.common.event.FMLConstructionEvent - replaced by a no-arg scan
 *     performed during mod loading (caller has to invoke {@code discoverSubInfoTypes()} once
 *     the mod registry is initialized).
 *   - Side check via {@code event.getSide().isClient()} -&gt; uses {@link net.minecraftforge.fml.loading.FMLEnvironment}.
 *   The wiring of SubInfoTypeRegistries.getInfoList() / .hasSubInfoTypesRegistry() is still
 *   stubbed in {@link SubInfoTypeRegistries} (Phase 0). The discovery loop therefore reflects
 *   on the returned object to register entries; failures are logged through DynamX.LOGGER.warn.
 */
public class SubInfoTypesRegistry<T extends ISubInfoTypeOwner<?>> {
    private final Map<String, SubInfoTypeEntry<T>> ENTRIES = new LinkedHashMap<>();
    private final Map<Class<? extends INamedObject>, IPackFilePropertyFixer> PROPERTY_FIXERS = new LinkedHashMap<>();

    /**
     * Protected : use {@link RegisteredSubInfoType} annotation
     */
    protected void addSubInfoType(SubInfoTypeEntry<T> entry) {
        if (ENTRIES.containsKey(entry.getKey()))
            throw new IllegalArgumentException("Sub info type entry with name " + entry.getKey() + " is already registered !");
        ENTRIES.put(entry.getKey(), entry);
    }

    /**
     * @return All registered sub info types, mapped by key name
     */
    public Map<String, SubInfoTypeEntry<T>> getEntries() {
        return ENTRIES;
    }

    protected void addSubInfoTypePropertiesFixer(Class<? extends INamedObject> subInfoTypeClass, IPackFilePropertyFixer fixer) {
        // In 1.20.1 the SubInfoTypeAnnotationCache may re-enter load() for a class whose
        // first attempt threw (e.g. an unparsable field). Re-registering the same fixer is
        // harmless and should not crash subsequent pack files of the same type, so skip
        // silently when the same class is registered again.
        if (PROPERTY_FIXERS.containsKey(subInfoTypeClass))
            return;
        PROPERTY_FIXERS.put(subInfoTypeClass, fixer);
    }

    @Nullable
    public IPackFilePropertyFixer getSubInfoTypePropertiesFixer(Class<? extends INamedObject> subInfoTypeClass) {
        return PROPERTY_FIXERS.get(subInfoTypeClass);
    }

    /**
     * Discovers all classes annotated with {@link RegisteredSubInfoType} across loaded mod
     * files and registers them with the appropriate {@link SubInfoTypeRegistries} entries.
     *
     * TODO port:1.20.1 - Original signature took an {@code FMLConstructionEvent}. NeoForge's
     * {@link ModFileScanData} is gathered via {@link ModList#getModFiles()} and can be queried
     * at any time after mod loading; the new signature takes no arguments. Reflection is used
     * to access {@link SubInfoTypeRegistries#getInfoList()} (currently returns Object/null per
     * Phase 0 stub) so this method degrades gracefully until DynamXObjectLoaders is wired in
     * Phase 3b.
     */
    public static void discoverSubInfoTypes() {
        List<ModFileScanData.AnnotationData> annotations = collectAnnotations();
        List<Class<?>> exploredClasses = new ArrayList<>();
        for (ModFileScanData.AnnotationData data : annotations) {
            String name = data.clazz().getClassName();
            try {
                Class<?> object = Class.forName(data.clazz().getClassName());
                if (!object.isAnnotationPresent(RegisteredSubInfoType.class) || exploredClasses.contains(object))
                    continue;
                exploredClasses.add(object);
                if (!ISubInfoType.class.isAssignableFrom(object))
                    throw new IllegalArgumentException("Only ISubInfoType objects can have the RegisteredSubInfoType annotation. Errored class: " + object);

                RegisteredSubInfoType an = object.getAnnotation(RegisteredSubInfoType.class);
                if (an.isClientOnly() && !net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient())
                    continue;
                Class<? extends ISubInfoTypeOwner<?>> subInfoTypeClass = null;
                if (an.registries().length >= 1)
                    subInfoTypeClass = an.registries()[0].getInfoOwnerType();
                //Find the right constructor
                Constructor<?> constructor = null;
                for (Constructor<?> cons : object.getDeclaredConstructors()) {
                    if (subInfoTypeClass != null && (Arrays.equals(cons.getParameterTypes(), new Class[]{subInfoTypeClass, String.class}) || Arrays.equals(cons.getParameterTypes(), new Class[]{subInfoTypeClass}))) {
                        constructor = cons;
                        break;
                    }
                    if (Arrays.equals(cons.getParameterTypes(), new Class[]{ISubInfoTypeOwner.class, String.class}) || Arrays.equals(cons.getParameterTypes(), new Class[]{ISubInfoTypeOwner.class})) {
                        constructor = cons;
                        break;
                    }
                }
                if (constructor == null) {
                    throw new NoSuchMethodException("@RegisteredSubInfoType class must have a constructor with parameters (ISubInfoTypeOwner, String) or (ISubInfoTypeOwner)");
                }
                //And register it
                Constructor<?> finalConstructor = constructor;
                for (SubInfoTypeRegistries registry : an.registries()) {
                    // TODO port:1.20.1 - SubInfoTypeRegistries.getInfoList() currently returns
                    //   Object/null (Phase 0 stub). Use reflection so missing wiring degrades
                    //   gracefully until DynamXObjectLoaders.* is connected.
                    try {
                        Object infoList = registry.getInfoList();
                        if (infoList == null) {
                            DynamX.LOGGER.warn("SubInfoTypeRegistries.{} has no infoList - skipping @RegisteredSubInfoType registration for {}", registry, object);
                            continue;
                        }
                        java.lang.reflect.Method hasReg = infoList.getClass().getMethod("hasSubInfoTypesRegistry");
                        if (!((Boolean) hasReg.invoke(infoList)))
                            throw new IllegalArgumentException("No sub info type registry on registry " + registry);
                        java.lang.reflect.Method getReg = infoList.getClass().getMethod("getDefaultSubInfoTypesRegistry");
                        Object subReg = getReg.invoke(infoList);
                        if (subReg instanceof SubInfoTypesRegistry) {
                            ((SubInfoTypesRegistry) subReg).addSubInfoType(new SubInfoTypeEntry<>(an.name(), (obj, objName) -> {
                                try {
                                    return (ISubInfoType) (finalConstructor.getParameterTypes().length == 1 ? finalConstructor.newInstance(obj) : finalConstructor.newInstance(obj, objName));
                                } catch (InstantiationException | IllegalAccessException |
                                         InvocationTargetException e) {
                                    throw new RuntimeException("Error with " + name, e);
                                }
                            }, an.strictName()));
                        }
                    } catch (ReflectiveOperationException reflEx) {
                        DynamX.LOGGER.warn("Failed to register @RegisteredSubInfoType for {} on registry {}", object, registry, reflEx);
                    }
                }
                //Also register all PackFilePropertyData, if we are generating the docs
                SubInfoTypeAnnotationCache.getOrLoadData(object);
            } catch (Exception e) {
                DynamXErrorManager.addError("DynamX initialization", DynamXErrorManager.INIT_ERRORS, "addon_error", ErrorLevel.FATAL, name, "Cannot load @RegisteredSubInfoType annotation in class " + data.clazz().getClassName(), e, 900);
            }
        }
    }

    /**
     * Collects all {@link RegisteredSubInfoType} annotation data from all loaded mod files.
     *
     * TODO port:1.20.1 - Original used FMLConstructionEvent.getASMHarvestedData().getAll(name).
     *   In NeoForge, annotation scan data is exposed per ModFile via
     *   {@link ModFileScanData#getAnnotations()}. We filter to the annotation type ourselves.
     *   Wrapped in a try/catch so a missing/incompatible class loader does not crash.
     */
    private static List<ModFileScanData.AnnotationData> collectAnnotations() {
        List<ModFileScanData.AnnotationData> out = new ArrayList<>();
        Set<String> wanted = new HashSet<>();
        wanted.add("L" + RegisteredSubInfoType.class.getName().replace('.', '/') + ";");
        try {
            for (Object modFileInfo : ModList.get().getModFiles()) {
                // Reflective access keeps us tolerant to small NeoForge SPI churn between
                // 20.x point releases (IModFileInfo.getFile().getScanResult() shape varies).
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
            DynamX.LOGGER.warn("Failed to enumerate ModFileScanData for @RegisteredSubInfoType discovery", e);
        }
        return out;
    }
}
