package fr.dynamx.common.network.packets;

import com.jme3.math.Vector3f;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.TrailerAttachModule;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.common.entities.vehicles.TrailerEntity;
import fr.dynamx.utils.DynamXUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.LogicalSide;

import java.util.List;

/**
 * Server-bound request to attach the trailer the player is currently sitting on.
 */
public class MessageAttachTrailer implements IDnxPacket {

    public MessageAttachTrailer() {
    }

    @Override
    public void fromBytes(ByteBuf byteBuf) {
    }

    @Override
    public void toBytes(ByteBuf byteBuf) {
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void handleUDPReceive(Player context, LogicalSide side) {
        if (side != LogicalSide.SERVER || context == null || context.level() == null) {
            return;
        }
        if (!(context.getVehicle() instanceof CarEntity)) {
            return;
        }
        CarEntity<?> carEntity = (CarEntity<?>) context.getVehicle();
        TrailerAttachModule trailerAttachModule = carEntity.getModuleByType(TrailerAttachModule.class);
        if (trailerAttachModule == null || trailerAttachModule.getAttachPoint() == null) {
            return;
        }
        Vector3f attachPoint = trailerAttachModule.getAttachPoint();
        float x = (float) carEntity.getX() + attachPoint.x;
        float y = (float) carEntity.getY() + attachPoint.y;
        float z = (float) carEntity.getZ() + attachPoint.z;
        AABB searchArea = carEntity.getBoundingBox().inflate(20);
        List<TrailerEntity> candidates = carEntity.level().getEntitiesOfClass(TrailerEntity.class, searchArea);
        TrailerEntity<?> trailer = null;
        double bestDistance = Double.MAX_VALUE;
        for (TrailerEntity<?> candidate : candidates) {
            double dx = candidate.getX() - x;
            double dy = candidate.getY() - y;
            double dz = candidate.getZ() - z;
            double dist = dx * dx + dy * dy + dz * dz;
            if (dist < bestDistance) {
                bestDistance = dist;
                trailer = candidate;
            }
        }
        if (trailer == null) {
            return;
        }
        DynamXUtils.attachTrailer(context, (BaseVehicleEntity<?>) carEntity, (BaseVehicleEntity<?>) trailer);
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
