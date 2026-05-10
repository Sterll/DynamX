package fr.dynamx.common.handlers;

import fr.dynamx.utils.DynamXConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
 * <p>The annotation-bearing methods are kept with stubbed bodies so:
 *   - {@code DynamXMain.proxy.init()} can still register an instance to the bus;
 *   - the public {@code PENDING_CHUNKS_COLLISIONS} field stays available for {@code TEDynamXBlock};
 *   - the static {@code onBlockChange(...)} helper called from {@code MixinChunk} stays callable.
 */
public class CommonEventHandler {

    public static final ResourceLocation CAPABILITY_LOCATION = new ResourceLocation(DynamXConstants.ID, "chunkaabb");

    public static final Map<ChunkPos, Map<BlockPos, AABB>> PENDING_CHUNKS_COLLISIONS = new HashMap<>();

    @SubscribeEvent
    public void onChunkLoad(Object event) {
        // TODO port:1.20.1 - hook ChunkEvent.Load (net.minecraftforge.event.level.ChunkEvent.Load),
        // then transfer PENDING_CHUNKS_COLLISIONS into the chunk's DynamXChunkData attachment.
    }

    @SubscribeEvent
    public void onLoggedIn(Object event) {
        // TODO port:1.20.1 - PlayerEvent.PlayerLoggedInEvent + send MessageSyncConfig (Phase 5).
    }

    @SubscribeEvent
    public void onDisconnect(Object event) {
        // TODO port:1.20.1 - PlayerEvent.PlayerLoggedOutEvent + ServerPhysicsSyncManager (Phase 5/9).
    }

    @SubscribeEvent
    public void onStartTracking(Object event) {
        // TODO port:1.20.1 - PlayerEvent.StartTracking + resync packets (Phase 5).
    }

    /**
     * Marks the physics terrain dirty and schedules a new computation. Don't abuse — may create lag.
     * Updates are filtered by {@code ITerrainUpdateBehavior}s.
     */
    public static void onBlockChange(Level world, BlockPos pos, BlockState oldState, BlockState newState) {
        // TODO port:1.20.1 - depends on DynamXContext.usesPhysicsWorld + DynamXTerrainApi; will be wired
        // once physics world is plugged back in.
    }

    @SubscribeEvent
    public void onExplosion(Object event) {
        // TODO port:1.20.1 - ExplosionEvent.Detonate + MessageHandleExplosion (Phase 5).
    }

    @SubscribeEvent
    public void onWorldLoad(Object event) {
        // TODO port:1.20.1 - LevelEvent.Load + add per-level listener; initialise physics world.
    }

    @SubscribeEvent
    public void onChunkUnload(Object event) {
        // TODO port:1.20.1 - ChunkEvent.Unload (NeoForge); schedule terrain unload.
    }

    @SubscribeEvent
    public void onWorldUnload(Object event) {
        // TODO port:1.20.1 - LevelEvent.Unload; clear physics world.
    }

    @SubscribeEvent
    public void onRightClickBlock(Object event) {
        // TODO port:1.20.1 - PlayerInteractEvent.RightClickBlock + ItemSlopes.fixPos (Phase 4b/items).
    }

    @SubscribeEvent
    public void onRightClick(Object event) {
        // TODO port:1.20.1 - PlayerInteractEvent.RightClickItem; clear slopes memory.
    }

    @SubscribeEvent
    public void onTick(Object event) {
        // TODO port:1.20.1 - TickEvent.ServerTickEvent; clear walking player floating-tick counters.
    }

    @SubscribeEvent
    public void onPlayerUpdate(Object event) {
        // TODO port:1.20.1 - TickEvent.PlayerTickEvent + PlayerPhysicsHandler (Phase 6/Physics).
    }

    @SubscribeEvent
    public void onVehicleMount(Object event) {
        // TODO port:1.20.1 - VehicleEntityEvent.EntityMount (Phase 6 entities).
    }

    @SubscribeEvent
    public void onVehicleDismount(Object event) {
        // TODO port:1.20.1 - VehicleEntityEvent.EntityDismount (Phase 6 entities).
    }

    /**
     * Item / Block registration handler.
     *
     * <p>TODO port:1.20.1 - In 1.20.1 NeoForge, items and blocks are registered via
     * {@code DeferredRegister<Item>} and {@code DeferredRegister<Block>}, not via
     * {@code RegistryEvent.Register}. Container kept to preserve the {@code RegisterObjects} symbol;
     * actual registration must be wired into the new DeferredRegister-based pipeline.
     */
    @Mod.EventBusSubscriber(modid = DynamXConstants.ID)
    public static class RegisterObjects {
        @SubscribeEvent
        public static void registerItems(Object event) {
            // TODO port:1.20.1 - DeferredRegister<Item> based registration (Phase 3+ items).
        }

        @SubscribeEvent
        public static void registerBlocks(Object event) {
            // TODO port:1.20.1 - DeferredRegister<Block> based registration (Phase 3+ blocks).
        }
    }
}
