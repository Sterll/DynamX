package fr.dynamx.api.network.sync;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import fr.dynamx.DynamX;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A registry for all the EntityVariableSerializers, for each synchronized entity variable of each class.
 */
// TODO port:1.20.1 - Forge 1.12's ASMDataTable / FMLConstructionEvent are gone. In NeoForge 1.20.1
// discovery should use ModList.get().getAllScanData() -> ModFileScanData#getAnnotations(). The
// discoverSyncVars(event) entry point has been replaced by discoverSyncVars() with no arg, to be wired
// up in Phase 5 (network impl) when we have a proper FMLConstructionEvent equivalent.
//
// fr.dynamx.common.network.sync.PhysicsEntitySynchronizer is also unported (Phase 5); addVarsOf uses
// Object as the parameter type until then.
public class SynchronizedEntityVariableRegistry {
    private static final Map<Class<?>, List<String>> baseSyncVarRegistry = new HashMap<>();
    private static final Map<Class<?>, String> classToMod = new HashMap<>();
    private static final Map<String, Field> fieldMap = HashBiMap.create();
    @Getter
    private static final BiMap<String, Integer> syncVarRegistry = HashBiMap.create();
    @Getter
    private static final Map<Integer, EntityVariableSerializer<?>> serializerMap = new HashMap<>();

    /**
     * Discovers all the synchronized entity variables in the loaded mods by scanning for classes
     * annotated with {@link SynchronizedEntityVariable.SynchronizedPhysicsModule}.
     *
     * <p>port:1.20.1 - re-implemented using Forge's {@code ModFileScanData} (replaces the 1.12
     * ASMDataTable scan). Must be called once during mod setup, followed by {@link #sortRegistry}.
     */
    public static void discoverSyncVars() {
        org.objectweb.asm.Type annotationType = org.objectweb.asm.Type.getType(SynchronizedEntityVariable.SynchronizedPhysicsModule.class);
        int found = 0;
        for (net.minecraftforge.forgespi.language.ModFileScanData scanData : net.minecraftforge.fml.ModList.get().getAllScanData()) {
            for (net.minecraftforge.forgespi.language.ModFileScanData.AnnotationData a : scanData.getAnnotations()) {
                if (!annotationType.equals(a.annotationType())) {
                    continue;
                }
                try {
                    Class<?> clazz = Class.forName(a.clazz().getClassName(), false, SynchronizedEntityVariableRegistry.class.getClassLoader());
                    Object modid = a.annotationData().get("modid");
                    registerClass(clazz, modid != null ? modid.toString() : DynamX.MOD_ID);
                    found++;
                } catch (Throwable t) {
                    DynamX.LOGGER.error("Failed to register synchronized entity variable class " + a.clazz(), t);
                }
            }
        }
        DynamX.LOGGER.info("Discovered {} synchronized-physics-module classes", found);
    }

    /**
     * Manually registers a class (used in unit tests and by Phase 5 once the scanner is reintroduced).
     */
    public static void registerClass(Class<?> classToParse, String modid) {
        classToMod.put(classToParse, modid);
        for (Field f : classToParse.getDeclaredFields()) {
            if (!EntityVariable.class.isAssignableFrom(f.getType()) || !f.isAnnotationPresent(SynchronizedEntityVariable.class)) {
                continue;
            }
            SynchronizedEntityVariable property = f.getAnnotation(SynchronizedEntityVariable.class);
            baseSyncVarRegistry.computeIfAbsent(classToParse, k -> new ArrayList<>());
            String propName = classToParse.getSimpleName() + "." + property.name();
            DynamX.LOGGER.debug("Registered synchronized entity variable {} in {} with mod {}", propName, classToParse.getName(), modid);
            baseSyncVarRegistry.get(classToParse).add(propName);
            fieldMap.put(propName, f);
        }
        if (!baseSyncVarRegistry.containsKey(classToParse)) {
            DynamX.LOGGER.error("Failed to detect any synchronized entity variable in {}", classToParse);
        }
    }

    /**
     * Finds the index of the given variable in the given list
     *
     * @param of The variable to find
     * @param in The list to search in
     * @return The index of the variable
     */
    private static int getIndex(String of, List<String> in) {
        for (int i = 0; i < in.size(); i++) {
            if (in.get(i).equals(of)) {
                return i;
            }
        }
        throw new IllegalArgumentException(of + " is not in input list " + in);
    }

