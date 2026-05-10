package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import io.netty.buffer.ByteBuf;

import java.util.Map;

/**
 * Sent to clients with terrain debug data to draw collision boxes.
 */
// TODO port:1.20.1 - ISerializablePacket (ACsLib) + DeserializedData not yet ported; the body is
// stubbed and the legacy custom serializer is replaced by a no-op until Phase 5b restores it.
public class MessageCollisionDebugDraw implements IDnxPacket {
    // TODO port:1.20.1 - TerrainDebugData not yet ported; relax to Object map.
    private Map<Integer, Object> chunkOrBlockData;
    private Map<Integer, Object> slopeData;

    public MessageCollisionDebugDraw() {
    }

    public MessageCollisionDebugDraw(Map<Integer, ?> chunkOrBlockData, Map<Integer, ?> slopeData) {
        this.chunkOrBlockData = (Map<Integer, Object>) chunkOrBlockData;
        this.slopeData = (Map<Integer, Object>) slopeData;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // TODO port:1.20.1 - ACsLib PacketSerializer needs port; serialization stubbed.
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // TODO port:1.20.1 - ACsLib PacketSerializer needs port; deserialization stubbed.
    }

    public static void handle(MessageCollisionDebugDraw message /*, IPayloadContext ctx */) {
        // TODO port:1.20.1 - Re-port using Minecraft.getInstance().tell once DynamXDebugOptions is ported.
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
