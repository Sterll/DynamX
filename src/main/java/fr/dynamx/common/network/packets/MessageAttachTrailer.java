package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import io.netty.buffer.ByteBuf;

/**
 * Server-bound request to attach the trailer the player is currently sitting on.
 */
// TODO port:1.20.1 - This message had IMessageHandler logic; the server-side handling references
// BaseVehicleEntity, TrailerAttachModule, TrailerEntity, CarEntity, DynamXUtils.attachTrailer which
// belong to phases not yet ported. Body stubbed; receive logic must be reinstated in Phase 5b once
// PayloadRegistrar is in place and Phase 8/9 deliver the handler entities.
public class MessageAttachTrailer implements IDnxPacket {

    public MessageAttachTrailer() {
    }

    @Override
    public void fromBytes(ByteBuf byteBuf) {
    }

    @Override
    public void toBytes(ByteBuf byteBuf) {
    }

    /**
     * Legacy onMessage body retained as a server-side handler to be wired via PayloadRegistrar.
     */
    // TODO port:1.20.1 - Wire as a server-side PayloadHandler. Body references unported modules.
    public static void handle(MessageAttachTrailer message /*, IPayloadContext ctx */) {
        // TODO port:1.20.1 - re-port from legacy MessageAttachTrailer#onMessage when Phase 8/9 land.
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
