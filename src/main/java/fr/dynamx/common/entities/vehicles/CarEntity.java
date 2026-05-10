package fr.dynamx.common.entities.vehicles;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.physics.entities.BaseWheeledVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * Wheeled car vehicle entity: seats, wheels, doors.
 */
// TODO port:1.20.1 - BaseWheeledVehiclePhysicsHandler/WheelsPhysicsHandler/DynamXObjectLoaders forward (Phase 4b/7).
public class CarEntity<T extends CarEntity.CarPhysicsHandler<?>> extends BaseVehicleEntity<T> implements
        IModuleContainer.ISeatsContainer, IModuleContainer.IDoorContainer {
    private SeatsModule seats;
    private WheelsModule wheels;
    private DoorsModule doors;

    public CarEntity(EntityType<? extends CarEntity<?>> type, Level level) {
        super(type, level);
    }

    public CarEntity(EntityType<? extends CarEntity<?>> type, String name, Level world, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(type, name, world, pos, spawnRotationAngle, metadata);
    }

    // TODO port:1.20.1 - Legacy (Level) constructor.
    public CarEntity(Level level) {
        super(level);
    }

    @Override
    public T createPhysicsHandler() {
        return (T) new CarPhysicsHandler(this);
    }

    @Override
    protected void getListenerModules() {
        super.getListenerModules();
        seats = getModuleByType(SeatsModule.class);
        wheels = getModuleByType(WheelsModule.class);
        doors = getModuleByType(DoorsModule.class);
    }

    @Override
    public ModularVehicleInfo createInfo(String infoName) {
        return DynamXObjectLoaders.WHEELED_VEHICLES.findInfo(infoName);
    }

    @Nonnull
    public WheelsModule getWheels() {
        return wheels;
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

    @Override
    public PackPhysicsEntity<?, ?> cast() {
        return this;
    }

    public static class CarPhysicsHandler<A extends CarEntity<?>> extends BaseWheeledVehiclePhysicsHandler<A> {
        public CarPhysicsHandler(A entity) {
            super(entity);
        }

        public WheelsPhysicsHandler getWheels() {
            return getHandledEntity().getWheels().getPhysicsHandler();
        }
    }
}
