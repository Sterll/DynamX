package fr.dynamx.common.contentpack.type.vehicle;

import fr.dynamx.api.contentpack.registry.PackFileProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Engine contained in an engine file
 *
 * TODO port:1.20.1 - Original referenced fr.dynamx.api.entities.modules.ModuleListBuilder,
 *   fr.dynamx.common.entities.{BaseVehicleEntity, PackPhysicsEntity},
 *   fr.dynamx.common.entities.modules.engines.CarEngineModule and
 *   fr.dynamx.common.entities.vehicles.CarEntity (Phase 6 dependencies). The addModules
 *   override has been relaxed to take Object parameters and is a no-op stub until those
 *   types are ported.
 */
public class CarEngineInfo extends BaseEngineInfo {
    @Getter
    @Setter
    @PackFileProperty(configNames = "SteeringMethod", required = false, defaultValue = "0")
    public int steeringMethod = 0;

    @Getter
    @Setter
    @PackFileProperty(configNames = "TurnSpeed", required = false, defaultValue = "0.09")
    public float turnSpeed = 0.09f;

    public List<GearInfo> gears = new ArrayList<>();

    byte i = 0;

    public CarEngineInfo(String packName, String name) {
        super(packName, name);
    }

    @Override
    public void addGear(GearInfo gear) {
        gear.setId(i);
        gears.add(i, gear);
        i++;
    }

    @Override
    public void addModules(Object entity, Object modules) {
        // TODO port:1.20.1 - Re-enable once PackPhysicsEntity / ModuleListBuilder / CarEngineModule
        //   / CarEntity / BaseVehicleEntity are ported (Phase 6).
        //   Original logic:
        //     if(!(entity instanceof CarEntity))
        //         throw new IllegalArgumentException("CarEngineInfo can only be used on CarEntity");
        //     modules.add(new CarEngineModule((BaseVehicleEntity<?>) entity, this));
    }
}
