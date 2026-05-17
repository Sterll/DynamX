package fr.dynamx.client.handlers.hud;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.gui.VehicleHudPart;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.engines.BoatPropellerModule;
import fr.dynamx.utils.DynamXConstants;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;

import java.util.Collections;
import java.util.List;

/**
 * Controleur bateau (port 1.20.1). HUD vanilla : affiche uniquement la vitesse via {@link GuiGraphics}.
 */
public class BoatController extends BaseController {
    public static final ResourceLocation STYLE = new ResourceLocation(DynamXConstants.ID, "css/vehicle_hud.css");

    @Getter
    @Setter
    private static HudIcons hudIcons;

    protected final BoatPropellerModule engine;

    /**
     * @param entity is assumed to implement {@link IModuleContainer.ISeatsContainer}
     */
    public BoatController(BaseVehicleEntity<?> entity, BoatPropellerModule engine) {
        super(entity, engine);
        this.engine = engine;
    }

    @Override
    protected void updateControls() {
        if (engine.getEngineProperties() != null) {
            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.ControllerUpdate<>(entity, this));
            int controls = 0;
            if (accelerating)
                controls = controls | 2;
            if (handbraking)
                controls = controls | 32;
            if (reversing)
                controls = controls | 4;
            if (turningLeft)
                controls = controls | 8;
            if (turningRight)
                controls = controls | 16;
            if (isEngineStarted)
                controls = controls | 1;
            engine.setControls(controls);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public VehicleHudPart createHud() {
        return new VehicleHudPart() {
            @Override
            public void render(GuiGraphics graphics, int screenWidth, int screenHeight, float partialTicks) {
                Font font = Minecraft.getInstance().font;
                float[] engineProperties = engine.getEngineProperties();
                if (engineProperties == null)
                    return;
                String speedTxt = engine.isEngineStarted()
                        ? String.valueOf((int) engineProperties[VehicleEntityProperties.EnumEngineProperties.SPEED.ordinal()])
                        : "--";
                int x = screenWidth - 100;
                int y = screenHeight - 30;
                graphics.drawString(font, speedTxt + " km/h", x, y, 0xFFFFFFFF, false);
            }
        };
    }

    @Override
    public List<ResourceLocation> getHudCssStyles() {
        return Collections.singletonList(STYLE);
    }
}
