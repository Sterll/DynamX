package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import io.netty.buffer.ByteBuf;

public class MessageOpenDebugGui implements IDnxPacket {
    private byte action;

    public MessageOpenDebugGui() {
    }

    public MessageOpenDebugGui(byte action) {
        this.action = action;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
    }

    /**
     * Legacy nested Handler retained.
     */
    public static class Handler {
        public static void handle(MessageOpenDebugGui message /*, IPayloadContext ctx */) {
            // TODO port:1.20.1 - Originally ACsGuiApi.asyncLoadThenShowGui("Dnx Debug", NewGuiDnxDebug::new).
            // ACsGuiApi + NewGuiDnxDebug not yet ported (Phase 8 client GUI). Restore later.
        }
    }
}
