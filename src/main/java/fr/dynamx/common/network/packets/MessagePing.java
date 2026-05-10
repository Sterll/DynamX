package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.LogicalSide;

public class MessagePing implements IDnxPacket {
    private long sentTime;
    private boolean manual;

    public MessagePing() {
    }

    public MessagePing(long creationTime, boolean manual) {
        this.sentTime = creationTime;
        this.manual = manual;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(sentTime);
        buf.writeBoolean(manual);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        sentTime = buf.readLong();
        manual = buf.readBoolean();
    }

    public static void handle(MessagePing message /*, IPayloadContext ctx */) {
        // TODO port:1.20.1 - On server side: echo MessagePing back to player; on client side: update ping.
        // Restore in Phase 5b once ClientPhysicsSyncManager is ported.
    }

    private static void clientHandle(MessagePing message) {
        // TODO port:1.20.1 - ClientPhysicsSyncManager.pingMs/lastPing update, Minecraft.getInstance().player.sendSystemMessage(...).
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.DYNAMX_UDP;
    }

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        if (side.equals(LogicalSide.SERVER)) {
            // TODO port:1.20.1 - DynamXContext.getNetwork().sendToClient(...) once Phase 4b lands the
            // singleton; for now silently drop the echo.
        } else {
            clientHandle(this);
        }
    }
}
