package fr.dynamx.common.entities.vehicles;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.entities.modules.engines.BoatPropellerModule;
import fr.dynamx.common.physics.entities.BoatPhysicsHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Boat vehicle entity: seats, propeller, doors.
 */
// TODO port:1.20.1 - BoatPhysicsHandler/DynamXObjectLoaders/ModularVehicleInfo forward (Phase 4b/7).
public class BoatEntity<T extends BoatPhysicsHandler<?>> extends BaseVehicleEntity<T> implements IModuleContainer.ISeatsContainer, IModuleContainer.IDoorContainer {
    private SeatsModule seats;
    private BoatPropellerModule propeller;
    private DoorsModule doors;

    public BoatEntity(EntityType<? extends BoatEntity<?>> type, Level level) {
        super(type, level);
    }

    public BoatEntity(EntityType<? extends BoatEntity<?>> type, String name, Level world, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(type, name, world, pos, spawnRotationAngle, metadata);
    }

    // TODO port:1.20.1 - Legacy (Level) constructor.
    public BoatEntity(Level level) {
        super(level);
    }

    @Override
    public T createPhysicsHandler() {
        return (T) new BoatPhysicsHandler(this);
    }

    @Override
    protected void getListenerModules() {
        super.getListenerModules();
        seats = getModuleByType(SeatsModule.class);
        propeller = getModuleByType(BoatPropellerModule.class);
        doors = getModuleByType(DoorsModule.class);
    }

    @Nonnull
    public BoatPropellerModule getPropeller() {
        return propeller;
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

    @Override
    public ModularVehicleInfo createInfo(String infoName) {
        return DynamXObjectLoaders.BOATS.findInfo(infoName);
    }

    @Override
    public void onPackInfosReloaded() {
        super.onPackInfosReloaded();
        if (physicsHandler != null)
            physicsHandler.onPackInfosReloaded();
    }

    @Nullable
    @Override
    public DoorsModule getDoors() {
        return doors;
    }
}
