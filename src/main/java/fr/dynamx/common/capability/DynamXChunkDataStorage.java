package fr.dynamx.common.capability;

import net.minecraft.nbt.CompoundTag;

/**
 * Serializer helpers for {@link DynamXChunkData}. The legacy 1.12 capability wrote/read an empty
 * tag (the chunk AABB cache is rebuilt at runtime by TEDynamXBlock.onLoad), so we keep that
 * contract: writeNBT returns an empty tag, readNBT is a no-op.
 */
public final class DynamXChunkDataStorage {
    private DynamXChunkDataStorage() {
    }

    public static CompoundTag writeNBT(DynamXChunkData instance) {
        return new CompoundTag();
    }

    public static void readNBT(DynamXChunkData instance, CompoundTag tag) {
    }
}
