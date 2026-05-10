package fr.dynamx.common.contentpack.type.vehicle;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.common.contentpack.parts.PartRotor;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.utils.errors.DynamXErrorManager;

import java.util.List;

/**
 * TODO port:1.20.1 - Original referenced fr.dynamx.common.items.DynamXItemSpawner and the
 *   ItemCar / ItemBoat / ItemHelicopter / ItemTrailer item classes (fr.dynamx.common.items.vehicle).
 *   These live in Phase 6 (items). The getSpawnItem method now returns Object and constructs
 *   nothing; concrete validators throw UnsupportedOperationException for getSpawnItem until
 *   Phase 6 is ported.
 */
public interface VehicleValidator {
    default void initProperties(ModularVehicleInfo info) {
    }

    /**
     * @return The item used to spawn the vehicle. TODO port:1.20.1 - typed as Object pending Phase 6.
     */
    Object getSpawnItem(ModularVehicleInfo info);

    void validate(ModularVehicleInfo info);

    default Class<? extends BaseEngineInfo> getEngineClass() {
        return CarEngineInfo.class;
    }

    VehicleValidator CAR_VALIDATOR = new VehicleValidator() {
        @Override
        public Object getSpawnItem(ModularVehicleInfo info) {
            // TODO port:1.20.1 - return ItemCar.getItemForCar(info) once items are ported.
            throw new UnsupportedOperationException("ItemCar not ported (Phase 6)");
        }

        @Override
        public void validate(ModularVehicleInfo info) {
            CarEngineInfo engine = info.getSubPropertyByType(CarEngineInfo.class);
            if (engine == null) //This will prevent any crash when spawning the vehicle
                throw new IllegalArgumentException("Car " + info.getFullName() + " has no engine");
            if (engine.getEngineSounds() == null)
                DynamXErrorManager.addPackError(info.getPackName(), "config_error", ErrorLevel.FATAL, info.getName(), "This car has no sounds !");
            if (info.getPartsByType(PartWheel.class).isEmpty())
                DynamXErrorManager.addPackError(info.getPackName(), "config_error", ErrorLevel.FATAL, info.getName(), "This car has no wheels !");
        }
    };
    VehicleValidator TRAILER_VALIDATOR = new VehicleValidator() {
        @Override
        public Object getSpawnItem(ModularVehicleInfo info) {
            // TODO port:1.20.1 - return new ItemTrailer(info) once items are ported.
            throw new UnsupportedOperationException("ItemTrailer not ported (Phase 6)");
        }

        @Override
        public void validate(ModularVehicleInfo info) {
            if (info.getPartsByType(PartWheel.class).isEmpty())
                DynamXErrorManager.addPackError(info.getPackName(), "config_error", ErrorLevel.FATAL, info.getName(), "This trailer has no wheels !");
            if (info.getSubPropertyByType(TrailerAttachInfo.class) == null)
                DynamXErrorManager.addPackError(info.getPackName(), "config_error", ErrorLevel.FATAL, info.getName(), "Missing trailer config !");
        }
    };
    VehicleValidator BOAT_VALIDATOR = new VehicleValidator() {
        @Override
        public void initProperties(ModularVehicleInfo info) {
            info.angularDamping = 0.5f;
        }

        @Override
        public Object getSpawnItem(ModularVehicleInfo info) {
            // TODO port:1.20.1 - return new ItemBoat(info) once items are ported.
            throw new UnsupportedOperationException("ItemBoat not ported (Phase 6)");
        }

        @Override
        public void validate(ModularVehicleInfo info) {
            BoatEngineInfo engine = info.getSubPropertyByType(BoatEngineInfo.class);
            if (engine != null && engine.getEngineSounds() == null) {
                DynamXErrorManager.addPackError(info.getPackName(), "config_error", ErrorLevel.FATAL, info.getName(), "The boat engine has no sounds !");
            }
            if (info.getSubPropertyByType(BoatPropellerInfo.class) == null)
                throw new IllegalArgumentException("Boat " + info.getFullName() + " has no propeller");
        }

        @Override
        public Class<? extends BaseEngineInfo> getEngineClass() {
            return BoatEngineInfo.class;
        }
    };
    VehicleValidator HELICOPTER_VALIDATOR = new VehicleValidator() {
        @Override
        public void initProperties(ModularVehicleInfo info) {
            info.linearDamping = 0.5f;
            info.angularDamping = 0.9f;
            info.inWaterAngularDamping = 0.9f;
        }

        @Override
        public Object getSpawnItem(ModularVehicleInfo info) {
            // TODO port:1.20.1 - return new ItemHelicopter(info) once items are ported.
            throw new UnsupportedOperationException("ItemHelicopter not ported (Phase 6)");
        }

        @Override
        public void validate(ModularVehicleInfo info) {
            HelicopterPhysicsInfo physicsInfo = info.getSubPropertyByType(HelicopterPhysicsInfo.class);
            if (physicsInfo == null)
                throw new IllegalArgumentException("Helicopter " + info.getFullName() + " has no HelicopterPhysics");
            BaseEngineInfo engine = info.getSubPropertyByType(BaseEngineInfo.class);
            if (engine == null)
                throw new IllegalArgumentException("Helicopter " + info.getFullName() + " has no engine");
            List<PartRotor> rotors = info.getPartsByType(PartRotor.class);
            if (rotors.isEmpty())
                throw new IllegalArgumentException("Helicopter " + info.getFullName() + " has no rotors");
        }

        @Override
        public Class<? extends BaseEngineInfo> getEngineClass() {
            return BaseEngineInfo.class;
        }
    };
}
