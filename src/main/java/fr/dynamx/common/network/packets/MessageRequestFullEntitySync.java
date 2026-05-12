package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.common.entities.PhysicsEntity;
import net.minecraft.server.level.ServerPlayer;
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
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null
                && serverPlayer.connection.connection.isConnected()
                && entity.getSynchronizer() != null) {
            entity.getSynchronizer().resyncEntity(serverPlayer);
        }
    }
}
