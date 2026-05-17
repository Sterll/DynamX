package fr.dynamx.common.network.packets;

import fr.aym.acslib.utils.DeserializedData;
import fr.aym.acslib.utils.packetserializer.ISerializablePacket;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.debug.TerrainDebugData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;

import java.util.Map;

/**
 * Sent to clients with terrain debug data to draw collision boxes.
 */
// TODO port:1.20.1 - The ACsLib PacketSerializer that backs ISerializablePacket is still a stub in
// the 1.20.1 port (no-op writeTo/readFrom). The wire format will become functional again once the
// serializer is ported. The receive logic below is fully restored.
public class MessageCollisionDebugDraw implements IDnxPacket, ISerializablePacket {
    private Map<Integer, TerrainDebugData> chunkOrBlockData;
    private Map<Integer, TerrainDebugData> slopeData;

    public MessageCollisionDebugDraw() {
    }

    public MessageCollisionDebugDraw(Map<Integer, TerrainDebugData> chunkOrBlockData, Map<Integer, TerrainDebugData> slopeData) {
        this.chunkOrBlockData = chunkOrBlockData;
        this.slopeData = slopeData;
    }

    @Override
    public Object[] getObjectsToSave() {
        return new Object[]{chunkOrBlockData, slopeData};
    }

    @Override
    @SuppressWarnings("unchecked")
    public void populateWithSavedObjects(DeserializedData objects) {
        this.chunkOrBlockData = objects.next();
        this.slopeData = objects.next();
    }

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        if (side != LogicalSide.CLIENT) {
            return;
        }
        Minecraft.getInstance().execute(() -> {
            if (DynamXDebugOptions.BLOCK_BOXES.isActive()) {
                DynamXDebugOptions.BLOCK_BOXES.setDataIn(chunkOrBlockData);
            }
            if (DynamXDebugOptions.SLOPE_BOXES.isActive()) {
                DynamXDebugOptions.SLOPE_BOXES.setDataIn(slopeData);
            }
        });
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
