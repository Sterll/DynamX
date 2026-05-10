package fr.dynamx.common.network.packets;

import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.utils.DynamXUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class MessageHandleExplosion implements IDnxPacket {

    private Vector3f explosionPosition;
    private List<Integer> entityIdList = new ArrayList<>();

    public MessageHandleExplosion() {
    }

    public MessageHandleExplosion(Vector3f explosionPosition, List<Entity> entityList) {
        this.explosionPosition = explosionPosition;
        for (Entity entity : entityList) {
            // TODO port:1.20.1 - Entity#getEntityId → Entity#getId.
            entityIdList.add(entity.getId());
        }
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        explosionPosition = DynamXUtils.readVector3f(buf);
        entityIdList = new ArrayList<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            entityIdList.add(buf.readInt());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        DynamXUtils.writeVector3f(buf, explosionPosition);
        buf.writeInt(entityIdList.size());
        for (Integer id : entityIdList) {
            buf.writeInt(id);
        }
    }

    public static void handle(MessageHandleExplosion message /*, IPayloadContext ctx */) {
        // TODO port:1.20.1 - Body needs Minecraft.getInstance().level.getEntity, PhysicsEntity check,
        // DynamXPhysicsHelper.createExplosion. Re-port in Phase 5b/8.
    }
}
