package fr.dynamx.common.physics.world;

import fr.dynamx.DynamX;
import fr.dynamx.api.events.PhysicsEvent;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.SubClassPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

/**
 * Where all the physics happen <br>
 * Not multithreaded but thread-safe
 */
public class BuiltinPhysicsWorld extends BasePhysicsWorld {
    protected final Thread physicsThread;
    private short serverAfkTime = 0;

    public BuiltinPhysicsWorld(Level world, boolean isRemoteWorld) {
        super(world, isRemoteWorld);
        initPhysicsWorld();
        this.physicsThread = Thread.currentThread();

        DynamX.LOGGER.info("Loading the physics world for the dimension {}", world.dimension());
        MinecraftForge.EVENT_BUS.post(new PhysicsEvent.PhysicsWorldLoad(this));
    }

    @Override
    public void stepSimulation(float deltaTime) {
        Vector3fPool.openPool(SubClassPool.PHYSICS_WORLD_STEP);
        {
            //Disable physics simulation
            //Note that minecraft does the same, but with a delay of 300, so it avoids physics while entities are paused
            if (mcWorld.players().isEmpty()) {
                if (serverAfkTime < 200) {
                    serverAfkTime++;
                }
            } else {
                serverAfkTime = 0;
            }

            if (serverAfkTime < 200) {
                stepSimulationImpl(Profiler.get(), null);
            } else {
                flushOperations(Profiler.get());
            }
        }
        Vector3fPool.closePool();

        Profiler.get().start(Profiler.Profiles.TICK_TERRAIN);
        manager.tickTerrain();
        Profiler.get().end(Profiler.Profiles.TICK_TERRAIN);
    }

    @Override
    public void clearAll() {
        DynamX.LOGGER.info("Unloading the physics world of the dimension " + mcWorld.dimension());
        super.clearAll();
    }

    @Override
    public Thread getPhysicsThread() {
        return physicsThread;
    }
}
