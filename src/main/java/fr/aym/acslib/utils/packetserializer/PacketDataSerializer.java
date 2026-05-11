// TODO port:1.20.1 stub - PacketDataSerializer<T> is the acslib custom-serializer SPI used by
// PacketSerializer.addCustomSerializer(...). Kept as a parametric abstract class so DynamXNetwork
// can subclass it for Vector3f / Quaternion adapters until acslib is re-bundled for 1.20.1.
package fr.aym.acslib.utils.packetserializer;

import io.netty.buffer.ByteBuf;

public abstract class PacketDataSerializer<T> {
    public abstract Class<T> objectType();
    public abstract void serialize(ByteBuf to, T value);
    public abstract T deserialize(ByteBuf from);
}
