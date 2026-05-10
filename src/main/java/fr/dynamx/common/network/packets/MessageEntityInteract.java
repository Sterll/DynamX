package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.LogicalSide;

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

    public static void handle(MessageEntityInteract message /*, IPayloadContext ctx */) {
        // TODO port:1.20.1 - Original handler scheduled message.handleUDPReceive on server thread.
        // Restore via ServerLifecycleHooks.getCurrentServer().execute(...) once available.
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        // TODO port:1.20.1 - body needs PhysicsEntity, PackPhysicsEntity, ItemWrench, IModuleContainer,
        // InteractivePart, VehicleEntityEvent (MinecraftForge.EVENT_BUS → NeoForge.EVENT_BUS),
        // Vector3fPool, level().getEntity(int). Restore in Phase 5b/8/9.
    }
}
