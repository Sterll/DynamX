package fr.dynamx.common.network.packets;

import fr.dynamx.common.entities.PhysicsEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MessageJoints extends PhysicsEntityMessage<MessageJoints> {
    // TODO port:1.20.1 - EntityJoint.CachedJoint not yet ported (Phase 7 — physics joints).
    // Until then we keep an opaque list of CachedJoint records using local fields.
    private List<CachedJoint> jointList;

    public MessageJoints() {
        super(null);
    }

    public MessageJoints(PhysicsEntity<?> entity, List<CachedJoint> jointList) {
        super(entity);
        this.jointList = jointList;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        super.toBytes(buf);
        buf.writeInt(jointList.size());
        FriendlyByteBuf fb = (buf instanceof FriendlyByteBuf) ? (FriendlyByteBuf) buf : new FriendlyByteBuf(buf);
        for (CachedJoint g : jointList) {
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
            jointList.add(new CachedJoint(UUID.fromString(fb.readUtf()), fb.readByte(),
                    new ResourceLocation(fb.readUtf()), fb.readBoolean()));
        }
    }

    @Override
    protected void processMessageClient(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, Player player) {
        // TODO port:1.20.1 - Re-port the joint synchronisation once Phase 7 (physics joints) lands.
    }

    @Override
    protected void processMessageServer(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, Player player) {
        throw new IllegalStateException();
    }

    public List<CachedJoint> getJointList() {
        return jointList;
    }

    /**
     * Minimal placeholder for fr.dynamx.common.physics.joints.EntityJoint.CachedJoint
     * (kept local until Phase 7 ports the real type).
     */
    // TODO port:1.20.1 - replace with real EntityJoint.CachedJoint reference after Phase 7.
    public static class CachedJoint {
        private final UUID id;
        private final byte jid;
        private final ResourceLocation type;
        private final boolean jointOwner;

        public CachedJoint(UUID id, byte jid, ResourceLocation type, boolean jointOwner) {
            this.id = id;
            this.jid = jid;
            this.type = type;
            this.jointOwner = jointOwner;
        }

        public UUID getId() { return id; }
        public byte getJid() { return jid; }
        public ResourceLocation getType() { return type; }
        public boolean isJointOwner() { return jointOwner; }
    }
}
