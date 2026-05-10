package fr.dynamx.common.slopes;

import fr.dynamx.api.physics.terrain.ITerrainManager;
import fr.dynamx.utils.debug.Profiler;

/**
 * Loads the slopes around you when you hold an {@code ItemSlopes}, so you can see them.
 *
 * <p>TODO port:1.20.1 - The legacy class extends {@code PhysicsEntityTerrainLoader} (not yet ported)
 * and reads {@code Minecraft.getMinecraft().player.chunkCoord*}. Stubbed to keep the call site in
 * {@link SlopePreviewer} compiling. Real impl will be wired with Phase 6 (physics terrain loaders).
 */
public class SlopesPreviewTerrainLoader {
    protected int lastChunkX, lastChunkY, lastChunkZ;
    protected final byte[][] loadMatrice = new byte[3][25];

    public SlopesPreviewTerrainLoader() {
        for (byte[] row : loadMatrice) {
            java.util.Arrays.fill(row, (byte) -1);
        }
    }

    public void update(ITerrainManager terrain, Profiler profiler) {
        // TODO port:1.20.1 - reimplement once PhysicsEntityTerrainLoader is ported.
    }

    public void onRemoved(ITerrainManager terrain) {
        // TODO port:1.20.1 - reimplement once PhysicsEntityTerrainLoader is ported.
    }
}
