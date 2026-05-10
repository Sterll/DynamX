package fr.dynamx.api.network.sync;

import net.minecraftforge.fml.LogicalSide;

/**
 * All possible targets for SynchronizedVariable <br>
 * Determines the targets of sync packets
 */
public enum SyncTarget {
    /**
     * Send to no one, except players starting to track the entity
     */
    NONE,
    /**
     * Send to server
     */
    SERVER,
    /**
     * Send to all tracking client
     */
    ALL_CLIENTS,
    /**
     * Send to all tracking clients except the driver (reduces network charge)
     */
    SPECTATORS,
    /**
     * Send to the driver of this entity
     */
    DRIVER;

    /**
     * True if the given target (in parameter) includes this target (example : ALL_CLIENTS includes SPECTATORS)
     */
    public boolean isIncluded(SyncTarget target) {
        return target == this || (target == ALL_CLIENTS && (this == DRIVER || this == SPECTATORS));
    }

    /**
     * @return if side is client : SPECTATORS, else SERVER
     */
    // TODO port:1.20.1 - Forge Side replaced by NeoForge LogicalSide.
    public static SyncTarget spectatorForSide(LogicalSide side) {
        return side == LogicalSide.SERVER ? SERVER : SPECTATORS;
    }

    /**
     * Same as spectatorForSide
     *
     * @return if side is client : SPECTATORS, else SERVER
     */
    public static SyncTarget nearSpectatorForSide(LogicalSide side) {
        return side == LogicalSide.SERVER ? SERVER : SPECTATORS;
    }
}
