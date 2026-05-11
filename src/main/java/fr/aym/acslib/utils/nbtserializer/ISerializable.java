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

    /**
     * Returns the ordered objects to be serialized into NBT.
     */
    default Object[] getObjectsToSave() {
        return new Object[0];
    }

    /**
     * Populates this instance from previously-saved data (sequential reader).
     */
    default void populateWithSavedObjects(DeserializedData objects) {
        // no-op default
    }
}
