package fr.dynamx.api.network;

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
}
