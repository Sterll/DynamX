package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.utils.VerticalChunkPos;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.LogicalSide;

public class MessageUpdateChunk implements IDnxPacket {
    private VerticalChunkPos[] chunksToUpdate;

    public MessageUpdateChunk() {
    }

    public MessageUpdateChunk(VerticalChunkPos[] chunksToUpdate) {
        this.chunksToUpdate = chunksToUpdate;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        int controlSum = chunksToUpdate.length;
        buf.writeInt(controlSum);
        for (VerticalChunkPos pos : chunksToUpdate) {
            buf.writeInt(pos.x);
            buf.writeInt(pos.y);
            buf.writeInt(pos.z);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        chunksToUpdate = new VerticalChunkPos[size];
        for (int i = 0; i < size; i++) {
            chunksToUpdate[i] = new VerticalChunkPos(buf.readInt(), buf.readInt(), buf.readInt());
        }
    }

    public static void handle(MessageUpdateChunk message /*, IPayloadContext ctx */) {
        try {
            Player p = net.minecraft.client.Minecraft.getInstance().player;
            message.handleUDPReceive(p, LogicalSide.CLIENT);
        } catch (Throwable t) {
            // server side or no client
        }
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        // TODO port:1.20.1 - Re-port body: DynamXContext.getPhysicsWorld(context.level()), Vector3fPool,
        // ChunkLoadingTicket/ChunkGraph debug, physicsWorld.getTerrainManager().onChunkChanged(pos).
    }
}
