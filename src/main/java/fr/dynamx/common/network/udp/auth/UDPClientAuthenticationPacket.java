package fr.dynamx.common.network.udp.auth;

import fr.dynamx.common.network.udp.UDPPacket;
import fr.dynamx.utils.DynamXConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;

public class UDPClientAuthenticationPacket extends UDPPacket {
    String hash;

    public UDPClientAuthenticationPacket(String hash) {
        this.hash = hash;
    }

    public byte id() {
        return (byte) 0;
    }

    public void write(ByteBuf out) {
        if (DynamXConfig.udpDebug) {
            // TODO port:1.20.1 - DynamXMain.log not yet ported (Phase 4b).
            System.out.println("[UDP-DEBUG] Writing auth RQ !");
        }
        // ByteBufUtils.writeUTF8String → FriendlyByteBuf#writeUtf
        if (out instanceof FriendlyByteBuf) {
            ((FriendlyByteBuf) out).writeUtf(hash);
        } else {
            new FriendlyByteBuf(out).writeUtf(hash);
        }
    }
}
