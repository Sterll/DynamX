package fr.dynamx.common.capability;

import fr.dynamx.utils.DynamXConstants;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Thin holder for the chunk DynamX data attachment.
 *
 * <p>In 1.12 this was an {@code ICapabilitySerializable<NBTBase>} that wrapped a single instance.
 * In 1.20.1 NeoForge, per-chunk data is provided by {@link AttachmentType} registered via a
 * {@link DeferredRegister}. We keep the same file name and the same {@code DYNAMX_CHUNK_DATA_CAPABILITY}
 * public field so the rest of the codebase can keep referring to it 1:1.
 */
public class DynamXChunkDataProvider {
    /**
     * DeferredRegister for attachment types. Must be registered with the mod event bus during
     * mod construction; see {@link #register(IEventBus)}.
     */
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, DynamXConstants.ID);

    /**
     * The attachment type holding the {@link DynamXChunkData} for a chunk.
     */
    public static final Supplier<AttachmentType<DynamXChunkData>> DYNAMX_CHUNK_DATA_ATTACHMENT =
            ATTACHMENT_TYPES.register("chunkaabb",
                    () -> AttachmentType.builder(DynamXChunkDataStorage::createEmpty)
                            .serialize(DynamXChunkDataStorage.CODEC)
                            .build());

    /**
     * Legacy-compatible field kept to preserve call sites that referenced the old
     * {@code DYNAMX_CHUNK_DATA_CAPABILITY} constant. Resolved at attachment-resolution time via
     * {@link #get(LevelChunk)}.
     *
     * TODO port:1.20.1 - Several callers use {@code chunk.getCapability(DYNAMX_CHUNK_DATA_CAPABILITY, null)}.
     * Migrate them to {@link #get(LevelChunk)} once Phase 5+ porting is done.
     */
    public static final Object DYNAMX_CHUNK_DATA_CAPABILITY = null;

    /**
     * Identifier kept for legacy code that referenced {@code CommonEventHandler.CAPABILITY_LOCATION}.
     */
    public static final ResourceLocation CAPABILITY_LOCATION = new ResourceLocation(DynamXConstants.ID, "chunkaabb");

    private DynamXChunkDataProvider() {
    }

    /**
     * Helper accessor: returns the {@link DynamXChunkData} for the given chunk, creating it if missing.
     */
    public static DynamXChunkData get(LevelChunk chunk) {
        return chunk.getData(DYNAMX_CHUNK_DATA_ATTACHMENT.get());
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
     * Registers the attachment {@link DeferredRegister} onto the mod event bus.
     */
    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
