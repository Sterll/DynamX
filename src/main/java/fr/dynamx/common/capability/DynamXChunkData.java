package fr.dynamx.common.capability;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds collisions data of DynamX blocks in each chunk <br>
 * Used for destroy and interaction raytracing.
 *
 * <p>In 1.20.1 NeoForge this is stored as an {@code AttachmentType} on chunks (see
 * {@link DynamXChunkDataProvider}); the legacy Forge {@code Capability} system was removed.
 */
public class DynamXChunkData {
    @Getter
    private final Map<BlockPos, AABB> blocksAABB = new HashMap<>();
}
