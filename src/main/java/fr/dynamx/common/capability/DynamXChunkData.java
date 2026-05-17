package fr.dynamx.common.capability;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds collisions data of DynamX blocks in each chunk.
 * Used for destroy and interaction raytracing.
 *
 * Attached to {@link net.minecraft.world.level.chunk.LevelChunk} via the Forge 1.20.1 Capabilities
 * system; the {@link DynamXChunkDataProvider} provides instances at chunk attach time.
 */
public class DynamXChunkData {
    @Getter
    private final Map<BlockPos, AABB> blocksAABB = new HashMap<>();
}
