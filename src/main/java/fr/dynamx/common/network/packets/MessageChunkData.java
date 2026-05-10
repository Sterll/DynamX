package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.utils.VerticalChunkPos;
import io.netty.buffer.ByteBuf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class MessageChunkData implements IDnxPacket {
    private static final byte version = 3;

    private byte[] dataType;
    private VerticalChunkPos pos;
    private byte[] data;

    public MessageChunkData() {
    }

    public MessageChunkData(VerticalChunkPos pos, byte[] dataType, byte[] data) {
        this.pos = pos;
        this.dataType = dataType;
        this.data = data;
    }

    // TODO port:1.20.1 - List<ITerrainElement> overload kept generic; ITerrainElement may not be fully
    // ported yet, so we accept List<?> and pull the legacy fields reflectively-free via interface.
    public MessageChunkData(VerticalChunkPos pos, byte[] dataType, List<?> terrainElements) {
        this.pos = pos;
        this.dataType = dataType;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(out));
            oos.writeInt(terrainElements.size());
            // TODO port:1.20.1 - Legacy wrote ITerrainElement.getFactory().ordinal() + e.save(NETWORK, out).
            // Until ITerrainElement is ported, leave size-only stub.
            oos.close();
            this.data = out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Ouch", e);
        }
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        byte ve = buf.readByte();
        if (ve != version) {
            throw new UnsupportedOperationException("Wrong encoding version, found " + ve + " and should be " + version);
        }
        pos = new VerticalChunkPos(buf.readInt(), buf.readInt(), buf.readInt());
        dataType = new byte[]{buf.readByte(), buf.readByte()};
        data = new byte[buf.readInt()];
        buf.readBytes(data);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(version);
        buf.writeInt(pos.x);
        buf.writeInt(pos.y);
        buf.writeInt(pos.z);
        buf.writeByte(dataType[0]);
        buf.writeByte(dataType[1]);
        buf.writeInt(data.length);
        buf.writeBytes(data);
    }

    /**
     * Legacy client-side Handler retained as a nested class with a static handle() entry point.
     */
    // TODO port:1.20.1 - Wire as a client-side PayloadHandler via PayloadRegistrar in Phase 5b.
    public static class Handler {
        public static void handle(MessageChunkData message /*, IPayloadContext ctx */) {
            // TODO port:1.20.1 - Re-port body using DynamXContext.getPhysicsWorld + RemoteTerrainCache once Phase 2/8 land.
        }
    }
}
