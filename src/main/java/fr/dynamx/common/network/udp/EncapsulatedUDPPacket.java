package fr.dynamx.common.network.udp;

import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.network.DynamXNetwork;
import fr.dynamx.utils.DynamXConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;

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
        if (DynamXConfig.udpDebug)
            DynamXMain.log.info("[UDP-DEBUG] Write packet {} {}", message, id());
        message.toBytes(bu);
    }

    public static void readAndHandle(byte id, ByteBuf data, Player player) {
        IDnxPacket packet = DynamXNetwork.getUdpPacketById(id - 10);
        if (DynamXConfig.udpDebug)
            DynamXMain.log.info("[UDP-DEBUG] Read packet {} {}", packet, id);
        if (packet == null)
            return;
        packet.fromBytes(data);
        boolean isClient = player.level().isClientSide;
        packet.handleUDPReceive(player, isClient ? LogicalSide.CLIENT : LogicalSide.SERVER);
    }
}
