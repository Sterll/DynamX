package fr.dynamx.common.entities.vehicles;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.contentpack.type.vehicle.TrailerAttachInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.physics.entities.BaseWheeledVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

import static fr.dynamx.client.ClientProxy.SOUND_HANDLER;

/**
 * Trailer wheeled vehicle entity; can be attached to a {@link CarEntity} via a hitch joint.
 */
// TODO port:1.20.1 - BaseWheeledVehiclePhysicsHandler/WheelsPhysicsHandler/TrailerAttachInfo/ClientProxy.SOUND_HANDLER forward (Phase 4b/7).
public class TrailerEntity<T extends TrailerEntity.TrailerPhysicsHandler<?>> extends BaseVehicleEntity<T> implements IModuleContainer.IDoorContainer, IModuleContainer.ISeatsContainer {
    private WheelsModule wheels;
    private DoorsModule doors;
    private SeatsModule seats;

    public TrailerEntity(EntityType<? extends TrailerEntity<?>> type, Level level) {
        super(type, level);
    }

    public TrailerEntity(EntityType<? extends TrailerEntity<?>> type, String name, Level world, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(type, name, world, pos, spawnRotationAngle, metadata);
    }

    // TODO port:1.20.1 - Legacy (Level) constructor.
    public TrailerEntity(Level level) {
        super(level);
    }

    @Override
    public T createPhysicsHandler() {
        return (T) new TrailerPhysicsHandler(this);
    }

    @Override
    protected void getListenerModules() {
        super.getListenerModules();
        seats = getModuleByType(SeatsModule.class);
        wheels = getModuleByType(WheelsModule.class);
        doors = getModuleByType(DoorsModule.class);
    }

    @Nonnull
    public WheelsModule getWheels() {
        return wheels;
    }

    @Override
    public PackPhysicsEntity<?, ?> cast() {
        return this;
    }

    @Override
    public ModularVehicleInfo createInfo(String infoName) {
        return DynamXObjectLoaders.TRAILERS.findInfo(infoName);
    }

    @Override
    public DoorsModule getDoors() {
        return doors;
    }

    @Nonnull
    @Override
    public SeatsModule getSeats() {
        return seats;
    }

    @OnlyIn(Dist.CLIENT)
    public void playAttachSound() {
        TrailerAttachInfo info = getPackInfo().getSubPropertyByType(TrailerAttachInfo.class);
        if (info.getAttachSound() != null)
            SOUND_HANDLER.playSingleSound(physicsPosition, info.getAttachSound(), 1, 1);
    }

    public static class TrailerPhysicsHandler<A extends TrailerEntity<?>> extends BaseWheeledVehiclePhysicsHandler<A> {
        public TrailerPhysicsHandler(A entity) {
            super(entity);
        }

        public WheelsPhysicsHandler getWheels() {
            return getHandledEntity().getWheels().getPhysicsHandler(); //WHEELS
        }
    }
}
