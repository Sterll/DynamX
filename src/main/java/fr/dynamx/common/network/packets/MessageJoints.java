package fr.dynamx.common.network.packets;

import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.EntityJointsHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class MessageJoints extends PhysicsEntityMessage<MessageJoints> {
    private List<EntityJoint.CachedJoint> jointList;

    public MessageJoints() {
        super(null);
    }

    public MessageJoints(PhysicsEntity<?> entity, List<EntityJoint.CachedJoint> jointList) {
        super(entity);
        this.jointList = jointList;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);
        buf.writeInt(jointList.size());
        FriendlyByteBuf fb = (buf instanceof FriendlyByteBuf) ? (FriendlyByteBuf) buf : new FriendlyByteBuf(buf);
        for (EntityJoint.CachedJoint g : jointList) {
            fb.writeUtf(g.getId().toString());
            fb.writeByte(g.getJid());
            fb.writeUtf(g.getType().toString());
            fb.writeBoolean(g.isJointOwner());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        super.fromBytes(buf);
        jointList = new ArrayList<>();
        FriendlyByteBuf fb = (buf instanceof FriendlyByteBuf) ? (FriendlyByteBuf) buf : new FriendlyByteBuf(buf);
        int size = fb.readInt();
        for (int i = 0; i < size; i++) {
            jointList.add(new EntityJoint.CachedJoint(UUID.fromString(fb.readUtf()), fb.readByte(),
                    new ResourceLocation(fb.readUtf()), fb.readBoolean()));
        }
    }

    @Override
    protected void processMessageClient(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, Player player) {
        if (entity.getJointsHandler() == null) {
            System.err.println("[DynamX] Cannot sync joints of " + entity + " : joint handler is null !");
            return;
        }
        List<EntityJoint.CachedJoint> joints = new ArrayList<>(((MessageJoints) message).getJointList());
        EntityJointsHandler handler = entity.getJointsHandler();
        Collection<EntityJoint<?>> curJoints = handler.getJoints();
        curJoints.removeIf(j -> {
            EntityJoint.CachedJoint found = null;
            for (EntityJoint.CachedJoint g : joints) {
                if (g.getId().equals(j.getOtherEntity(entity).getUUID())) {
                    found = g;
                    break;
                }
            }
            if (found != null) {
                joints.remove(found);
                return false;
            }
            handler.onRemoveJoint(j);
            return true;
        });
        for (EntityJoint.CachedJoint g : joints) {
            if (g.isJointOwner()) {
                handler.onNewJointSynchronized(g);
            }
        }
    }

    @Override
    protected void processMessageServer(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, Player player) {
        throw new IllegalStateException();
    }

    public List<EntityJoint.CachedJoint> getJointList() {
        return jointList;
    }
}
