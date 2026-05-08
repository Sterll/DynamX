package fr.dynamx.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import fr.dynamx.DynamX;
import lombok.SneakyThrows;
import net.minecraft.world.level.block.SoundType;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Reflection helpers. In 1.20.1 with Mojmap most of the previously-reflected entity
 * methods are accessible directly via access transformers. This class is preserved
 * to maintain the public API for callers that haven't been ported yet.
 *
 * TODO port:1.20.1 - Replace reflection-based calls with direct mojmap method calls
 * once entity classes are ported. Add the corresponding entries to
 * src/main/resources/META-INF/accesstransformer.cfg as needed.
 */
public class DynamXReflection {
    public static Method updateFallState, dealFireDamage, canTriggerWalking, doBlockCollisions, playStepSound, makeFlySound, playFlySound;
    public static Method worldIsChunkLoaded;

    public static void initReflection() {
        DynamX.LOGGER.debug("---- Start Reflection ----");
        // TODO port:1.20.1 - resolve methods via direct mojmap names + AT entries
        // - checkFallDamage(double, boolean, BlockState, BlockPos)
        // - isMovementNoisy()
        // - checkInsideBlocks()
        // - playStepSound(BlockPos, BlockState)  (signature changed: BlockState replaces Block)
        // - playFlySound(float)
        // - makeFlySound()
        // - Level#hasChunk(int, int)
        DynamX.LOGGER.debug("---- End Reflection (stubbed for 1.20.1) ----");
    }

    public static Object invokeMethod(Method method, Object obj, Object... args) {
        if (method == null) return obj;
        try {
            return args.length > 0 ? method.invoke(obj, args) : method.invoke(obj);
        } catch (IllegalAccessException | InvocationTargetException e) {
            DynamX.LOGGER.warn("Failed reflective invoke of " + method.getName(), e);
            return obj;
        }
    }

    /**
     * TODO port:1.20.1 - In 1.20.1 there are no static Material.* fields anymore;
     * Material has been removed. Returns an empty map for now.
     */
    @SneakyThrows
    public static BiMap<String, Object> getBlockMaterialMap() {
        return HashBiMap.create();
    }

    @SneakyThrows
    public static BiMap<String, SoundType> getSoundTypeMap() {
        BiMap<String, SoundType> soundTypes = HashBiMap.create();
        Field[] fields = SoundType.class.getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers()) || !field.getType().equals(SoundType.class)) {
                continue;
            }
            soundTypes.put(field.getName(), (SoundType) field.get(null));
        }
        return soundTypes;
    }
}
