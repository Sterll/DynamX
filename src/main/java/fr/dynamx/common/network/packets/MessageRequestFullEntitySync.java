package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.common.entities.PhysicsEntity;
import net.minecraft.world.entity.player.Player;

public class MessageRequestFullEntitySync extends PhysicsEntityMessage<MessageRequestFullEntitySync> {
    public MessageRequestFullEntitySync() {
        super(null);
    }

    public MessageRequestFullEntitySync(PhysicsEntity<?> entity) {
        super(entity);
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    protected void processMessageClient(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, Player player) {
        throw new IllegalStateException();
    }

    @Override
    protected void processMessageServer(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, Player player) {
        // TODO port:1.20.1 - Re-port using ServerPlayer#connection#getConnection#isConnected and
        // entity.getSynchronizer().resyncEntity((ServerPlayer) player) once PhysicsEntitySynchronizer wiring is finalized.
    }
}
