// TODO port:1.20.1 stub - NBTSerializer helper
package fr.aym.acslib.utils.nbtserializer;

import fr.aym.acslib.utils.DeserializedData;
import net.minecraft.nbt.CompoundTag;

import java.util.Collections;

public class NBTSerializer {
    public static void serialize(CompoundTag tag, ISerializable target) {
        // TODO port:1.20.1 stub
    }

    /**
     * Single-argument serializer used by legacy call sites that wrap the result in a CompoundTag.
     */
    public static CompoundTag serialize(ISerializable target) {
        // TODO port:1.20.1 stub
        return new CompoundTag();
    }

    public static DeserializedData deserialize(CompoundTag tag, ISerializable target) {
        // TODO port:1.20.1 stub
        return new DeserializedData(0, Collections.emptyList());
    }

    public static CompoundTag toNBT(ISerializable target) {
        // TODO port:1.20.1 stub
        return new CompoundTag();
    }

    public static <T extends ISerializable> T fromNBT(CompoundTag tag, T target) {
        // TODO port:1.20.1 stub
        return target;
    }

    /**
     * Convert helper for serialized raw values; primarily used to coerce Object -> boolean/int/...
     *
     * TODO port:1.20.1 - real implementation lives in acslib; for now we just identity-cast.
     */
    @SuppressWarnings("unchecked")
    public static <T> T convert(Object raw) {
        return (T) raw;
    }
}
