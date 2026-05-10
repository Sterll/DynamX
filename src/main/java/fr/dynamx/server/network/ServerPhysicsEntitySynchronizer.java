package fr.dynamx.server.network;

import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SyncTarget;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.MPPhysicsEntitySynchronizer;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.PooledHashMap;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-side variant of {@link MPPhysicsEntitySynchronizer}. Schedules sync packets
 * to tracking players each physics tick.
 *
 * <p>TODO port:1.20.1 - {@code WorldServer#getEntityTracker} no longer exists; tracking players
 * are derived from {@code ServerLevel#getChunkSource().chunkMap.getPlayers(chunkPos, false)}.
 * The lookup is stubbed for now.
 */
@OnlyIn(Dist.DEDICATED_SERVER)
public class ServerPhysicsEntitySynchronizer<T extends PhysicsEntity<?>> extends MPPhysicsEntitySynchronizer<T> {
    private final Map<Integer, SyncTarget> varsToSync = new HashMap<>();
    private int updateCount = 0;

    public ServerPhysicsEntitySynchronizer(T entityIn) {
        super(entityIn);
    }

    @Override
    public void onPlayerStartControlling(Player player, boolean addControllers) {
        if (entity.physicsHandler != null)
            entity.physicsHandler.setForceActivation(true);
        ServerPhysicsSyncManager.putTime(player, 0);
        setSimulationHolder(SimulationHolder.DRIVER, player);
    }

    @Override
    public void onPlayerStopControlling(Player player, boolean removeControllers) {
        if (entity.physicsHandler != null)
            entity.physicsHandler.setForceActivation(false);
        setSimulationHolder(getDefaultSimulationHolder(), null);
    }

    @Override
    public void onPrePhysicsTick(Object profilerObj) {
        Profiler profiler = profilerObj instanceof Profiler ? (Profiler) profilerObj : Profiler.get();
        readReceivedPackets();

        profiler.start(Profiler.Profiles.PHY1);
        Vector3fPool.openPool();
        entity.prePhysicsUpdateWrapper(profiler, true);
        Vector3fPool.closePool();
        profiler.end(Profiler.Profiles.PHY1);

        if (entity.tickCount % entity.getSyncTickRate() == 0) {
            profiler.start(Profiler.Profiles.PKTSEND2);
            // TODO port:1.20.1 - replace getEntityTracker() with ServerLevel chunkSource lookup.
            if (entity.level() instanceof ServerLevel) {
                // Stubbed: iterate tracking players when port is wired.
            }
            profiler.end(Profiler.Profiles.PKTSEND2);
            updateCount++;
        }
    }

    @Override
    public void onPostPhysicsTick(Object profilerObj) {
        Profiler profiler = profilerObj instanceof Profiler ? (Profiler) profilerObj : Profiler.get();
        entity.postUpdatePhysicsWrapper(profiler, true);
    }

    @SuppressWarnings("unused")
    private void sendSyncTo(Player p, PooledHashMap<Integer, EntityVariable<?>> varsToSync) {
        if (!varsToSync.isEmpty()) {
            ServerPhysicsSyncManager.addEntitySync(p, entity, varsToSync);
        } else {
            varsToSync.release();
        }
    }

    @Override
    public void setSimulationTimeClient(int simulationTimeClient) {
        // TODO port:1.20.1 - update stored driver simulation time when getControllingPassenger() is wired.
        if (entity.getControllingPassenger() instanceof Player p) {
            ServerPhysicsSyncManager.putTime(p, simulationTimeClient - 1);
        }
    }

    @SuppressWarnings("unused")
    private LogicalSide serverSide() {
        return LogicalSide.SERVER;
    }
}
