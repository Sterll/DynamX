// TODO port:1.20.1 stub - ISerializablePacket marker interface
package fr.aym.acslib.utils.packetserializer;

import fr.aym.acslib.utils.DeserializedData;

public interface ISerializablePacket {
    default void writeTo(PacketDataSerializer<?> buf) {
        // TODO port:1.20.1 stub
    }

    default void readFrom(PacketDataSerializer<?> buf) {
        // TODO port:1.20.1 stub
    }

    /**
     * Ordered list of values to serialize.
     */
    default Object[] getObjectsToSave() {
        return new Object[0];
    }

    /**
     * Populate this instance from previously-saved data (sequential reader).
     */
    default void populateWithSavedObjects(DeserializedData objects) {
        // no-op default
    }
}
