package fr.dynamx.server.network.udp;

import net.minecraft.server.level.ServerPlayer;

import java.math.BigInteger;
import java.net.InetSocketAddress;

/**
 * Represents an authenticated UDP client (Minecraft {@link ServerPlayer} + their socket address).
 */
public class UDPClient {
    public ServerPlayer player;
    public InetSocketAddress socketAddress;
    private final int key;

    UDPClient(ServerPlayer player, InetSocketAddress socketAddress, String hash) {
        this.player = player;
        this.socketAddress = socketAddress;
        this.key = (int) (new BigInteger(hash.replaceAll("[^0-9.]", ""))).longValue();
    }

    @Override
    public String toString() {
        return "Client[" + this.socketAddress + ": " + this.key + ", " + this.player + "]";
    }
}
