package fr.dynamx.common.network.packets;

import com.jme3.math.Vector3f;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.DynamXNetwork;
import fr.dynamx.common.physics.player.WalkingOnPlayerController;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class MessageWalkingPlayer extends PhysicsEntityMessage<MessageWalkingPlayer> {
    public int playerId;
    public Vector3f offset;
    public byte face;

    public MessageWalkingPlayer() {
        super(null);
    }

    public MessageWalkingPlayer(PhysicsEntity<?> entity, int playerId, Vector3f offset, byte face) {
        super(entity);
        this.playerId = playerId;
        this.offset = offset;
        this.face = face;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);
        buf.writeInt(playerId);
        buf.writeFloat(offset.x);
        buf.writeFloat(offset.y);
        buf.writeFloat(offset.z);
        buf.writeByte(face);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        super.fromBytes(buf);
        playerId = buf.readInt();
        offset = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        face = buf.readByte();
    }

    @Override
    protected void processMessageClient(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, Player player) {
        apply((MessageWalkingPlayer) message, entity);
    }

    @Override
    protected void processMessageServer(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, Player player) {
        MessageWalkingPlayer m = (MessageWalkingPlayer) message;
        apply(m, entity);
        DynamXNetwork.sendToAllTracking(m, entity);
    }

    private static void apply(MessageWalkingPlayer m, PhysicsEntity<?> entity) {
        Entity target = entity.level().getEntity(m.playerId);
        if (!(target instanceof Player walking)) {
            return;
        }
        if (m.face == -1) {
            WalkingOnPlayerController controller = entity.walkingOnPlayers.remove(walking);
            if (controller != null) {
                DynamXContext.getWalkingPlayers().remove(walking);
            }
            return;
        }
        Direction direction = Direction.from3DDataValue(m.face);
        WalkingOnPlayerController controller = new WalkingOnPlayerController(walking, entity, direction, m.offset);
        entity.walkingOnPlayers.put(walking, controller);
        DynamXContext.getWalkingPlayers().put(walking, entity);
    }
}
