package fr.dynamx.common.network.sync;

import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.packets.PhysicsEntityMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class MessageSeatsSync extends PhysicsEntityMessage<MessageSeatsSync> {
    private final Map<Byte, Integer> seatToEntity = new HashMap<>();

    public MessageSeatsSync() {
        super(null);
    }

    // TODO port:1.20.1 - Originally IModuleContainer.ISeatsContainer (not yet ported, Phase 8).
    // Relaxed parameter to Object to keep API surface; receive helper handles real container later.
    public MessageSeatsSync(Object vehicleEntity) {
        super(extractEntity(vehicleEntity));
        // TODO port:1.20.1 - Populate seatToEntity once ISeatsContainer + BasePartSeat are ported.
    }

    private static PhysicsEntity<?> extractEntity(Object vehicleEntity) {
        // TODO port:1.20.1 - Real impl: ((ISeatsContainer) vehicleEntity).cast() returns PhysicsEntity<?>.
        if (vehicleEntity instanceof PhysicsEntity) return (PhysicsEntity<?>) vehicleEntity;
        return null;
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
        // TODO port:1.20.1 - Re-port using ISeatsContainer#getSeats#updateSeats + DynamXContext.getPhysicsWorld.
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
