package fr.dynamx.common.network.udp;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;

public class UdpTestPacket extends UDPPacket {
    private final int id;
    private final long sendTime;
    private final long rcvTime;
    private final String sample;

    public UdpTestPacket(int id, long sendTime) {
        this.id = id;
        this.sample = "By AymericRed";
        this.sendTime = sendTime;
        this.rcvTime = -1;
    }

    public UdpTestPacket(int id, String sample, long sendTime, long rcvTime) {
        System.out.println("RCV " + id);
        this.id = id;
        this.sample = sample;
        this.sendTime = sendTime;
        this.rcvTime = rcvTime;
    }

    @Override
    public byte id() {
        return 9;
    }

    @Override
    public void write(ByteBuf var1) {
        var1.writeInt(id);
        // TODO port:1.20.1 - ByteBufUtils.writeUTF8String → FriendlyByteBuf#writeUtf
        if (var1 instanceof FriendlyByteBuf) {
            ((FriendlyByteBuf) var1).writeUtf(sample);
        } else {
            new FriendlyByteBuf(var1).writeUtf(sample);
        }
        var1.writeLong(sendTime);
        var1.writeLong(rcvTime);
    }
}