    /**
     * Finds the serializer of the given variable
     *
     * @param variableName The name of the variable
     * @return The serializer of the variable
     * @throws IllegalArgumentException If the variable is not registered
     */
    private static EntityVariableSerializer<?> findSerializer(String variableName) {
        Field f = fieldMap.get(variableName);
        ParameterizedType type;
        if (f.getGenericType() instanceof ParameterizedType) {
            type = (ParameterizedType) f.getGenericType();
        } else if (f.getType().getGenericSuperclass() instanceof ParameterizedType) {
            type = (ParameterizedType) f.getType().getGenericSuperclass();
        } else {
            throw new IllegalArgumentException("Bad entity variable " + f + " name : " + variableName);
        }
        EntityVariableSerializer<?> serializer = EntityVariableTypes.getSerializerRegistry().get(type.getActualTypeArguments()[0]);
        if (serializer == null && type.getActualTypeArguments()[0] instanceof ParameterizedType) {
            serializer = EntityVariableTypes.getSerializerRegistry().get(((ParameterizedType) type.getActualTypeArguments()[0]).getRawType());
            if (serializer == null) {
                DynamX.LOGGER.error("Cannot find serializer for entity variable {}. Tried: {}", variableName, ((ParameterizedType) type.getActualTypeArguments()[0]).getRawType());
            }
        }
        if (serializer == null) {
            DynamX.LOGGER.error("Cannot find serializer for entity variable {}. Tried: {}. Generic type is {}. Generic superclass is {}", variableName, type.getActualTypeArguments()[0], f.getGenericType(), f.getType().getGenericSuperclass());
            throw new IllegalArgumentException("Don't know how to serialize entity variable " + f + " " + type.getActualTypeArguments()[0] + " name : " + variableName);
        }
        return serializer;
    }

    /**
     * Sorts variable ids in alphabetical order
     */
    public static void sortRegistry(Predicate<String> useMod) {
        DynamX.LOGGER.debug("Sorting SynchronizedVariables registry ids...");
        DynamX.LOGGER.debug("Mod variables registry is {}", classToMod.toString());
        List<String> buff = new ArrayList<>();
        for (Class<?> res : baseSyncVarRegistry.keySet()) {
            if (useMod.test(classToMod.get(res))) {
                buff.addAll(baseSyncVarRegistry.get(res));
            }
        }
        buff.sort(Comparator.comparing(String::toString)); //Unique sorting
        syncVarRegistry.clear();
        serializerMap.clear();
        for (String res : buff) {
            int index = getIndex(res, buff);
            DynamX.LOGGER.debug("Add : {} = {}", res, index);
            syncVarRegistry.put(res, index);
            serializerMap.put(index, findSerializer(res));
        }
    }

    // TODO port:1.20.1 - First parameter should be PhysicsEntitySynchronizer<?>; replaced with Object
    // until fr.dynamx.common.network.sync.PhysicsEntitySynchronizer is ported (Phase 5). The caller
    // is expected to expose a registerVariable(int, EntityVariable<?>) method which will be invoked
    // via reflection or wired up in Phase 5.
    @SneakyThrows
    public static void addVarsOf(Object synchronizer, Object instance) {
        Class<?> clazz = instance.getClass();
        while (clazz != null) {
            if (!baseSyncVarRegistry.containsKey(clazz)) {
                clazz = clazz.getSuperclass();
                continue;
            }
            for (String variable : baseSyncVarRegistry.get(clazz)) {
                Field f = fieldMap.get(variable);
                f.setAccessible(true);
                EntityVariable<?> v = (EntityVariable<?>) f.get(instance);
                f.setAccessible(false);
                v.init(variable, findSerializer(variable));
                if (syncVarRegistry.containsKey(variable)) {
                    // The synchronizer exposes registerVariable(Integer, EntityVariable). Use Integer.class
                    // (not int.class) for the reflective lookup, otherwise getMethod throws NoSuchMethodException
                    // and no variable ever gets registered (the entity then never syncs).
                    try {
                        synchronizer.getClass()
                                .getMethod("registerVariable", Integer.class, EntityVariable.class)
                                .invoke(synchronizer, syncVarRegistry.get(variable), v);
                    } catch (NoSuchMethodException nsme) {
                        DynamX.LOGGER.error("Synchronizer {} has no registerVariable(Integer, EntityVariable) method", synchronizer);
                    }
                } else {
                    DynamX.LOGGER.error("SynchronizedVariable {} not registered !", variable);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }
}
