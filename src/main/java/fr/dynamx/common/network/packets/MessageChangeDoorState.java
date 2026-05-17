package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;

public class MessageChangeDoorState implements IDnxPacket {

    public int vehicleID;
    public DoorsModule.DoorState doorState;
    public byte doorId;

    public MessageChangeDoorState() {
    }

    public MessageChangeDoorState(BaseVehicleEntity<?> vehicle, DoorsModule.DoorState doorState, byte doorId) {
        this.vehicleID = vehicle.getId();
        this.doorState = doorState;
        this.doorId = doorId;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(vehicleID);
        buf.writeInt(doorState.ordinal());
        buf.writeByte(doorId);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        vehicleID = buf.readInt();
        doorState = DoorsModule.DoorState.values()[buf.readInt()];
        doorId = buf.readByte();
    }

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        if (side != LogicalSide.SERVER || context == null || context.level() == null) {
            return;
        }
        Entity entity = context.level().getEntity(vehicleID);
        if (entity instanceof BaseVehicleEntity<?> vehicle) {
            DoorsModule doors = vehicle.getModuleByType(DoorsModule.class);
            if (doors != null) {
                doors.setDoorState(doorId, doorState);
            }
        }
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
