package fr.dynamx.common.network;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.IDnxNetworkHandler;
import fr.dynamx.api.network.IDnxPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

/**
 * Vanilla TCP-based DynamX network handler. In Forge 1.12 this wrapped a {@code SimpleNetworkWrapper}.
 * In NeoForge 1.20.1 packets are dispatched via {@link net.minecraftforge.network.PacketDistributor}
 * after registration through {@link net.minecraftforge.network.registration.PayloadRegistrar}.
 */
// TODO port:1.20.1 - PacketDistributor send wiring and PayloadRegistrar registration are deferred to
// DynamXNetwork#init. This class keeps the public API surface stable; methods stub the actual send.
public class VanillaNetworkHandler implements IDnxNetworkHandler {

    public VanillaNetworkHandler() {
    }

    @Override
    public <T> void sendPacket(IDnxPacket packet, EnumPacketTarget<T> targetType, @Nullable T target) {
        if (EnumPacketTarget.SERVER == targetType) {
            sendToServer(packet);
        } else {
            sendPacketServer(packet, targetType, target);
        }
    }

    private <T> void sendPacketServer(IDnxPacket packet, EnumPacketTarget<T> targetType, @Nullable T target) {
        if (EnumPacketTarget.PLAYER == targetType) {
            sendToPlayer(packet, (ServerPlayer) target);
        } else if (EnumPacketTarget.ALL_AROUND == targetType) {
            sendToAllAround(packet, (EnumPacketTarget.TargetPoint) target);
        } else if (EnumPacketTarget.ALL_TRACKING_ENTITY == targetType) {
            sendToAllTracking(packet, (Entity) target);
        } else if (EnumPacketTarget.ALL == targetType) {
            sendToAll(packet);
        }
    }

    // TODO port:1.20.1 - Replace with PacketDistributor.sendToServer(packet) once payload is registered.
    public void sendToServer(IDnxPacket packet) {
        // PacketDistributor.sendToServer((CustomPacketPayload) packet);
    }

    // TODO port:1.20.1 - PacketDistributor.sendToPlayer(player, payload).
    public void sendToPlayer(IDnxPacket packet, ServerPlayer player) {
        // PacketDistributor.sendToPlayer(player, (CustomPacketPayload) packet);
    }

    // TODO port:1.20.1 - PacketDistributor.sendToPlayersNear(level, exclude, x, y, z, range, payload).
    public void sendToAllAround(IDnxPacket packet, EnumPacketTarget.TargetPoint target) {
        // PacketDistributor.sendToPlayersNear(..., (CustomPacketPayload) packet);
    }

    // TODO port:1.20.1 - PacketDistributor.sendToPlayersTrackingEntity(entity, payload).
    public void sendToAllTracking(IDnxPacket packet, Entity entity) {
        // PacketDistributor.sendToPlayersTrackingEntity(entity, (CustomPacketPayload) packet);
    }

    // TODO port:1.20.1 - PacketDistributor.sendToAllPlayers(payload).
    public void sendToAll(IDnxPacket packet) {
        // PacketDistributor.sendToAllPlayers((CustomPacketPayload) packet);
    }

    @Override
    public EnumNetworkType getType() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public void stop() {
    }

    /**
     * @return The legacy channel handle. In NeoForge 1.20.1 there is no equivalent; returns null.
     */
    // TODO port:1.20.1 - SimpleNetworkWrapper does not exist in 1.20.1; the legacy {@code getChannel()}
    // call sites must move to using PayloadRegistrar registration directly.
    public Object getChannel() {
        return null;
    }
}
