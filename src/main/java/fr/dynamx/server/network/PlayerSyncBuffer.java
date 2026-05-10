package fr.dynamx.server.network;

import com.google.common.collect.Queues;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.MessageMultiPhysicsEntitySync;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.utils.optimization.PooledHashMap;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

/**
 * Manages sending of sync packets for a player, buffers and merges packets with lowest priority
 * (distant entities) to limit bandwidth, while keeping a good sync. This doc names "packet" the
 * data of an entity, but only one network packet is sent per tick.
 *
 * <p>TODO port:1.20.1 - Logic ported as-is; relies on Phase 5 sync packet plumbing.
 *
 * @author aym
 */
public class PlayerSyncBuffer {
    public static int NEW_SENDS_LIMIT = 20;
    public static int DELAYED_SENDS_LIMIT = 10;
    public static int FIRST_RADIUS = 21 * 21;
    public static int SECOND_RADIUS = 39 * 39;
    public static int MAX_SKIP = 4;
    public static int ENTITIES_PER_PACKETS = 10;

    private final ServerPlayer playerIn;
    private final Queue<SyncItem<?>> queuedPackets = Queues.newArrayDeque();
    private final List<SyncItem<?>> delayedPackets = new ArrayList<>();
    private int syncTime;

    public PlayerSyncBuffer(ServerPlayer playerIn) {
        this.playerIn = playerIn;
    }

    @SuppressWarnings("unchecked")
    public <T extends PhysicsEntity<?>> void addEntitySync(T entity, PooledHashMap<Integer, EntityVariable<?>> varsToSync) {
        SyncItem<T> sync = new SyncItem<>(entity, varsToSync);
        if (delayedPackets.contains(sync)) {
            for (SyncItem<?> s : delayedPackets) {
                if (s.equals(sync)) {
                    sync.merge((SyncItem<T>) s);
                    break;
                }
            }
            delayedPackets.remove(sync);
        }
        queuedPackets.add(sync);
    }

    public void update() {
        final Queue<MessagePhysicsEntitySync<?>> sendQueue = new ArrayDeque<>();
        if (queuedPackets.size() <= NEW_SENDS_LIMIT && delayedPackets.size() <= DELAYED_SENDS_LIMIT) {
            if (!delayedPackets.isEmpty()) {
                delayedPackets.forEach(syncItem -> syncItem.send(sendQueue));
                delayedPackets.clear();
            }
            queuedPackets.forEach(syncItem -> syncItem.send(sendQueue));
            queuedPackets.clear();
        } else {
            List<SyncItem<?>> keep = new ArrayList<>();
            if (!delayedPackets.isEmpty()) {
                delayedPackets.forEach(s -> {
                    if (s.entity.distanceToSqr(playerIn) > SECOND_RADIUS && s.skip())
                        keep.add(s);
                    else {
                        s.send(sendQueue);
                    }
                });
                delayedPackets.clear();
            }
            queuedPackets.forEach(s -> {
                if (s.entity.distanceToSqr(playerIn) > FIRST_RADIUS && s.skip())
                    keep.add(s);
                else {
                    s.send(sendQueue);
                }
            });
            queuedPackets.clear();

            while (!keep.isEmpty() && sendQueue.size() <= NEW_SENDS_LIMIT + DELAYED_SENDS_LIMIT) {
                SyncItem<?> s = keep.remove(0);
                s.send(sendQueue);
            }
            if (!keep.isEmpty()) {
                delayedPackets.addAll(keep);
                keep.clear();
            }
        }

        syncTime++;

        if (sendQueue.isEmpty()) {
            return;
        }
        if (sendQueue.size() == 1) {
            DynamXContext.getNetwork().sendToClient(sendQueue.poll(), EnumPacketTarget.PLAYER, playerIn);
            return;
        }
        while (sendQueue.size() > ENTITIES_PER_PACKETS) {
            List<MessagePhysicsEntitySync<?>> buff = new ArrayList<>();
            for (int i = 0; i < ENTITIES_PER_PACKETS; i++) {
                buff.add(sendQueue.poll());
            }
            DynamXContext.getNetwork().sendToClient(new MessageMultiPhysicsEntitySync(buff), EnumPacketTarget.PLAYER, playerIn);
        }
        DynamXContext.getNetwork().sendToClient(new MessageMultiPhysicsEntitySync(new ArrayList<>(sendQueue)), EnumPacketTarget.PLAYER, playerIn);
    }

    public void setSyncTime(int syncTime) {
        this.syncTime = syncTime;
    }

    public int getSyncTime() {
        return syncTime;
    }

    public void clear() {
        queuedPackets.clear();
        delayedPackets.clear();
    }

    @Override
    public String toString() {
        return "Buffer{" +
                "player=" + playerIn.getName().getString() +
                ", queued=" + queuedPackets.size() +
                ", delayed=" + delayedPackets.size() +
                ", syncT=" + syncTime +
                '}';
    }

    /**
     * Holds the data for one entity sync and tracks the number of times it has been delayed.
     */
    private class SyncItem<T extends PhysicsEntity<?>> {
        private final T entity;
        private final PooledHashMap<Integer, EntityVariable<?>> varsToSync;
        private int skippedSends;

        private SyncItem(T entity, PooledHashMap<Integer, EntityVariable<?>> varsToSync) {
            this.entity = entity;
            this.varsToSync = varsToSync;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void send(Queue<MessagePhysicsEntitySync<?>> sendQueue) {
            if (!entity.isRemoved()) {
                sendQueue.add(new MessagePhysicsEntitySync(entity, syncTime, varsToSync, varsToSync.size() > NEW_SENDS_LIMIT));
            }
        }

        private boolean skip() {
            if (skippedSends < MAX_SKIP)
                skippedSends++;
            return skippedSends < MAX_SKIP;
        }

        private void merge(SyncItem<T> withOlder) {
            withOlder.varsToSync.forEach((i, s) -> {
                if (!varsToSync.containsKey(i))
                    varsToSync.put(i, s);
            });
            withOlder.varsToSync.release();
            skippedSends += withOlder.skippedSends;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SyncItem<?> syncItem = (SyncItem<?>) o;
            return entity.equals(syncItem.entity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entity);
        }

        @Override
        public String toString() {
            return "{" +
                    "e=" + entity.getId() +
                    ", c=" + varsToSync.size() +
                    ", sk=" + skippedSends +
                    '}';
        }
    }
}
