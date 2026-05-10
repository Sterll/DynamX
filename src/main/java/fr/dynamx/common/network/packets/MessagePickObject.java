package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import io.netty.buffer.ByteBuf;

public class MessagePickObject implements IDnxPacket {
    // TODO port:1.20.1 - MovableModule.Action not yet ported (Phase 8/entities modules);
    // relaxed to Object until that lands.
    private Object moduleAction;

    public MessagePickObject() {
    }

    public MessagePickObject(Object action) {
        this.moduleAction = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // TODO port:1.20.1 - Re-port using MovableModule.Action/EnumAction once Phase 8 lands.
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // TODO port:1.20.1 - Re-port using MovableModule.Action/EnumAction once Phase 8 lands.
    }

    public static void handle(MessagePickObject message /*, IPayloadContext ctx */) {
        // TODO port:1.20.1 - Body: DynamXContext.getPhysicsWorld(player.level()).schedule(...).
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
