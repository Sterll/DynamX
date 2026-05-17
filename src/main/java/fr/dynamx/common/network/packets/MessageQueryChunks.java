package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.physics.terrain.cache.RemoteTerrainCache;
import fr.dynamx.utils.VerticalChunkPos;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;

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

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        if (side == LogicalSide.CLIENT) {
            if (ClientEventHandler.MC == null || ClientEventHandler.MC.level == null) {
                return;
            }
            IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(ClientEventHandler.MC.level);
            if (physicsWorld == null) {
                return;
            }
            requests.forEach((pos, dataType) -> ((RemoteTerrainCache) physicsWorld.getTerrainManager().getCache())
                    .receiveChunkData(pos, dataType[0], dataType[1], null));
            return;
        }
        // TODO port:1.20.1 - server-side chunk orchestration depends on
        // IDnxNetworkSystem#sendToClientFromOtherThread which still throws UnsupportedOperationException
        // (Phase 5 network impl). The legacy body queues responses through
        // DynamXContext.getNetwork().sendToClientFromOtherThread(...). Re-port once that lands.
    }
}
