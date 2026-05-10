package fr.dynamx.server.network;

import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.PooledHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds player-physics sync buffers, one per connected player.
 *
 * <p>TODO port:1.20.1 - Logic ported as-is; expects {@link ServerPlayer} keys at runtime.
 */
public class ServerPhysicsSyncManager {
    private static final Map<Player, PlayerSyncBuffer> sendBuffers = new HashMap<>();

    public static void tick(Profiler profiler) {
        profiler.start(Profiler.Profiles.SYNC_BUFFER_UPDATE);
        sendBuffers.values().forEach(PlayerSyncBuffer::update);
        profiler.end(Profiler.Profiles.SYNC_BUFFER_UPDATE);
    }

    public static String toDebugString() {
        return sendBuffers.values().toString();
    }

    public static void putTime(Player player, int time) {
        if (sendBuffers.containsKey(player))
            sendBuffers.get(player).setSyncTime(time);
    }

    public static int getTime(Player player) {
        if (sendBuffers.containsKey(player))
            return sendBuffers.get(player).getSyncTime();
        return 0;
    }

    public static void onDisconnect(Player player) {
        if (sendBuffers.containsKey(player))
            sendBuffers.remove(player).clear();
    }

    public static <T extends PhysicsEntity<?>> void addEntitySync(Player target, T entity, PooledHashMap<Integer, EntityVariable<?>> varsToSync) {
        if (!sendBuffers.containsKey(target)) {
            if (!(target instanceof ServerPlayer)) {
                // Cannot create a sync buffer for non-server players.
                varsToSync.release();
                return;
            }
            sendBuffers.put(target, new PlayerSyncBuffer((ServerPlayer) target));
        }
        sendBuffers.get(target).addEntitySync(entity, varsToSync);
    }
}
