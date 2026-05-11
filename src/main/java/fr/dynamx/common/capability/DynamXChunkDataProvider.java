package fr.dynamx.common.capability;

import fr.dynamx.utils.DynamXConstants;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Thin holder for the chunk DynamX data attachment.
 *
 * TODO port:1.20.1 - Originally this used Forge 1.20.5+ DataAttachment API (net.minecraftforge.attachment).
 * In NeoForge 1.20.1 the canonical pattern is Capabilities; for Phase 1 we keep a compile-only stub
 * with no-op getters so the rest of the codebase still compiles. Real migration to Capabilities is
 * a Phase 5+ task. Until then, get(LevelChunk) returns null.
 */
public class DynamXChunkDataProvider {
    /**
     * Legacy-compatible field kept to preserve call sites that referenced the old
     * {@code DYNAMX_CHUNK_DATA_CAPABILITY} constant.
     *
     * TODO port:1.20.1 - Several callers use {@code chunk.getCapability(DYNAMX_CHUNK_DATA_CAPABILITY, null)}.
     * Migrate them to {@link #get(LevelChunk)} once Capabilities wiring is in place.
     */
    public static final Object DYNAMX_CHUNK_DATA_CAPABILITY = null;

    /**
     * Identifier kept for legacy code that referenced {@code CommonEventHandler.CAPABILITY_LOCATION}.
     */
    public static final ResourceLocation CAPABILITY_LOCATION = new ResourceLocation(DynamXConstants.ID, "chunkaabb");

    private DynamXChunkDataProvider() {
    }

    /**
     * Helper accessor: returns the {@link DynamXChunkData} for the given chunk.
     *
     * TODO port:1.20.1 - Returns null until Capabilities migration is done.
     */
    public static DynamXChunkData get(LevelChunk chunk) {
        // TODO port:1.20.1 - Needs Capabilities migration; returns null for now.
        return null;
    }

    /**
     * Bridge helper to preserve the 1.12 signature {@code chunk.getCapability(CAP, side)}.
     *
     * @param chunk the chunk to get data from
     * @param side  ignored (kept for source-level compatibility)
     */
    public static DynamXChunkData get(LevelChunk chunk, Direction side) {
        return get(chunk);
    }

    /**
     * Registers the attachment {@link net.minecraftforge.registries.DeferredRegister} onto the mod event bus.
     *
     * TODO port:1.20.1 - No-op for now; will hook Capabilities registration later.
     */
    public static void register(IEventBus modEventBus) {
        // no-op stub
    }
}
