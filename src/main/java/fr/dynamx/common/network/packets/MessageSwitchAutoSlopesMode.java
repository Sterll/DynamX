package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import io.netty.buffer.ByteBuf;

public class MessageSwitchAutoSlopesMode implements IDnxPacket {
    public int mode;

    public MessageSwitchAutoSlopesMode(int mode) {
        this.mode = mode;
    }

    public MessageSwitchAutoSlopesMode() {
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        mode = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(mode);
    }

    /**
     * Legacy nested Handler retained.
     */
    public static class Handler {
        public static void handle(MessageSwitchAutoSlopesMode message /*, IPayloadContext ctx */) {
            handleClient(message);
        }

        private static void handleClient(MessageSwitchAutoSlopesMode message) {
            // TODO port:1.20.1 - ContentPackLoader.PLACE_SLOPES = message.mode == 1; — ContentPackLoader is ported.
            try {
                fr.dynamx.common.contentpack.ContentPackLoader.PLACE_SLOPES = message.mode == 1;
            } catch (Throwable t) {
                // TODO port:1.20.1 - ContentPackLoader.PLACE_SLOPES field may not yet exist.
            }
        }
    }
}
