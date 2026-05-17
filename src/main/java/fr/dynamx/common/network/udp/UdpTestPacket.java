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
        FriendlyByteBuf fb = (var1 instanceof FriendlyByteBuf) ? (FriendlyByteBuf) var1 : new FriendlyByteBuf(var1);
        fb.writeUtf(sample);
        var1.writeLong(sendTime);
        var1.writeLong(rcvTime);
    }
}
