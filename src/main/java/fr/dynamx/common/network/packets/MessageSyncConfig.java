package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.LogicalSide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageSyncConfig implements IDnxPacket {
    private boolean reloadResources;
    private int syncDelay;
    private Map<Block, float[]> blockInfos;
    private List<Block> slopes;
    private int slopesLength;
    private boolean slopesPlace;
    private int physicsSimulationMode;
    private int entityId;
    private int serverSynchronizedVariablesCount;

    public MessageSyncConfig() {
    }

    public MessageSyncConfig(boolean reloadResources, int entityId) {
        this.reloadResources = reloadResources;
        // TODO port:1.20.1 - DynamXConfig.mountedVehiclesSyncTickRate is available; rely on Phase 0 config port.
        this.syncDelay = 1;
        this.blockInfos = new HashMap<>();
        this.slopes = new ArrayList<>();
        this.slopesLength = 0;
        this.slopesPlace = false;
        this.physicsSimulationMode = 0;
        this.entityId = entityId;
        this.serverSynchronizedVariablesCount = 0;
        // TODO port:1.20.1 - Populate from ContentPackLoader.getBlocksGrip(), slopes, SLOPES_LENGTH,
        // PLACE_SLOPES, DynamXContext.getPhysicsSimulationMode, SynchronizedEntityVariableRegistry size
        // once ContentPackLoader API is fully wired.
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(slopesPlace);
        buf.writeInt(slopesLength);
        buf.writeBoolean(reloadResources);
        buf.writeInt(syncDelay);
        buf.writeInt(blockInfos.size());
        // TODO port:1.20.1 - Block.getIdFromBlock removed; use BuiltInRegistries.BLOCK.getId(b) in 1.20.1.
        blockInfos.forEach((b, f) -> {
            buf.writeInt(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getId(b));
            for (float f1 : f) buf.writeFloat(f1);
        });
        buf.writeInt(slopes.size());
        slopes.forEach(b -> buf.writeInt(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getId(b)));
        buf.writeInt(physicsSimulationMode);
        buf.writeInt(entityId);
        buf.writeInt(serverSynchronizedVariablesCount);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        slopesPlace = buf.readBoolean();
        slopesLength = buf.readInt();
        reloadResources = buf.readBoolean();
        syncDelay = buf.readInt();
        blockInfos = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            Block b = net.minecraft.core.registries.BuiltInRegistries.BLOCK.byId(buf.readInt());
            float[] d = new float[]{buf.readFloat(), buf.readFloat()};
            if (b != null) blockInfos.put(b, d);
        }
        slopes = new ArrayList<>();
        size = buf.readInt();
        for (int i = 0; i < size; i++) {
            Block b = net.minecraft.core.registries.BuiltInRegistries.BLOCK.byId(buf.readInt());
            if (b != null) slopes.add(b);
        }
        physicsSimulationMode = buf.readInt();
        entityId = buf.readInt();
        serverSynchronizedVariablesCount = buf.readInt();
    }

    public static void handle(MessageSyncConfig message /*, IPayloadContext ctx */) {
        message.handleUDPReceive(null, LogicalSide.CLIENT);
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        // TODO port:1.20.1 - Re-port body: schedule on client thread, update DynamXConfig,
        // DynamXLoadingTasks.reload, ContentPackLoader, DynamXContext.setPhysicsSimulationMode,
        // server-side entityId rebinding, version-count check + connection.closeChannel(Component).
    }
}
