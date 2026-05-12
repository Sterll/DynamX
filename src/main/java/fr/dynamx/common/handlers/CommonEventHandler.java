package fr.dynamx.common.handlers;

import com.jme3.math.Vector3f;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.items.tools.ItemSlopes;
import fr.dynamx.common.network.DynamXNetwork;
import fr.dynamx.common.network.packets.MessageHandleExplosion;
import fr.dynamx.common.network.packets.MessageSyncConfig;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

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

    @SubscribeEvent
    public void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            DynamXNetwork.sendTo(new MessageSyncConfig(false, serverPlayer.getId()), serverPlayer);
        }
    }

    @SubscribeEvent
    public void onDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player != null) {
            fr.dynamx.server.network.ServerPhysicsSyncManager.onDisconnect(player);
        }
    }

    @SubscribeEvent
    public void onStartTracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof fr.dynamx.common.entities.PhysicsEntity<?> entity
                && event.getEntity() instanceof ServerPlayer serverPlayer
                && entity.getSynchronizer() != null) {
            entity.getSynchronizer().resyncEntity(serverPlayer);
        }
    }

    /**
     * Marks the physics terrain dirty and schedules a new computation. Don't abuse - may create lag.
     * Updates are filtered by {@code ITerrainUpdateBehavior}s.
     */
    public static void onBlockChange(Level world, BlockPos pos, BlockState oldState, BlockState newState) {
        // TODO port:1.20.1 - depends on DynamXContext.usesPhysicsWorld + DynamXTerrainApi; will be wired
        // once physics world is plugged back in.
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        List<Entity> affected = new ArrayList<>();
        for (Entity e : event.getAffectedEntities()) {
            if (e instanceof PhysicsEntity<?>) {
                affected.add(e);
            }
        }
        if (affected.isEmpty()) {
            return;
        }
        Vec3 pos = event.getExplosion().getPosition();
        Vector3f explosionPos = new Vector3f((float) pos.x, (float) pos.y, (float) pos.z);
        DynamXNetwork.sendToAll(new MessageHandleExplosion(explosionPos, affected));
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

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player == null) {
            return;
        }
        if (event.getItemStack().getItem() instanceof ItemSlopes slopes) {
            if (!player.isShiftKeyDown()) {
                Vec3 hit = event.getHitVec() != null ? event.getHitVec().getLocation() : player.getEyePosition(1.0F);
                Vector3fPool.openPool();
                try {
                    Vector3f pos = ItemSlopes.fixPos(event.getLevel(), hit);
                    slopes.clickedWith(event.getLevel(), player, event.getHand(), pos);
                } finally {
                    Vector3fPool.closePool();
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player == null) {
            return;
        }
        if (player.isShiftKeyDown() && event.getItemStack().getItem() instanceof ItemSlopes slopes) {
            slopes.clearMemory(event.getLevel(), player, event.getItemStack());
        }
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
