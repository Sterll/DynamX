package fr.dynamx.api.network;

import javax.annotation.Nullable;

/**
 * Responsible to dispatch vanilla and udp packets and manages udp network
 */
// TODO port:1.20.1 - VanillaNetworkHandler (fr.dynamx.common.network) is not yet ported (Phase 5).
// sendToClientFromOtherThread used FMLCommonHandler in 1.12; in NeoForge 1.20.1 it should schedule on
// the server thread (MinecraftServer#executeIfPossible) — left as TODO until Phase 5.
public interface IDnxNetworkSystem {
    /**
     * Sends the packet to the server, using preferred network of the packet
     */
    default void sendToServer(IDnxPacket packet) {
        throw new UnsupportedOperationException("Unsupported on this side !");
    }

    /**
     * Sends the packet to the targeted client(s), using preferred network of the packet
     *
     * @param targetType The client(s) type
     */
    default void sendToClient(IDnxPacket packet, EnumPacketTarget<Void> targetType) {
        sendToClient(packet, targetType, null);
    }

    /**
     * Sends the packet to the targeted clients, using preferred network of the packet <br>
     * <strong>Only use this from the server thread because fml packets code isn't multi-threaded. Use 'sendToClientFromOtherThread' instead.</strong>
     *
     * @param targetType The client(s) type
     * @param target     The client(s)
     */
    default <T> void sendToClient(IDnxPacket packet, EnumPacketTarget<T> targetType, @Nullable T target) {
        throw new UnsupportedOperationException("Unsupported on this side !");
    }

    /**
     * Sends the packet to the targeted clients, using preferred network of the packet <br>
     * <strong>Unlike 'sendToClient', this will send the send task to the server thread, so use this if you are sending the packet from another thread.</strong>
     *
     * @param targetType The client(s) type
     * @param target     The client(s)
     */
    default <T> void sendToClientFromOtherThread(IDnxPacket packet, EnumPacketTarget<T> targetType, @Nullable T target) {
        // TODO port:1.20.1 - FMLCommonHandler removed in NeoForge 1.20.1; must schedule via
        // ServerLifecycleHooks.getCurrentServer().execute(...) once Phase 5 wires the network layer.
        throw new UnsupportedOperationException("sendToClientFromOtherThread not yet implemented on 1.20.1 (waiting for Phase 5 network impl)");
    }

    /**
     * @return The VanillaNetworkHandler, permits to bypass any udp handling
     */
    // TODO port:1.20.1 - return type should be VanillaNetworkHandler once Phase 5 ports it.
    Object getVanillaNetwork();

    boolean isConnected();

    /**
     * Called to start udp server, on server side
     */
    void startNetwork();

    /**
     * Called to stop udp server
     */
    void stopNetwork();

    /**
     * @return The preferred network, can be vanilla or udp
     */
    IDnxNetworkHandler getQuickNetwork();
}
