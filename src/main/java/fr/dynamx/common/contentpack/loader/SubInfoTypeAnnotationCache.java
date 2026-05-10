package fr.dynamx.common.contentpack.loader;

import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.DynamX;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal cache for loaded {@link PackFilePropertyData}.
 *
 * @see PackFileProperty
 *
 * TODO port:1.20.1 - The original used SubInfoTypeRegistries.getInfoList().getDefaultSubInfoTypesRegistry()
 *   to register pack file property fixers. SubInfoTypeRegistries currently returns Object/null
 *   from getInfoList() (Phase 0 stub). We attempt the wiring via reflection so the registration
 *   degrades gracefully until SubInfoTypeRegistries is fully connected.
 */
public class SubInfoTypeAnnotationCache {
    private static final Map<Class<?>, Map<String, PackFilePropertyData<?>>> cache = new HashMap<>();

    /**
     * Checks in the cache to find the specified property data, or loads properties of the given object's class to find it
     *
     * @param loadedObject The object containing the property
     * @param propertyName The property name
     * @return A matching {@link PackFilePropertyData}, or null if not found
     */
    @Nullable
    public static PackFilePropertyData<?> getFieldFor(INamedObject loadedObject, String propertyName) {
        return getOrLoadData(loadedObject.getClass()).get(propertyName);
    }

    /**
     * Checks in the cache to get the properties of the given class, or loads them
     *
     * @param from The class
     * @return All the properties found in the given class
     */
    @Nonnull
    public static Map<String, PackFilePropertyData<?>> getOrLoadData(Class<?> from) {
        if (!cache.containsKey(from))
            load(from);
        return cache.get(from);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void load(Class<?> classToParse) {
        Map<String, PackFilePropertyData<?>> packFileProperties = new HashMap<>();
        for (Field f : classToParse.getDeclaredFields()) {
            if (f.isAnnotationPresent(PackFileProperty.class)) {
                PackFileProperty property = f.getAnnotation(PackFileProperty.class);
                DefinitionType<?> type = property.type().type;
                if (type == null) {
                    type = DefinitionType.getParserOf(f.getType());
                }
                if (type != null) {
                    for (String configName : property.configNames()) {
                        PackFilePropertyData<?> d = new PackFilePropertyData(f, configName, property.configNames(), type, property.required(), property.description(), property.defaultValue());
                        packFileProperties.put(d.getConfigFieldName(), d);
                    }
                } else {
                    throw new IllegalArgumentException("No parser for field " + f.getName() + " of " + classToParse.getName());
                }
            }
            if (f.isAnnotationPresent(IPackFilePropertyFixer.PackFilePropertyFixer.class)) {
                if (!INamedObject.class.isAssignableFrom(classToParse)) {
                    throw new IllegalArgumentException("Only INamedObject objects can have the @PackFilePropertyFixer annotation. Errored class: " + classToParse);
                }
                try {
                    Object value = f.get(null);
                    if (!(value instanceof IPackFilePropertyFixer)) {
                        throw new IllegalArgumentException("@PackFilePropertyFixer should annotate a static IPackFilePropertyFixer field. Errored class: " + classToParse);
                    }
                    for (SubInfoTypeRegistries registry : f.getAnnotation(IPackFilePropertyFixer.PackFilePropertyFixer.class).registries()) {
                        // TODO port:1.20.1 - registry.getInfoList() currently returns Object/null until
                        //   SubInfoTypeRegistries is wired to DynamXObjectLoaders. Use reflection so
                        //   missing wiring doesn't crash classloading.
                        try {
                            Object infoList = registry.getInfoList();
                            if (infoList == null) {
                                DynamX.LOGGER.warn("SubInfoTypeRegistries.{} has no infoList - skipping property fixer registration for {}", registry, classToParse);
                                continue;
                            }
                            java.lang.reflect.Method hasReg = infoList.getClass().getMethod("hasSubInfoTypesRegistry");
                            if (!((Boolean) hasReg.invoke(infoList))) {
                                throw new IllegalArgumentException("No sub info type registry on registry " + registry);
                            }
                            java.lang.reflect.Method getReg = infoList.getClass().getMethod("getDefaultSubInfoTypesRegistry");
                            Object subReg = getReg.invoke(infoList);
                            if (subReg instanceof SubInfoTypesRegistry) {
                                ((SubInfoTypesRegistry) subReg).addSubInfoTypePropertiesFixer((Class<? extends INamedObject>) classToParse, (IPackFilePropertyFixer) value);
                            }
                        } catch (ReflectiveOperationException reflEx) {
                            DynamX.LOGGER.warn("Failed to register property fixer for {} on registry {}", classToParse, registry, reflEx);
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (classToParse.getSuperclass() != null) {
            Map<String, PackFilePropertyData<?>> dataMap = getOrLoadData(classToParse.getSuperclass());
            packFileProperties.putAll(dataMap);
        }
        cache.put(classToParse, packFileProperties);
    }
}
