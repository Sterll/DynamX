package fr.dynamx.common;

import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.handlers.CommonEventHandler;
import fr.dynamx.common.network.sync.PhysicsEntitySynchronizer;
import fr.dynamx.common.network.sync.SPPhysicsEntitySynchronizer;
import fr.dynamx.common.physics.PhysicsTickHandler;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.common.physics.world.BuiltinPhysicsWorld;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.common.MinecraftForge;

/**
 * Server-side root proxy. Mirrors the legacy 1.12 {@code CommonProxy}; client side overrides this
 * in {@code fr.dynamx.client.ClientProxy} and server side in {@code fr.dynamx.server.ServerProxy}.
 *
 * <p>TODO port:1.20.1 - Several legacy entry points are touched here:
 * <ul>
 *   <li>{@code GameRegistry.registerTileEntity} -> in 1.20.1, {@code BlockEntityType}s are
 *       registered with a {@code DeferredRegister<BlockEntityType<?>>} on the mod bus. The
 *       {@code TEDynamXBlock.TYPE} placeholder must be wired up there, not here.</li>
 *   <li>{@code FMLServerHandler.instance().getServer().getEntityWorld()} -> use
 *       {@code ServerLifecycleHooks.getCurrentServer().overworld()} (or whichever level is the
 *       "current" one) instead.</li>
 *   <li>{@code world.provider.getDimension()} (int) -> 1.20.1 uses
 *       {@code Level#dimension()} (a {@code ResourceKey<Level>}). The
 *       {@code PHYSICS_WORLD_PER_DIMENSION} map needs to be re-keyed; for now we use the
 *       int hash of the dimension key.</li>
 *   <li>{@code FMLCommonHandler.instance().getMinecraftServerInstance().getTickCounter()} ->
 *       {@code ServerLifecycleHooks.getCurrentServer().getTickCount()}.</li>
 * </ul>
 */
public abstract class CommonProxy {

    public void preInit() {
        // TODO port:1.20.1 - BlockEntityType registration is now done via DeferredRegister on
        //   the mod bus (see TEDynamXBlock.TYPE). Nothing to do here anymore.
    }

    public void init() {
        MinecraftForge.EVENT_BUS.register(new PhysicsTickHandler());
        MinecraftForge.EVENT_BUS.register(new CommonEventHandler());
    }

    public void completeInit() {
    }

    /**
     * @return The client world, if loaded
     */
    public Level getClientWorld() {
        return null;
    }

    /**
     * @return The server world (overworld), if loaded
     */
    public Level getServerWorld() {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.overworld() : null;
    }

    /**
     * @return True if the bullet physics engine should be used for the world. Always true except for client single player worlds
     */
    public boolean shouldUseBulletSimulation(Level world) {
        return world != null && DynamXContext.getPhysicsWorldPerDimensionMap().containsKey(world.dimension());
    }

    /**
     * @return The {@link AbstractEntityPhysicsHandler} for the given entity, according to the side and game type (solo or multi)
     */
    public <T extends AbstractEntityPhysicsHandler<?, ?>> PhysicsEntitySynchronizer<? extends PhysicsEntity<T>> getNetHandlerForEntity(PhysicsEntity<T> tPhysicsEntity) {
        // Does not work at all on dedicated servers or in lan games — overridden in ServerProxy.
        return new SPPhysicsEntitySynchronizer<>(tPhysicsEntity, LogicalSide.SERVER);
    }

    /**
     * @return The minecraft server's tick counter
     */
    public int getTickTime() {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.getTickCount() : 0;
    }

    /**
     * @param entity The entity to test
     * @return True if the current side is playing a simulation of this entity
     */
    public abstract boolean ownsSimulation(PhysicsEntity<?> entity);

    /**
     * Schedules the given task in the client or server threads, according to the given world's side.
     */
    public abstract void scheduleTask(Level mcWorld, Runnable task);

    /**
     * Creates the physics world.
     */
    public void initPhysicsWorld(Level world) {
        // TODO port:1.20.1 - now keyed by ResourceKey<Level>.
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key = world.dimension();
        if (DynamXContext.getPhysicsWorldPerDimensionMap().containsKey(key)) {
            DynamXMain.log.warn("Physics world of " + world + " is already loaded ! Keeping the previously loaded world.");
            return;
        }
        DynamXContext.getPhysicsWorldPerDimensionMap().put(key, new BuiltinPhysicsWorld(world, false));
    }

    public abstract void schedulePacksInit();

    /**
     * Replacement for the legacy {@code world.provider.getDimension()} int key. We hash the
     * dimension {@code ResourceKey} so the map stays keyed by int. Long-term this should
     * change to a {@code ResourceKey<Level>} key.
     */
    protected static int dimensionKey(Level world) {
        // TODO port:1.20.1 - migrate PHYSICS_WORLD_PER_DIMENSION to a ResourceKey<Level> key.
        if (world == null) return 0;
        if (world instanceof ServerLevel sl) return sl.dimension().location().hashCode();
        return world.dimension().location().hashCode();
    }

    @SuppressWarnings("unused")
    private static Dist dynamX$keepDistImport() {
        return Dist.DEDICATED_SERVER;
    }
}
