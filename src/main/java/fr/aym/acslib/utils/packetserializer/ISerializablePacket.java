// TODO port:1.20.1 stub - ISerializablePacket marker interface
package fr.aym.acslib.utils.packetserializer;

public interface ISerializablePacket {
    default void writeTo(PacketDataSerializer buf) {
        // TODO port:1.20.1 stub
    }

    default void readFrom(PacketDataSerializer buf) {
        // TODO port:1.20.1 stub
    }
}
