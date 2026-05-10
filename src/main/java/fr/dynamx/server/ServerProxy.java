package fr.dynamx.server;

import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.common.CommonProxy;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.PhysicsEntitySynchronizer;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.server.network.ServerPhysicsEntitySynchronizer;
import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.optimization.SubClassPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Dedicated-server-side proxy. 1.20.1 NeoForge has no {@code @SidedProxy} mechanism; instead
 * the boot code chooses which proxy to instantiate based on {@code Dist}.
 *
 * <p>TODO port:1.20.1 - acslib {@code ThreadedLoadingService} isn't ported; pack loading is
 * scheduled synchronously here for the moment.
 */
@OnlyIn(Dist.DEDICATED_SERVER)
public class ServerProxy extends CommonProxy {
    @Override
    public <T extends AbstractEntityPhysicsHandler<?, ?>> PhysicsEntitySynchronizer<? extends PhysicsEntity<T>> getNetHandlerForEntity(PhysicsEntity<T> tPhysicsEntity) {
        return new ServerPhysicsEntitySynchronizer<>(tPhysicsEntity);
    }

    @Override
    public boolean ownsSimulation(PhysicsEntity<?> entity) {
        return entity.getSynchronizer().getSimulationHolder() == SimulationHolder.SERVER;
    }

    @Override
    public void scheduleTask(Level mcWorld, Runnable task) {
        if (mcWorld instanceof ServerLevel sl && sl.getServer() != null) {
            sl.getServer().execute(task);
        } else {
            // TODO port:1.20.1 - fall back to direct run; ideally route to the server thread.
            task.run();
        }
    }

    @Override
    public void schedulePacksInit() {
        // TODO port:1.20.1 - acslib ThreadedLoadingService isn't ported. Run synchronously for now.
        Vector3fPool.openPool(SubClassPool.PACK_MODEL_LOAD);
        DynamXLoadingTasks.reload(DynamXLoadingTasks.TaskContext.MC_INIT, DynamXLoadingTasks.PACK);
        Vector3fPool.closePool();
        preInit();
    }
}
