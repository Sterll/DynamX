package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.entities.PhysicsEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;

import javax.annotation.Nullable;

/**
 * Base class for all messages tied to a {@link PhysicsEntity} by entity id.
 */
// TODO port:1.20.1 - Originally implements IMessageHandler<T, IMessage> - those handlers are wired
// via PayloadRegistrar in NeoForge 1.20.1. Re-route through {@code handle(IPayloadContext)} in
// Phase 5b. The legacy processMessage* abstract methods are preserved as-is.
public abstract class PhysicsEntityMessage<T extends PhysicsEntityMessage<T>> implements IDnxPacket {
    /**
     * Handled internally, should not be modified
     */
    protected int entityId = -1;

    private PhysicsEntityMessage() {
    }

    /*
     * @param entity can be null for empty constructor
     */
    public PhysicsEntityMessage(@Nullable PhysicsEntity<?> entity) {
        if (entity != null)
            this.entityId = entity.getId();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
    }

    /**
     * Legacy onMessage body retained for porting. The PayloadRegistrar wiring in Phase 5b should call
     * this static helper with the deserialized message + payload context.
     */
    // TODO port:1.20.1 - Replace MessageContext with IPayloadContext, schedule on physics world or
    // server executor depending on side. Body stubbed; legacy logic preserved in processMessage().
    public static void handle(PhysicsEntityMessage<?> message /*, IPayloadContext ctx */) {
        // TODO port:1.20.1 - schedule processMessage on appropriate thread.
    }

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        processMessage(side, this, context);
    }

    protected void processMessage(LogicalSide side, PhysicsEntityMessage<?> message, Player player) {
        if (message.entityId == -1)
            throw new IllegalArgumentException("EntityId isn't valid, maybe you don't call fromBytes and toBytes " + message);
        if (player == null || player.level() == null) {
            // TODO port:1.20.1 - DynamXMain.log not yet ported; use System.err for now.
            System.err.println("Skipping receive PhysicsEntityMessage " + message + ": local player or world not yet loaded. Player: " + player);
            return;
        }
        // TODO port:1.20.1 - Original getEntityByID(int) is now level.getEntity(int).
        Object ent = player.level().getEntity(message.entityId);
        if (ent instanceof PhysicsEntity) {
            if (side.equals(LogicalSide.CLIENT)) {
                processMessageClient(message, (PhysicsEntity<?>) ent, player);
            } else {
                processMessageServer(message, (PhysicsEntity<?>) ent, player);
            }
        }
    }

    protected abstract void processMessageClient(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, Player player);

    protected abstract void processMessageServer(PhysicsEntityMessage<?> message, PhysicsEntity<?> entity, Player player);

    // TODO port:1.20.1 - Minecraft.getMinecraft() → Minecraft.getInstance() (client-only access).
    protected Player getClientPlayer() {
        try {
            return net.minecraft.client.Minecraft.getInstance().player;
        } catch (Throwable t) {
            return null;
        }
    }

    protected void clientSchedule(Runnable task) {
        // TODO port:1.20.1 - Original used Minecraft#addScheduledTask or physicsWorld.schedule().
        // Use Minecraft.getInstance().execute(...) in Phase 5b once Minecraft client is reachable here.
        try {
            net.minecraft.client.Minecraft.getInstance().execute(task);
        } catch (Throwable t) {
            // ignore (likely server side)
        }
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
