package fr.dynamx.common.physics.terrain.chunk;

import fr.dynamx.utils.VerticalChunkPos;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class DebugChunkCollisions extends ChunkCollisions {
    private final UUID id;

    public DebugChunkCollisions(Level mcWorld, VerticalChunkPos pos) {
        super(mcWorld, pos);
        id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        return "DebugChunkCollisions[x=" + getPos().x + ";y=" + getPos().y + ";z=" + getPos().z + "] with id " + id + " and state " + getChunkState();
    }
}
