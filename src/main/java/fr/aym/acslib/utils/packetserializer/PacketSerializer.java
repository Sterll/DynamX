// TODO port:1.20.1 stub - PacketSerializer helper
package fr.aym.acslib.utils.packetserializer;

public class PacketSerializer {
    public static void serialize(PacketDataSerializer<?> buf, ISerializablePacket packet) {
        // TODO port:1.20.1 stub
    }

    public static <T extends ISerializablePacket> T deserialize(PacketDataSerializer<?> buf, T packet) {
        // TODO port:1.20.1 stub
        return packet;
    }

    /**
     * Registers a custom (de)serializer for a specific type.
     *
     * TODO port:1.20.1 - acslib's PacketSerializer is not yet re-bundled for 1.20.1; this is a
     * no-op stub so DynamXNetwork can still install its Vector3f / Quaternion adapters.
     */
    public static <T> void addCustomSerializer(PacketDataSerializer<T> serializer) {
        // no-op stub
    }
}
