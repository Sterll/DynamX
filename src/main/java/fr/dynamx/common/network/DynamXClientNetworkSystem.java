package fr.dynamx.common.network;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.IDnxNetworkHandler;
import fr.dynamx.api.network.IDnxNetworkSystem;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.utils.DynamXConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Client (and single player) network system.
 */
public class DynamXClientNetworkSystem implements IDnxNetworkSystem {
    private final VanillaNetworkHandler VANILLA_NETWORK;
    private IDnxNetworkHandler QUICK_NETWORK;
    private Thread clientHandlerThread;
    public boolean connected;

    public DynamXClientNetworkSystem(EnumNetworkType networkType) {
        Runtime.getRuntime().addShutdownHook(new Thread(DynamXClientNetworkSystem.this::stopNetwork));
        VANILLA_NETWORK = new VanillaNetworkHandler();
        switch (networkType) {
            case VANILLA_TCP:
            case DYNAMX_UDP:
                QUICK_NETWORK = VANILLA_NETWORK;
                break;
            default:
                throw new UnsupportedOperationException("Network type " + networkType + " isn't supported for the moment !");
        }
    }

    @Override
    public void sendToServer(IDnxPacket packet) {
        if (packet.getPreferredNetwork() == EnumNetworkType.VANILLA_TCP) {
            VANILLA_NETWORK.sendPacket(packet, EnumPacketTarget.SERVER, null);
        } else {
            QUICK_NETWORK.sendPacket(packet, EnumPacketTarget.SERVER, null);
        }
    }

    @Override
    public <T> void sendToClient(IDnxPacket packet, EnumPacketTarget<T> targetType, @Nullable T target) {
        VANILLA_NETWORK.sendPacket(packet, targetType, target);
    }

    @Override
    public VanillaNetworkHandler getVanillaNetwork() {
        return VANILLA_NETWORK;
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    @Override
    public void startNetwork() {
    }

    /**
     * Opens a connection with the remote server.
     */
    public void startNetwork(EnumNetworkType type, String hash, String ip, int udpPort) {
        if (this.isConnected()) {
            this.stopNetwork();
        }

        switch (type) {
            case VANILLA_TCP:
                QUICK_NETWORK = VANILLA_NETWORK;
                break;
            case DYNAMX_UDP:
                String serverAddress = ip;
                if (ip.isEmpty()) {
                    ServerData serverData;
                    // TODO port:1.20.1 - Minecraft.getMinecraft() → Minecraft.getInstance(); getCurrentServerData() renamed to getCurrentServer().
                    if ((serverData = Minecraft.getInstance().getCurrentServer()) != null) {
                        serverAddress = serverData.ip;
                    } else {
                        try {
                            SocketAddress address = Minecraft.getInstance().getConnection().getConnection().getRemoteAddress();
                            if (address instanceof InetSocketAddress) {
                                serverAddress = ((InetSocketAddress) address).getAddress().getHostAddress();
                            } else {
                                serverAddress = "localhost";
                            }
                        } catch (Throwable t) {
                            serverAddress = "localhost";
                        }
                    }
                }
                if (DynamXConfig.udpDebug) {
                    // TODO port:1.20.1 - DynamXMain.log not yet ported (Phase 4b).
                    System.out.println("[UDP-DEBUG] Authing with " + serverAddress);
                }
                // TODO port:1.20.1 - UdpClientNetworkHandler is part of fr.dynamx.client.network.udp (Phase 8 client).
                // Cannot instantiate here; keep using VANILLA_NETWORK until that lands.
                QUICK_NETWORK = VANILLA_NETWORK;
                break;
            default:
                throw new UnsupportedOperationException("UDP client type " + type + " not supported yet");
        }

        if (QUICK_NETWORK != VANILLA_NETWORK) {
            this.clientHandlerThread = new Thread(() -> QUICK_NETWORK.start(), "DynamX UDP Client Receiver");
            this.clientHandlerThread.setDaemon(true);
            this.clientHandlerThread.start();
        }
        this.connected = true;
        System.out.println("Connected to [" + type + "] Server.");
    }

    @Override
    public void stopNetwork() {
        this.connected = false;

        if (QUICK_NETWORK != VANILLA_NETWORK) {
            QUICK_NETWORK.stop();
        }

        if (this.clientHandlerThread != null) {
            this.clientHandlerThread.interrupt();
        }
        this.clientHandlerThread = null;
        this.QUICK_NETWORK = VANILLA_NETWORK;

        VANILLA_NETWORK.stop();
    }

    @Override
    public IDnxNetworkHandler getQuickNetwork() {
        return QUICK_NETWORK;
    }
}
