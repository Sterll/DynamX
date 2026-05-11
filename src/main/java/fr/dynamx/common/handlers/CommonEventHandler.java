package fr.dynamx.common.handlers;

import fr.dynamx.utils.DynamXConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

/**
 * Central server-side event handler for DynamX.
 *
 * <p>TODO port:1.20.1 - The 1.12 version of this class hooked into many forge events
 * ({@code AttachCapabilitiesEvent<Chunk>}, {@code PlayerEvent.PlayerLoggedInEvent},
 * {@code ChunkEvent.Load/Unload}, {@code WorldEvent.Load/Unload}, {@code ExplosionEvent},
 * {@code PlayerInteractEvent}, {@code TickEvent}, {@code VehicleEntityEvent}, {@code RegistryEvent.Register}).
 * Most of those have been renamed/replaced in 1.20.1 (e.g. {@code LevelEvent}, {@code ChunkEvent},
 * {@code AttachCapabilitiesEvent} removed in favor of attachment types, etc.) and rely on classes
 * that are not yet ported (network, server, picking, content registry).
 *
 * <p>The {@code @SubscribeEvent} annotations have been intentionally removed from the stub methods:
 * Forge eagerly validates the parameter type when the class is scanned, and {@code Object} is not a
 * valid {@code Event} subtype. The method skeletons are kept as a guide for what to wire when each
 * event type is restored. The static {@code onBlockChange(...)} helper called from {@code MixinChunk}
 * stays callable.
 */
public class CommonEventHandler {

    public static final ResourceLocation CAPABILITY_LOCATION = new ResourceLocation(DynamXConstants.ID, "chunkaabb");

    public static final Map<ChunkPos, Map<BlockPos, AABB>> PENDING_CHUNKS_COLLISIONS = new HashMap<>();

    public void onChunkLoad(Object event) {
        // TODO port:1.20.1 - hook ChunkEvent.Load (net.minecraftforge.event.level.ChunkEvent.Load),
        // then transfer PENDING_CHUNKS_COLLISIONS into the chunk's DynamXChunkData attachment.
    }

    public void onLoggedIn(Object event) {
        // TODO port:1.20.1 - PlayerEvent.PlayerLoggedInEvent + send MessageSyncConfig (Phase 5).
    }

    public void onDisconnect(Object event) {
        // TODO port:1.20.1 - PlayerEvent.PlayerLoggedOutEvent + ServerPhysicsSyncManager (Phase 5/9).
    }

    public void onStartTracking(Object event) {
        // TODO port:1.20.1 - PlayerEvent.StartTracking + resync packets (Phase 5).
    }

    /**
     * Marks the physics terrain dirty and schedules a new computation. Don't abuse - may create lag.
     * Updates are filtered by {@code ITerrainUpdateBehavior}s.
     */
    public static void onBlockChange(Level world, BlockPos pos, BlockState oldState, BlockState newState) {
        // TODO port:1.20.1 - depends on DynamXContext.usesPhysicsWorld + DynamXTerrainApi; will be wired
        // once physics world is plugged back in.
    }

    public void onExplosion(Object event) {
        // TODO port:1.20.1 - ExplosionEvent.Detonate + MessageHandleExplosion (Phase 5).
    }

    public void onWorldLoad(Object event) {
        // TODO port:1.20.1 - LevelEvent.Load + add per-level listener; initialise physics world.
    }

    public void onChunkUnload(Object event) {
        // TODO port:1.20.1 - ChunkEvent.Unload (NeoForge); schedule terrain unload.
    }

    public void onWorldUnload(Object event) {
        // TODO port:1.20.1 - LevelEvent.Unload; clear physics world.
    }

    public void onRightClickBlock(Object event) {
        // TODO port:1.20.1 - PlayerInteractEvent.RightClickBlock + ItemSlopes.fixPos (Phase 4b/items).
    }

    public void onRightClick(Object event) {
        // TODO port:1.20.1 - PlayerInteractEvent.RightClickItem; clear slopes memory.
    }

    public void onTick(Object event) {
        // TODO port:1.20.1 - TickEvent.ServerTickEvent; clear walking player floating-tick counters.
    }

    public void onPlayerUpdate(Object event) {
        // TODO port:1.20.1 - TickEvent.PlayerTickEvent + PlayerPhysicsHandler (Phase 6/Physics).
    }

    public void onVehicleMount(Object event) {
        // TODO port:1.20.1 - VehicleEntityEvent.EntityMount (Phase 6 entities).
    }

    public void onVehicleDismount(Object event) {
        // TODO port:1.20.1 - VehicleEntityEvent.EntityDismount (Phase 6 entities).
    }

    /**
     * Item / Block registration container.
     *
     * <p>TODO port:1.20.1 - In 1.20.1 NeoForge, items and blocks are registered via
     * {@code DeferredRegister<Item>} and {@code DeferredRegister<Block>}, not via
     * {@code RegistryEvent.Register}. The {@code @Mod.EventBusSubscriber} annotation has been removed
     * because it eagerly scans for {@code @SubscribeEvent} methods at class load and Forge rejects
     * {@code Object} parameters. Container kept to preserve the {@code RegisterObjects} symbol; actual
     * registration must be wired into the new DeferredRegister-based pipeline.
     */
    public static class RegisterObjects {
        public static void registerItems(Object event) {
            // TODO port:1.20.1 - DeferredRegister<Item> based registration (Phase 3+ items).
        }

        public static void registerBlocks(Object event) {
            // TODO port:1.20.1 - DeferredRegister<Block> based registration (Phase 3+ blocks).
        }
    }
}
