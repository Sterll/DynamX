package fr.dynamx.client.handlers.hud;

import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.common.contentpack.type.vehicle.CarEngineInfo;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.resources.ResourceLocation;

/**
 * <p>TODO port:1.20.1 - direct port; only ResourceLocation package update
 * ({@code net.minecraft.util} -> {@code net.minecraft.resources}).</p>
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
    public boolean tick() {
        if (!super.tick()) {
            return false;
        }
        prevValue = value;
        value = carController.engine.getEngineProperty(VehicleEntityProperties.EnumEngineProperties.REVS) * maxRevs;
        return true;
    }
}
