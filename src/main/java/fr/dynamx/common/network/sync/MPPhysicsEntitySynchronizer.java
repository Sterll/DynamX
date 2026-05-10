package fr.dynamx.common.network.sync;

import com.google.common.collect.Queues;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.variables.SynchronizedEntityVariableSnapshot;
import lombok.Getter;

import java.util.Queue;

public abstract class MPPhysicsEntitySynchronizer<T extends PhysicsEntity<?>> extends PhysicsEntitySynchronizer<T> {
    /**
     * Ordered sync waiting packet queue: when network lags, all packets arrive in the same tick.
     */
    @Getter
    private final Queue<MessagePhysicsEntitySync<T>> receivedPackets = Queues.newArrayDeque();

    public MPPhysicsEntitySynchronizer(T entity) {
        super(entity);
    }

    public void receiveEntitySyncPacket(MessagePhysicsEntitySync<T> msg) {
        receivedPackets.offer(msg);
    }

    public void readReceivedPackets() {
        if (!receivedPackets.isEmpty()) {
            MessagePhysicsEntitySync<T> msg;
            while ((msg = receivedPackets.poll()) != null) {
                if (msg.getVarsToRead() != null) {
                    getReceivedVariables().putAll(msg.getVarsToRead());
                }
                setSimulationTimeClient(msg.getSimulationTimeClient());
                onDataReceived(msg);
            }
            getReceivedVariables().forEach((key, value) -> ((SynchronizedEntityVariableSnapshot<Object>) value).updateVariable(tryGetVariable(key)));
        }
    }

    protected void onDataReceived(MessagePhysicsEntitySync<T> msg) {
        if (msg.getVarsToRead() != null) {
            msg.getVarsToRead().release();
        }
    }

    public abstract void setSimulationTimeClient(int simulationTimeClient);
}
