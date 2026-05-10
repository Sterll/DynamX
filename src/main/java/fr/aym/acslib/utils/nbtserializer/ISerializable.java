// TODO port:1.20.1 stub - ISerializable interface
package fr.aym.acslib.utils.nbtserializer;

import fr.aym.acslib.utils.DeserializedData;
import net.minecraft.nbt.CompoundTag;

public interface ISerializable {
    default int getVersion() { return 1; }

    default void populateWithNbt(CompoundTag tag) {
        // TODO port:1.20.1 stub
    }

    default void writeToNBT(CompoundTag tag) {
        // TODO port:1.20.1 stub
    }

    default void readFromNBT(DeserializedData data) {
        // TODO port:1.20.1 stub
    }
}
