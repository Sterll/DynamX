package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;

import java.util.HashMap;
import java.util.Map;

public class MessageSyncPlayerPicking implements IDnxPacket {

    private Map<Integer, Integer> map = new HashMap<>();

    public MessageSyncPlayerPicking() {
    }

    public MessageSyncPlayerPicking(Map<Integer, Integer> map) {
        this.map = map;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            map.put(buf.readInt(), buf.readInt());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(map.size());
        map.forEach((a, b) -> {
            buf.writeInt(a);
            buf.writeInt(b);
        });
    }

    public Map<Integer, Integer> getMap() {
        return map;
    }

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        if (side == LogicalSide.CLIENT) {
            fr.dynamx.common.DynamXContext.setPlayerPickingObjects(map);
        }
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
