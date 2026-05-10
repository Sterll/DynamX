package fr.dynamx.common.capability;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

/**
 * NBT/Codec serializer holder for the {@link DynamXChunkData} attachment.
 *
 * <p>In 1.12 this was a {@code Capability.IStorage}. In 1.20.1 NeoForge attachments rely on a
 * {@link Codec} (or an {@code IAttachmentSerializer}). Block-AABB cache is treated as transient
 * (rebuilt at runtime by {@code TEDynamXBlock.onLoad}), so the codec serializes to an empty map.
 *
 * TODO port:1.20.1 - if persistence is needed in the future, wire a proper {@code AABB}/{@code BlockPos} codec.
 */
public final class DynamXChunkDataStorage {
    private DynamXChunkDataStorage() {
    }

    /**
     * Codec that always (de)serializes as an empty {@link DynamXChunkData} instance.
     * Matches the legacy 1.12 storage that wrote/read an empty {@code NBTTagCompound}.
     */
    public static final Codec<DynamXChunkData> CODEC = Codec.unit(DynamXChunkData::new);

    /**
     * Helper to build a fresh, empty data instance (used as the attachment default factory).
     */
    public static DynamXChunkData createEmpty() {
        return new DynamXChunkData();
    }

    /**
     * Helper kept for API parity with the 1.12 storage; the cache is rebuilt at runtime.
     */
    public static Map<BlockPos, AABB> emptyMap() {
        return new HashMap<>();
    }
}
