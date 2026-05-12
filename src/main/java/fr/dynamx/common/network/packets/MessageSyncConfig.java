package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.utils.DynamXConfig;
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
        this.syncDelay = DynamXConfig.mountedVehiclesSyncTickRate;
        this.blockInfos = new HashMap<>(ContentPackLoader.getBlocksGrip());
        this.slopes = new ArrayList<>(ContentPackLoader.slopes);
        this.slopesLength = ContentPackLoader.SLOPES_LENGTH;
        this.slopesPlace = ContentPackLoader.PLACE_SLOPES;
        // TODO port:1.20.1 - encode IPhysicsSimulationMode via a registry index once one exists.
        // Sending 0 (full simulation) for now; the receiving end ignores until physics modes have an index.
        this.physicsSimulationMode = 0;
        this.entityId = entityId;
        // TODO port:1.20.1 - SynchronizedEntityVariableRegistry size once Phase 5 sync registry is ported.
        this.serverSynchronizedVariablesCount = 0;
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
        if (side != LogicalSide.CLIENT) {
            return;
        }
        // Apply received server settings to the local client-side mirrors used by the physics
        // entity sync layer.
        DynamXConfig.mountedVehiclesSyncTickRate = syncDelay;
        ContentPackLoader.PLACE_SLOPES = slopesPlace;
        ContentPackLoader.SLOPES_LENGTH = slopesLength;
        ContentPackLoader.getBlocksGrip().clear();
        ContentPackLoader.getBlocksGrip().putAll(blockInfos);
        ContentPackLoader.slopes.clear();
        ContentPackLoader.slopes.addAll(slopes);
        // TODO port:1.20.1 - DynamXContext.setPhysicsSimulationMode(LogicalSide.CLIENT, ...) once a
        // mode-by-index registry exists. SynchronizedEntityVariableRegistry version check and
        // connection.disconnect(Component) on mismatch also deferred to Phase 5 sync registry.
        if (reloadResources) {
            fr.dynamx.utils.DynamXLoadingTasks.reload(
                    fr.dynamx.utils.DynamXLoadingTasks.TaskContext.CLIENT,
                    fr.dynamx.utils.DynamXLoadingTasks.PACK);
        }
    }
}
