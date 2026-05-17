package fr.dynamx.common.network.sync;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.BasePartSeat;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.network.packets.PhysicsEntityMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

import static fr.dynamx.common.DynamXMain.log;

public class MessageSeatsSync extends PhysicsEntityMessage<MessageSeatsSync> {
    private final Map<Byte, Integer> seatToEntity = new HashMap<>();

    public MessageSeatsSync() {
        super(null);
    }

    public MessageSeatsSync(IModuleContainer.ISeatsContainer vehicleEntity) {
        super((PhysicsEntity<?>) vehicleEntity.cast());
        Object seatsObj = vehicleEntity.getSeats();
        if (seatsObj instanceof SeatsModule) {
            SeatsModule seats = (SeatsModule) seatsObj;
            for (Map.Entry<BasePartSeat, Entity> e : seats.getSeatToPassengerMap().entrySet()) {
                seatToEntity.put(e.getKey().getId(), e.getValue().getId());
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        super.fromBytes(buf);
        int size = buf.readInt();
        for (int i = 0; i < size; i++)
            seatToEntity.put(buf.readByte(), buf.readInt());
    }

    @Override
    protected void processMessageClient(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, Player player) {
        if (!(entity instanceof IModuleContainer.ISeatsContainer) || !((IModuleContainer.ISeatsContainer) entity).hasSeats()) {
            if (entity != null) {
                log.error("Received seats packet for an entity that have no seats! Entity: {}", entity);
            }
            return;
        }
        Object seatsObj = ((IModuleContainer.ISeatsContainer) entity).getSeats();
        if (!(seatsObj instanceof SeatsModule)) return;
        SeatsModule seats = (SeatsModule) seatsObj;
        DynamXContext.getPhysicsWorld(entity.level()).schedule(() -> seats.updateSeats((MessageSeatsSync) message, entity.getSynchronizer()));
    }

    @Override
    protected void processMessageServer(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, Player player) {
        throw new IllegalStateException();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);
        buf.writeInt(seatToEntity.size());
        for (Map.Entry<Byte, Integer> e : seatToEntity.entrySet()) {
            buf.writeByte(e.getKey());
            buf.writeInt(e.getValue());
        }
    }

    public Map<Byte, Integer> getSeatToEntity() {
        return seatToEntity;
    }
}
