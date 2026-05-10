package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import io.netty.buffer.ByteBuf;

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

    public static void handle(MessageSyncPlayerPicking message /*, IPayloadContext ctx */) {
        // TODO port:1.20.1 - DynamXContext.setPlayerPickingObjects(message.map) once DynamXContext is ported (Phase 4b).
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
