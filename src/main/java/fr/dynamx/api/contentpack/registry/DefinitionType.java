package fr.dynamx.api.contentpack.registry;

import com.google.common.collect.BiMap;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.utils.DynamXReflection;
import fr.dynamx.utils.EnumPlayerStandOnTop;
import fr.dynamx.utils.EnumSeatPlayerPosition;
import fr.dynamx.utils.RegistryNameSetter;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import fr.dynamx.utils.physics.EnumCollisionType;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;

import org.joml.Vector2f;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A DefinitionType provides a method to parse a configurable string value to the corresponding java object
 *
 * @param <T> The corresponding object's type
 * @see PackFileProperty
 */
public class DefinitionType<T> {
    // TODO port:1.20.1 - In 1.20.1 Material was removed from MC; map stays empty.
    private static final BiMap<String, Object> MATERIALS = DynamXReflection.getBlockMaterialMap();
    private static final BiMap<String, SoundType> SOUNDS = DynamXReflection.getSoundTypeMap();

    private static final Map<Class<?>, DefinitionType<?>> definitionTypes = new HashMap<>();

    /**
     * Gets the parser for this type of object
     */
    @SuppressWarnings("unchecked")
    public static <A> DefinitionType<A> getParserOf(Class<A> type) {
        return (DefinitionType<A>) definitionTypes.get(type);
    }

    private final Function<String, T> parser;
    private final Function<T, String> inverser;

    /**
     * -- GETTER --
     *
     * @return the type name of the property
     */
    @Getter
    private final String typeName;

    public DefinitionType(Class<T> type, Function<String, T> parser, String typeName) {
        this(type, parser, Object::toString, typeName);
        if (type != null && type.isArray())
            throw new IllegalArgumentException("You should add an inverser on " + type + " DefinitionType");
    }

    public DefinitionType(Class<T> type, Function<String, T> parser, Function<T, String> inverser, String typeName) {
        this.parser = parser;
        this.inverser = inverser;
        this.typeName = typeName;
        if (type != null) {
            definitionTypes.put(type, this);
        }
    }

    /**
     * Parses the given configurable string value into the corresponding object.
     *
     * TODO port:1.20.1 - PackConstants lives in fr.dynamx.common.contentpack.loader (Phase 3b);
     *   the loop applying pack constant substitutions is disabled until that class is ported.
     */
    public T getValue(String value) {
        String newValue = value;
        // PackConstants substitution is disabled until Phase 3b ports PackConstants.
        return parser.apply(newValue);
    }

    /**
     * Returns the string value of this object.
     */
    @SuppressWarnings("unchecked")
    public String toValue(Object object) {
        return inverser.apply((T) object);
    }

