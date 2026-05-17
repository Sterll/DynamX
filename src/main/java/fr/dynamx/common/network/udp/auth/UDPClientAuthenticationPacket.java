package fr.dynamx.common.network.udp.auth;

import fr.dynamx.common.DynamXMain;
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
        if (DynamXConfig.udpDebug)
            DynamXMain.log.info("[UDP-DEBUG] Writing auth RQ !");
        FriendlyByteBuf fb = (out instanceof FriendlyByteBuf) ? (FriendlyByteBuf) out : new FriendlyByteBuf(out);
        fb.writeUtf(hash);
    }
}
