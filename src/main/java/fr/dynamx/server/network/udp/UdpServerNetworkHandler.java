package fr.dynamx.server.network.udp;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.IDnxNetworkHandler;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.handlers.TaskScheduler;
import fr.dynamx.common.network.udp.EncapsulatedUDPPacket;
import fr.dynamx.common.network.udp.UDPPacket;
import fr.dynamx.utils.DynamXConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.Map;

/**
 * UDP server-side {@link IDnxNetworkHandler} implementation. Dispatches DynamX packets over UDP,
 * with a TCP fallback when the UDP transport is unavailable for the target.
 *
 * <p>TODO port:1.20.1 - {@code WorldServer.getEntityTracker().getTrackingPlayers(entity)} is gone in
 * 1.20.1: tracking players are retrieved via {@code ServerLevel#getChunkSource().chunkMap.getPlayers(chunkPos, false)}.
 * The lookup is stubbed for {@link EnumPacketTarget#ALL_TRACKING_ENTITY}.
 */
public class UdpServerNetworkHandler implements IDnxNetworkHandler {
    public final Map<String, ServerPlayer> waitingAuth = new HashMap<>();

    public static volatile boolean running;
    private UDPServerPacketHandler handler;
    public Map<Integer, UDPClient> clientMap;
    private UdpServer server;

    public UdpServerNetworkHandler() {
    }

    public void closeConnection(int id) {
        UDPClient client = this.clientMap.get(id);
        if (client != null) {
            this.handler.closeConnection(client.socketAddress);
        }
        this.clientMap.remove(id);
    }

    public void sendPacket(UDPPacket packet, UDPClient client) {
        ByteBuf packetBuffer = Unpooled.buffer();
        packetBuffer.writeByte(packet.id());
        packet.write(packetBuffer);
        byte[] data = packetBuffer.array();

        try {
            this.server.send(new DatagramPacket(data, data.length, client.socketAddress));
            if (DynamXConfig.udpDebug) {
                DynamXMain.log.info("[UDP-DEBUG] Sent the packet {}", packet.id());
            }
        } catch (IOException e) {
            DynamXMain.log.error("Error while sending udp packet " + packet + " to " + client + ". Disconnecting the client.", e);
            // Schedule on server thread to avoid concurrent modification issues.
            TaskScheduler.schedule(new TaskScheduler.ScheduledTask((short) 0) {
                @Override
                public void run() {
                    if (client.player.connection != null && !client.player.hasDisconnected()) {
                        client.player.connection.disconnect(Component.literal("DynamX mod had an unexpected udp error. Please try to reconnect."));
                    }
                }
            });
        }
    }

    @Override
    public boolean start() {
        this.clientMap = new HashMap<>();
        this.handler = new UDPServerPacketHandler(this);
        MinecraftServer mc = ServerLifecycleHooks.getCurrentServer();

        if (mc != null && mc.isDedicatedServer()) {
            String hostname = mc.getLocalIp();
            if (hostname == null || hostname.isEmpty()) {
                this.server = new UdpServer(DynamXMain.log, DynamXConfig.udpPort);
            } else {
                this.server = new UdpServer(DynamXMain.log, hostname, DynamXConfig.udpPort);
                DynamXMain.log.info("[UDP-Server] Applied custom IP " + hostname);
            }
        } else {
            this.server = new UdpServer(DynamXMain.log, "localhost", DynamXConfig.udpPort);
        }

        this.server.addUdpServerListener(evt -> {
            try {
                UdpServerNetworkHandler.this.handler.read(evt.getPacketAsBytes(), evt.getPacket());
            } catch (Exception e) {
                DynamXMain.log.error("Error while reading udp packet from " + evt.getPacket().getSocketAddress(), e);
            }
        });
        this.server.start();
        return true;
    }

    @Override
    public void stop() {
        if (running) {
            running = false;
            if (handler != null)
                this.handler.close();
            this.server.clearUdpListeners();
            this.server.stop();
            this.clientMap.clear();
            this.handler = null;
            this.server = null;
        }
    }

    @Override
    public <T> void sendPacket(IDnxPacket packet, EnumPacketTarget<T> targetType, @Nullable T target) {
        UDPPacket pck = new EncapsulatedUDPPacket(packet);
        if (EnumPacketTarget.SERVER == targetType) {
            throw new IllegalArgumentException("Cannot send a packet to the server, from the server !");
        } else if (EnumPacketTarget.PLAYER == targetType) {
            UDPClient client = clientMap.get(((Entity) target).getId());
            if (client == null)
                vanillaFallback(packet, (ServerPlayer) target);
            else
                sendPacket(pck, client);
        } else if (EnumPacketTarget.ALL_AROUND == targetType) {
            throw new UnsupportedOperationException("Not implemented yet in UDP, please contact DynamX devs");
        } else if (EnumPacketTarget.ALL_TRACKING_ENTITY == targetType) {
            // TODO port:1.20.1 - replace getEntityTracker().getTrackingPlayers() with ServerLevel chunk-map lookup.
            Entity entity = (Entity) target;
            if (entity.level() instanceof ServerLevel) {
                // Stubbed: iterate tracking players when port is wired.
            }
        } else if (EnumPacketTarget.ALL == targetType) {
            MinecraftServer mc = ServerLifecycleHooks.getCurrentServer();
            if (mc != null) {
                mc.getPlayerList().getPlayers().forEach(player -> {
                    UDPClient client = clientMap.get(player.getId());
                    if (client == null)
                        vanillaFallback(packet, player);
                    else
                        sendPacket(pck, client);
                });
            }
        }
    }

    @Override
    public EnumNetworkType getType() {
        return EnumNetworkType.DYNAMX_UDP;
    }

    private void vanillaFallback(IDnxPacket packet, ServerPlayer target) {
        if (target != null && target.connection != null && target.connection.getConnection().isConnected()) {
            DynamXContext.getNetwork().getVanillaNetwork().sendPacket(packet, EnumPacketTarget.PLAYER, target);
        }
    }
}
