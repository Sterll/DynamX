package fr.dynamx.common.network.packets;

import com.jme3.math.Vector3f;
import fr.dynamx.common.entities.PhysicsEntity;
import io.netty.buffer.ByteBuf;
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
        // TODO port:1.20.1 - Re-port using level().getEntity, EnumFacing → Direction.from3DDataValue,
        // entity.walkingOnPlayers, DynamXContext.getWalkingPlayers, WalkingOnPlayerController.
    }

    @Override
    protected void processMessageServer(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, Player player) {
        // TODO port:1.20.1 - Same as client + re-broadcast via DynamXContext.getNetwork().sendToClientFromOtherThread.
    }
}
