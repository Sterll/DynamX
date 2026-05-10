package fr.dynamx.common.network.udp;

import io.netty.buffer.ByteBuf;

/**
 * Base class for raw UDP packets used by DynamX's optional UDP sync layer.
 */
// TODO port:1.20.1 - UDP layer is opt-in and independent from the Forge/NeoForge networking;
// minimal migration: just the import shuffles.
public abstract class UDPPacket {
    public abstract byte id();

    public abstract void write(ByteBuf var1);
}
