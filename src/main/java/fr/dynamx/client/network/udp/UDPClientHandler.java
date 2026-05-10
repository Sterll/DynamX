package fr.dynamx.client.network.udp;

import fr.dynamx.client.command.CmdUdpTest;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.network.udp.EncapsulatedUDPPacket;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.UDPByteArrayPool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * <p>TODO port:1.20.1 - minor migration:</p>
 * <ul>
 *   <li>{@code Minecraft.getMinecraft()} -> {@code Minecraft.getInstance()}.</li>
 *   <li>{@code TextComponentString} -> {@code Component.literal}.</li>
 *   <li>{@code ByteBufUtils.readUTF8String} -> read length-prefixed UTF8 ourselves (Forge's helper is gone in 1.20).
 *       We delegate to {@link DynamXUtils#readUtf8String(ByteBuf)} so the legacy wire format is preserved.</li>
 *   <li>{@code closeChannel} -> {@code Connection.disconnect(Component)}.</li>
 * </ul>
 */
public class UDPClientHandler implements Runnable {
    final LinkedBlockingQueue<byte[]> packetQueue;
    private final UdpClientNetworkHandler client;
    private final long startTime;

    UDPClientHandler(UdpClientNetworkHandler client) {
        this.client = client;
        this.packetQueue = new LinkedBlockingQueue<>();
        this.startTime = System.currentTimeMillis();
    }

    private void handleAuthComplete() {
        this.client.handleAuth();
    }

    public void read(byte[] data) {
        ByteBuf in = Unpooled.wrappedBuffer(data);
        byte id = in.readByte();

        if (id == 0)
            this.handleAuthComplete();
        else if (id == 9) {
            int testId = in.readInt();
            String sample = DynamXUtils.readUtf8String(in);
            long sentTime = in.readLong() - CmdUdpTest.startTime;
            long rcvTime = in.readLong() - CmdUdpTest.startTime;
            long clientRcv = System.currentTimeMillis() - CmdUdpTest.startTime;
            CmdUdpTest.received[testId] = true;
            System.out.println("Packet " + testId + " sent at " + sentTime + " received on srv at " + rcvTime + " on client at " + clientRcv);
        } else {
            if (id >= 10) {
                if (Minecraft.getInstance().player != null)
                    EncapsulatedUDPPacket.readAndHandle(id, in, Minecraft.getInstance().player);
            } else
                throw new IllegalArgumentException("Illegal dynamx packet id " + id);
        }
        UDPByteArrayPool.getINSTANCE().free(data);
    }

    @Override
    public void run() {
        while (UdpClientNetworkHandler.running) {
            if (DynamXConfig.udpDebug)
                DynamXMain.log.info("Looping to handle " + packetQueue);
            if (!this.packetQueue.isEmpty()) {
                try {
                    this.read(this.packetQueue.poll());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                synchronized (this) {
                    try {
                        if (client.isAuthenticated())
                            this.wait(1000);
                        else
                            this.wait(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!client.isAuthenticated() && (System.currentTimeMillis() - startTime) > 30000) {
                DynamXMain.log.fatal("Failed to establish an UDP connection : timed out (0x2)");
                client.stop();
                if (DynamXConfig.doUdpTimeOut && Minecraft.getInstance().getConnection() != null)
                    Minecraft.getInstance().getConnection().getConnection().disconnect(Component.literal("DynamX UDP connection timed out (Auth started)"));
            }
        }
    }
}
