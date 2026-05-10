package fr.dynamx.common.network.sync;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;

import java.util.ArrayList;
import java.util.Collection;

public class MessageMultiPhysicsEntitySync implements IDnxPacket {
    private Collection<MessagePhysicsEntitySync<?>> syncs;

    public MessageMultiPhysicsEntitySync() {
    }

    public MessageMultiPhysicsEntitySync(Collection<MessagePhysicsEntitySync<?>> syncs) {
        this.syncs = syncs;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.DYNAMX_UDP;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        syncs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            MessagePhysicsEntitySync<?> msg = new MessagePhysicsEntitySync<>();
            msg.fromBytes(buf);
            syncs.add(msg);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(syncs.size());
        syncs.forEach(s -> s.toBytes(buf));
    }

    public static void handle(MessageMultiPhysicsEntitySync message /*, IPayloadContext ctx */) {
        try {
            Player p = net.minecraft.client.Minecraft.getInstance().player;
            message.handleUDPReceive(p, LogicalSide.CLIENT);
        } catch (Throwable t) {
            // server side
        }
    }

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        syncs.forEach(s -> s.handleUDPReceive(context, side));
    }

    @Override
    public String toString() {
        return "MessageMultiPhysicsEntitySync{syncs=" + syncs + '}';
    }
}