    /**
     * Build-in definition types for this mod.
     *
     * TODO port:1.20.1 - SOUND_EVENT, PARTICLE_TYPE, SHAPE_TYPE, MATERIAL kept here for source
     *   compatibility but their parsers are stubbed because the underlying APIs have moved
     *   (registries) or been removed (Material) in 1.20.1. PartShape lives in
     *   fr.dynamx.common.contentpack.parts (Phase 3b).
     */
    public enum DynamXDefinitionTypes {
        AUTO(null),
        INT(new DefinitionType<>(int.class, Integer::parseInt, "type.int")),
        BYTE(new DefinitionType<>(byte.class, Byte::parseByte, "type.byte")),
        FLOAT(new DefinitionType<>(float.class, Float::parseFloat, "type.float")),
        BOOL(new DefinitionType<>(boolean.class, Boolean::parseBoolean, "type.boolean")),
        STRING(new DefinitionType<>(String.class, (s) -> s, "type.string")),
        STRING_ARRAY(new DefinitionType<>(String[].class, (s) -> {
            String[] t = s.split(" ");
            String[] string_array = new String[t.length];
            System.arraycopy(t, 0, string_array, 0, string_array.length);
            return string_array;
        }, DefinitionType::arrayToString, "type.string.array")),
        STRING_ARRAY_2D(new DefinitionType<>(String[][].class, (s) -> {
            String[] t = s.split(",");
            String[][] result = new String[t.length][2];
            for (int i = 0; i < t.length; i++) {
                t[i] = t[i].trim();
                String[] ts = t[i].split(" ");
                result[i][0] = ts[0];
                if (ts.length > 1)
                    result[i][1] = ts[1];
            }
            return result;
        }, DefinitionType::arrayToString, "type.string.array2d")),
        INT_ARRAY(new DefinitionType<>(int[].class, (s) -> {
            String[] t = s.split(" ");
            int[] int_array = new int[t.length];
            for (int i = 0; i < int_array.length; i++) {
                int_array[i] = Integer.parseInt(t[i]);
            }
            return int_array;
        }, DefinitionType::arrayToString, "type.int.array")),
        FLOAT_ARRAY(new DefinitionType<>(float[].class, (s) -> {
            String[] t = s.split(" ");
            float[] int_array = new float[t.length];
            for (int i = 0; i < int_array.length; i++) {
                int_array[i] = Float.parseFloat(t[i]);
            }
            return int_array;
        }, DefinitionType::arrayToString, "type.float.array")),
        VECTOR3F_ARRAY_ORDERED(new DefinitionType<>(null, (s) -> {
            String[] t = s.split(",");
            Vector3f[] vec_array = new Vector3f[t.length];
            for (int i = 0; i < vec_array.length; i++) {
                vec_array[i] = new Vector3f(Float.parseFloat(t[i].split(" ")[0]), Float.parseFloat(t[i].split(" ")[1]), Float.parseFloat(t[i].split(" ")[2]));
            }
            return vec_array;
        }, DefinitionType::arrayToString, "type.vector3f.array")),
        VECTOR3F_ARRAY_BLENDER(new DefinitionType<>(Vector3f[].class, (s) -> {
            String[] t = s.split(", ");
            Vector3f[] vec_array = new Vector3f[t.length];
            for (int i = 0; i < vec_array.length; i++) {
                vec_array[i] = new Vector3f(Float.parseFloat(t[i].split(" ")[0]), Float.parseFloat(t[i].split(" ")[2]), Float.parseFloat(t[i].split(" ")[1]) * -1);
            }
            return vec_array;
        }, DefinitionType::arrayToString, "type.vector3f.array.blender")),
        VECTOR3F(new DefinitionType<>(Vector3f.class, (s) -> {
            String[] t = s.split(" ");
            return new Vector3f(Float.parseFloat(t[0]), Float.parseFloat(t[1]), Float.parseFloat(t[2]));
        }, v -> v.x + " " + v.y + " " + v.z, "type.vector3f")),
        QUATERNION(new DefinitionType<>(Quaternion.class, (s) -> {
            String[] t = s.split(" ");
            Quaternion q = new Quaternion(Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3]), Float.parseFloat(t[0]));
            return q.normalizeLocal();
        }, v -> v.getX() + " " + v.getY() + " " + v.getZ() + " " + v.getW(), "type.quaternion")),
        /**
         * Vector3f def, but only with x and y arguments, z=0
         */
        VECTOR3F_0Z(new DefinitionType<>(null, (s) -> {
            String[] t = s.split(" ");
            return new Vector3f(Float.parseFloat(t[0]), Float.parseFloat(t[1]), 0);
        }, v -> v.x + " " + v.y, "type.vector3f_0z")),
        /**
         * Vector3f def, but with y and z arguments inversed
         */
        VECTOR3F_INVERSED(new DefinitionType<>(null, (s) -> {
            String[] t = s.split(" ");
            return new Vector3f(Float.parseFloat(t[0]), Float.parseFloat(t[2]), Float.parseFloat(t[1]));
        }, v -> v.x + " " + v.z + " " + v.y, "type.vector3f.inverse")),
        /**
         * Vector3f def, but with (y= y* -1) and z arguments inversed - Blender format
         */
        VECTOR3F_INVERSED_Y(new DefinitionType<>(null, (s) -> {
            String[] t = s.split(" ");
            return new Vector3f(Float.parseFloat(t[0]), Float.parseFloat(t[2]), Float.parseFloat(t[1]) * -1);
        }, v -> v.x + " " + (-v.z) + " " + v.y, "type.vector3f.inverse_Y")),
        VECTOR2F(new DefinitionType<>(Vector2f.class, (s) -> {
            String[] t = s.split(" ");
            return new Vector2f(Float.parseFloat(t[0]), Float.parseFloat(t[1]));
        }, v -> v.x + " " + v.y, "type.vector2f")),
        VECTOR2F_UNIT(new DefinitionType<>(Vector2f.class, (s) -> {
            String[] t = s.split(" ");
            float[] values = new float[2];
            for (int i = 0; i < t.length; i++) {
                String str = t[i];
                values[i] = Float.parseFloat(str);
                if (str.endsWith("Deg")) {
                    values[i] = (float) Math.toRadians(Float.parseFloat(str.substring(0, str.indexOf("Deg"))));
                }
            }
            return new Vector2f(values[0], values[1]);
        }, v -> v.x + " " + v.y, "type.vector2f")),
        ITEM_RENDER_LOCATION(new DefinitionType<>(Enum3DRenderLocation.class, Enum3DRenderLocation::fromString, "type.item_render_location")),
        // TODO port:1.20.1 - SoundEvent.REGISTRY is gone; use BuiltInRegistries.SOUND_EVENT.get(rl) once registries are wired up.
        SOUND_EVENT(new DefinitionType<>(null, s -> {
            throw new UnsupportedOperationException("SOUND_EVENT parser not yet ported to 1.20.1");
        }, o -> "", "type.sound_event")),
        // TODO port:1.20.1 - EnumParticleTypes no longer exists; use ParticleTypes / registry lookups.
        PARTICLE_TYPE(new DefinitionType<>(null, s -> {
            if (s.equals("none")) return null;
            throw new UnsupportedOperationException("PARTICLE_TYPE parser not yet ported to 1.20.1");
        }, p -> p == null ? "none" : p.toString(), "type.particle")),
        COLLISION_TYPE(new DefinitionType<>(EnumCollisionType.class, EnumCollisionType::valueOf,
                p -> p == null ? EnumCollisionType.SIMPLE.name() : p.name(), "type.collision")),
        // TODO port:1.20.1 - PartShape.EnumPartType lives in fr.dynamx.common.contentpack.parts (Phase 3b)
        SHAPE_TYPE(new DefinitionType<>(null, s -> {
            throw new UnsupportedOperationException("SHAPE_TYPE parser not yet ported to 1.20.1");
        }, o -> "", "type.shapetype")),
        DYNX_RESOURCE_LOCATION(new DefinitionType<>(ResourceLocation.class, RegistryNameSetter::getDynamXModelResourceLocation, "type.resourcelocation")),
        PLAYER_STAND_ON_TOP(new DefinitionType<>(EnumPlayerStandOnTop.class, EnumPlayerStandOnTop::fromString, "type.player_stand_on_top")),
        PLAYER_SEAT_POSITION(new DefinitionType<>(EnumSeatPlayerPosition.class, EnumSeatPlayerPosition::fromString, "type.player_seat_position")),
        // TODO port:1.20.1 - Material was removed in 1.20.1. Parser kept as a no-op for source compatibility.
        MATERIAL(new DefinitionType<>(null, s -> MATERIALS.get(s.toUpperCase()), material -> {
            String inv = MATERIALS.inverse().get(material);
            return inv == null ? "" : inv;
        }, "type.material")),
        AXIS(new DefinitionType<>(DynamXPhysicsHelper.EnumPhysicsAxis.class, DynamXPhysicsHelper.EnumPhysicsAxis::fromString, "type.axis")),
        SOUND_TYPE(new DefinitionType<>(SoundType.class, (s) -> SOUNDS.get(s.toUpperCase()), sound -> SOUNDS.inverse().get(sound), "type.sound_type"));

        public final DefinitionType<?> type;

        DynamXDefinitionTypes(DefinitionType<?> type) {
            this.type = type;
        }
    }

    public static <U> String arrayToString(U[] array) {
        StringBuilder s = new StringBuilder();
        for (U a : array) {
            s.append(a.toString()).append(" ");
        }
        return s.toString().trim();
    }

    public static <U> String array2DToString(U[][] array) {
        StringBuilder s = new StringBuilder();
        for (U[] a : array) {
            s.append(arrayToString(a)).append(", ");
        }
        return s.toString().trim();
    }

    public static String arrayToString(int[] array) {
        StringBuilder s = new StringBuilder();
        for (int a : array) {
            s.append(a).append(" ");
        }
        return s.toString().trim();
    }

    public static String arrayToString(float[] array) {
        StringBuilder s = new StringBuilder();
        for (float a : array) {
            s.append(a).append(" ");
        }
        return s.toString().trim();
    }
}
