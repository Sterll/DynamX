package fr.dynamx.common.network.udp;

import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.network.DynamXNetwork;
import fr.dynamx.utils.DynamXConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.LogicalSide;

public class EncapsulatedUDPPacket extends UDPPacket {
    private final IDnxPacket message;

    public EncapsulatedUDPPacket(IDnxPacket packet) {
        this.message = packet;
    }

    @Override
    public byte id() {
        return (byte) (10 + DynamXNetwork.getUdpMessageId(message));
    }

    @Override
    public void write(ByteBuf bu) {
        if (DynamXConfig.udpDebug) {
            // TODO port:1.20.1 - DynamXMain.log not yet ported (Phase 4b).
            System.out.println("[UDP-DEBUG] Write packet " + message + " " + id());
        }
        // TODO port:1.20.1 - IDnxPacket no longer exposes toBytes (now CustomPacketPayload-based);
        // re-wire once Phase 5 finalises payload codecs.
        // message.toBytes(bu);
    }

    public static void readAndHandle(byte id, ByteBuf data, Player player) {
        IDnxPacket packet = DynamXNetwork.getUdpPacketById(id - 10);
        if (DynamXConfig.udpDebug) {
            System.out.println("[UDP-DEBUG] Read packet " + packet + " " + id);
        }
        if (packet == null) return;
        // TODO port:1.20.1 - fromBytes removed from IDnxPacket; receive logic deferred to Phase 5.
        // packet.fromBytes(data);
        boolean isClient = player.level().isClientSide;
        packet.handleUDPReceive(player, isClient ? LogicalSide.CLIENT : LogicalSide.SERVER);
    }
}
