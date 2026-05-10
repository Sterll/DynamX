package fr.dynamx.api.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Fake enum, enumerating all possible targets for {@link IDnxPacket}
 *
 * @param <O> Type of additional target information
 */
// TODO port:1.20.1 - Forge's NetworkRegistry.TargetPoint no longer exists in NeoForge 1.20.1.
// ALL_AROUND now uses a dedicated TargetPoint record (see nested class) until the network layer
// is fully ported in Phase 5.
public class EnumPacketTarget<O> {
    /**
     * Server target, can be only used from client side
     */
    public static final EnumPacketTarget<Void> SERVER = new EnumPacketTarget<>();
    /**
     * Specific player target, can be only used from server side
     */
    public static final EnumPacketTarget<ServerPlayer> PLAYER = new EnumPacketTarget<>();
    /**
     * All around point target, can be only used from server side
     */
    public static final EnumPacketTarget<TargetPoint> ALL_AROUND = new EnumPacketTarget<>();
    /**
     * All players tracking a specific entity, can be only used from server side
     */
    public static final EnumPacketTarget<Entity> ALL_TRACKING_ENTITY = new EnumPacketTarget<>();
    /**
     * All players target, can be only used from server side
     */
    public static final EnumPacketTarget<Void> ALL = new EnumPacketTarget<>();

    /**
     * Replacement for the removed Forge NetworkRegistry.TargetPoint.
     * Used to address all players within a given range around a point in a dimension.
     */
    // TODO port:1.20.1 - kept minimal; will be wired to NeoForge's PacketDistributor.TargetPoint in Phase 5.
    public static class TargetPoint {
        public final String dimension;
        public final double x, y, z;
        public final double range;

        public TargetPoint(String dimension, double x, double y, double z, double range) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.range = range;
        }
    }
}
