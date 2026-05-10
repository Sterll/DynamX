package fr.dynamx.api.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.LogicalSide;

/**
 * Base interface for all DynamX network packets.
 */
// TODO port:1.20.1 - In Forge 1.12 this extended IMessage; in NeoForge 1.20.1 packets must be
// CustomPacketPayload registered via PayloadRegistrar. The bridge to PayloadRegistrar will be added
// in Phase 5 (common/network impl). For now this is a pure abstract marker so API consumers compile.
public interface IDnxPacket {
    EnumNetworkType getPreferredNetwork();

    // TODO port:1.20.1 - Forge Side replaced by NeoForge LogicalSide.
    default void handleUDPReceive(Player context, LogicalSide side) {
        throw new UnsupportedOperationException("UDP handling of this packet is not implemented !");
    }

    /**
     * Legacy ByteBuf serialization API, retained as default no-op to ease the 1.12 → 1.20.1 port.
     * <p>
     * In NeoForge 1.20.1 messages must implement CustomPacketPayload with a FriendlyByteBuf codec; the
     * methods below keep call-sites compiling until each Message* class wires its CustomPacketPayload
     * codec in Phase 5b.
     */
    // TODO port:1.20.1 - Migrate to CustomPacketPayload codec (FriendlyByteBuf based).
    default void fromBytes(ByteBuf buf) {
    }

    // TODO port:1.20.1 - Migrate to CustomPacketPayload codec (FriendlyByteBuf based).
    default void toBytes(ByteBuf buf) {
    }
}
