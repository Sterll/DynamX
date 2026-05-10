package fr.dynamx.common.network.sync;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.EntityVariableSerializer;
import fr.dynamx.api.network.sync.SynchronizedEntityVariableRegistry;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.packets.PhysicsEntityMessage;
import fr.dynamx.common.network.sync.variables.SynchronizedEntityVariableSnapshot;
import fr.dynamx.utils.optimization.HashMapPool;
import fr.dynamx.utils.optimization.PooledHashMap;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/**
 * Per-entity sync packet bundling a set of {@link EntityVariable} updates.
 */
public class MessagePhysicsEntitySync<T extends PhysicsEntity<?>> extends PhysicsEntityMessage<MessagePhysicsEntitySync<T>> {
    private Map<Integer, EntityVariable<?>> varsToSend;
    @Getter
    private PooledHashMap<Integer, SynchronizedEntityVariableSnapshot<?>> varsToRead;
    private int simulationTimeClient;

    private final boolean doSizeTrack = false;
    private boolean lightData;
    private T targetEntity;

    public MessagePhysicsEntitySync() {
        super(null);
    }

    public MessagePhysicsEntitySync(T entity, int simulationTimeClient, Map<Integer, EntityVariable<?>> varsToSync, boolean lightData) {
        super(entity);
        this.targetEntity = entity;
        this.varsToSend = varsToSync;
        this.simulationTimeClient = simulationTimeClient;
        this.lightData = lightData;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);
        buf.writeInt(simulationTimeClient);
        buf.writeInt(varsToSend.size());
        for (Map.Entry<Integer, EntityVariable<?>> entry : varsToSend.entrySet()) {
            Integer i = entry.getKey();
            EntityVariable<Object> v = (EntityVariable<Object>) entry.getValue();
            buf.writeInt(i);
            v.writeValue(buf, lightData);
            v.setChanged(false);
        }
        if (varsToSend instanceof PooledHashMap) {
            ((PooledHashMap<Integer, EntityVariable<?>>) varsToSend).release();
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        super.fromBytes(buf);
        simulationTimeClient = buf.readInt();
        varsToRead = HashMapPool.get();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            int id = buf.readInt();
            EntityVariableSerializer<?> serializer = SynchronizedEntityVariableRegistry.getSerializerMap().get(id);
            if (serializer == null)
                throw new IllegalArgumentException("Serializer not found for id " + id + " in " + SynchronizedEntityVariableRegistry.getSerializerMap() + ". Variable is " + SynchronizedEntityVariableRegistry.getSyncVarRegistry().inverse().get(id));
            SynchronizedEntityVariableSnapshot<?> v = new SynchronizedEntityVariableSnapshot<>(serializer, null);
            v.read(buf);
            varsToRead.put(id, v);
        }
    }

    @Override
    protected void processMessageClient(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, Player player) {
        ((MPPhysicsEntitySynchronizer<?>) entity.getSynchronizer()).receiveEntitySyncPacket((MessagePhysicsEntitySync) message);
    }

    @Override
    protected void processMessageServer(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, Player player) {
        ((MPPhysicsEntitySynchronizer<?>) entity.getSynchronizer()).receiveEntitySyncPacket((MessagePhysicsEntitySync) message);
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.DYNAMX_UDP;
    }

    public int getSimulationTimeClient() {
        return simulationTimeClient;
    }

    @Override
    public String toString() {
        return "MessagePhysicsEntitySync{" +
                "varsToSend=" + varsToSend +
                ", varsToRead=" + varsToRead +
                ", simulationTimeClient=" + simulationTimeClient +
                ", lightData=" + lightData +
                ", targetEntity=" + targetEntity +
                '}';
    }

    /**
     * Legacy generics workaround retained.
     */
    public static class Handler {
        public static void handle(MessagePhysicsEntitySync<?> message /*, IPayloadContext ctx */) {
            // TODO port:1.20.1 - delegate to PhysicsEntityMessage#handle once PayloadRegistrar is wired.
            PhysicsEntityMessage.handle(message);
        }
    }
}
