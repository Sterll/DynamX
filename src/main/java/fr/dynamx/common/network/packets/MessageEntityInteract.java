package fr.dynamx.common.network.packets;

import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.contentpack.parts.PartEntitySeat;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.items.tools.ItemWrench;
import fr.dynamx.utils.optimization.Vector3fPool;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

public class MessageEntityInteract implements IDnxPacket {
    private int vehicleID;

    public MessageEntityInteract() {
    }

    public MessageEntityInteract(int vehicleID) {
        this.vehicleID = vehicleID;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(vehicleID);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        vehicleID = buf.readInt();
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void handleUDPReceive(Player context, LogicalSide side) {
        if (side != LogicalSide.SERVER || context == null || context.level() == null) {
            return;
        }
        Entity entity = context.level().getEntity(vehicleID);
        if (!(entity instanceof PhysicsEntity)) {
            return;
        }
        PhysicsEntity<?> physicsEntity = (PhysicsEntity<?>) entity;
        if (context.distanceTo(physicsEntity) > 21) {
            return;
        }
        if (context.getMainHandItem().getItem() instanceof ItemWrench) {
            ((ItemWrench) context.getMainHandItem().getItem()).interact(context, physicsEntity);
            return;
        }
        if (!(physicsEntity instanceof PackPhysicsEntity)) {
            return;
        }
        // TODO port:1.20.1 - re-add the IModuleContainer.ISeatsContainer "already sitting" early return
        // once SeatsModule#isEntitySitting is ported.
        PackPhysicsEntity<?, ?> targetEntity = (PackPhysicsEntity<?, ?>) physicsEntity;
        Vector3fPool.openPool();
        try {
            InteractivePart hitPart = targetEntity.getHitPart(context);
            if (hitPart != null && hitPart.canInteract(targetEntity, context)) {
                if (hitPart instanceof PartEntitySeat && ((PartEntitySeat) hitPart).hasDoor() && context.isShiftKeyDown()) {
                    return;
                }
                if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.PlayerInteract(context, targetEntity, hitPart))) {
                    hitPart.interact(targetEntity, context);
                }
            } else {
                MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.PlayerInteract(context, targetEntity, null));
            }
        } finally {
            Vector3fPool.closePool();
        }
    }
}
