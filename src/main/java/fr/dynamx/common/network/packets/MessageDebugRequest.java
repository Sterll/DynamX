package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import io.netty.buffer.ByteBuf;

public class MessageDebugRequest implements IDnxPacket {
    public int debugMode;

    public MessageDebugRequest() {
    }

    /**
     * @param debugMode A terrain debug mode id, OR -15815 to switch slopes item mode, OR -15816 to switch wrench item mode
     */
    public MessageDebugRequest(int debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(debugMode);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        debugMode = buf.readInt();
    }

    public static void handle(MessageDebugRequest message /*, IPayloadContext ctx */) {
        // TODO port:1.20.1 - Body relies on ItemSlopes/ItemWrench (Phase items), PhysicsTickHandler (Phase 2),
        // Component messaging (TextComponentString → Component.literal). Restore in Phase 5b.
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
