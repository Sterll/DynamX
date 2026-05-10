package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import io.netty.buffer.ByteBuf;

public class MessageChangeDoorState implements IDnxPacket {

    public int vehicleID;
    // TODO port:1.20.1 - DoorsModule.DoorState not yet ported; relax to int (ordinal) until Phase 8/9.
    public int doorStateOrdinal;
    public byte doorId;

    public MessageChangeDoorState() {
    }

    // TODO port:1.20.1 - Legacy ctor took (BaseVehicleEntity, DoorsModule.DoorState, byte). Stubbed.
    public MessageChangeDoorState(Object vehicle, Object doorState, byte doorId) {
        // TODO port:1.20.1 - Original: this.vehicleID = vehicle.getEntityId(); this.doorState = doorState;
        this.doorId = doorId;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(vehicleID);
        buf.writeInt(doorStateOrdinal);
        buf.writeByte(doorId);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        vehicleID = buf.readInt();
        doorStateOrdinal = buf.readInt();
        doorId = buf.readByte();
    }

    public static void handle(MessageChangeDoorState message /*, IPayloadContext ctx */) {
        // TODO port:1.20.1 - re-port from legacy MessageChangeDoorState#onMessage (needs IModuleContainer + DoorsModule).
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
