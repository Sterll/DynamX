package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.utils.VerticalChunkPos;
import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;

public class MessageQueryChunks implements IDnxPacket {
    // TODO port:1.20.1 - PooledHashMap relies on optimization pool not fully ported; relax to HashMap.
    private Map<VerticalChunkPos, byte[]> requests;

    public MessageQueryChunks() {
    }

    public MessageQueryChunks(Map<VerticalChunkPos, byte[]> requests) {
        this.requests = requests;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        requests = new HashMap<>();
        for (int i = 0; i < size; i++) {
            requests.put(new VerticalChunkPos(buf.readInt(), buf.readInt(), buf.readInt()), new byte[]{buf.readByte(), buf.readByte()});
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(requests.size());
        requests.forEach((pos, dataType) -> {
            buf.writeInt(pos.x);
            buf.writeInt(pos.y);
            buf.writeInt(pos.z);
            buf.writeByte(dataType[0]);
            buf.writeByte(dataType[1]);
        });
    }

    public Map<VerticalChunkPos, byte[]> getRequests() {
        return requests;
    }

    /**
     * Legacy bi-directional Handler retained as a nested class with a static handle() entry point.
     */
    public static class Handler {
        public static void handle(MessageQueryChunks message /*, IPayloadContext ctx */) {
            // TODO port:1.20.1 - Re-port the chunk loading orchestration body.
            // It references IPhysicsWorld, ITerrainManager, ChunkLoadingTicket, ChunkGraph,
            // RemoteTerrainCache, FileTerrainCache, Profiler — most of which sit in Phase 2/8.
        }
    }
}
