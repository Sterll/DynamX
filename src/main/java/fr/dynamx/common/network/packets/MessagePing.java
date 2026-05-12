package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.client.network.ClientPhysicsSyncManager;
import fr.dynamx.common.network.DynamXNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;

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

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.DYNAMX_UDP;
    }

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        if (side == LogicalSide.SERVER) {
            if (context instanceof ServerPlayer serverPlayer) {
                DynamXNetwork.sendTo(new MessagePing(sentTime, manual), serverPlayer);
            }
        } else {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> clientHandle(sentTime, manual));
        }
    }

    private static void clientHandle(long sentTime, boolean manual) {
        ClientPhysicsSyncManager.pingMs = ((int) (System.currentTimeMillis() - sentTime)) / 2;
        ClientPhysicsSyncManager.lastPing = sentTime;
        Player local = Minecraft.getInstance().player;
        if (manual && local != null) {
            local.sendSystemMessage(Component.literal("[DynamX] Your ping is " + ClientPhysicsSyncManager.pingMs + " ms"));
        }
    }
}
