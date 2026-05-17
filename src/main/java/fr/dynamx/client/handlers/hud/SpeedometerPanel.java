package fr.dynamx.client.handlers.hud;

import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.common.contentpack.type.vehicle.CarEngineInfo;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.resources.ResourceLocation;

/**
 * Compteur de vitesse : utilise la texture rpm_curve et lit le regime moteur
 * pour piloter le remplissage. Hereditairement {@link CircleCounterPanel}.
 */
public class SpeedometerPanel extends CircleCounterPanel {
    private final CarController carController;
    private final float maxRevs;

    public SpeedometerPanel(CarController carController, float scale, float maxRpm) {
        super(new ResourceLocation(DynamXConstants.ID, "textures/rpm_curve.png"),
                false, 300, 300, scale, maxRpm);
        this.carController = carController;
        this.maxRevs = carController.entity.getPackInfo().getSubPropertyByType(CarEngineInfo.class).getMaxRevs();
    }

    @Override
    public void tick() {
        prevValue = value;
        value = carController.engine.getEngineProperty(VehicleEntityProperties.EnumEngineProperties.REVS) * maxRevs;
    }
}
