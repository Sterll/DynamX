package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.ChunkGraph;
import fr.dynamx.utils.optimization.Vector3fPool;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.LogicalSide;

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
        if (context == null || context.level() == null) {
            return;
        }
        Level level = context.level();
        IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(level);
        if (physicsWorld != null && DynamXMain.proxy.shouldUseBulletSimulation(level)) {
            physicsWorld.schedule(() -> {
                Vector3fPool.openPool();
                try {
                    for (VerticalChunkPos pos : chunksToUpdate) {
                        if (DynamXConfig.enableDebugTerrainManager) {
                            ChunkLoadingTicket ticket = physicsWorld.getTerrainManager().getTicket(pos);
                            if (ticket != null) {
                                ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.CHK_UPDATE,
                                        ChunkGraph.ActionLocation.UNKNOWN, ticket.getCollisions(),
                                        "Chunk changed from handleUDPReceive. Ticket " + ticket);
                            }
                        }
                        physicsWorld.getTerrainManager().onChunkChanged(pos);
                    }
                } finally {
                    Vector3fPool.closePool();
                }
            });
        } else if (DynamXConfig.enableDebugTerrainManager) {
            DynamXMain.log.info("RCV FAILZ " + physicsWorld + " "
                    + DynamXMain.proxy.shouldUseBulletSimulation(level));
        }
    }
}
