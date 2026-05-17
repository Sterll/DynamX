package fr.dynamx.client.handlers.hud;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.gui.VehicleHudPart;
import fr.dynamx.client.handlers.KeyHandler;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.engines.HelicopterEngineModule;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.List;

/**
 * Controleur helicoptere (port 1.20.1). HUD vanilla via {@link GuiGraphics}.
 *
 * <p>TODO port:1.20.1 - la gestion souris (anciennement {@code MouseEvent#getDx/getDy}) doit etre relue
 * depuis {@code Minecraft#mouseHandler} dans un evenement de tick, voir {@link #tickMouse(Object)}.</p>
 */
@Mod.EventBusSubscriber(modid = DynamXConstants.ID, value = Dist.CLIENT)
public class HelicopterController extends BaseController {
    public static final ResourceLocation STYLE = new ResourceLocation(DynamXConstants.ID, "css/vehicle_hud.css");

    protected final HelicopterEngineModule engine;

    /**
     * @param entity is assumed to implement {@link IModuleContainer.ISeatsContainer}
     */
    @OnlyIn(Dist.CLIENT)
    public HelicopterController(BaseVehicleEntity<?> entity, HelicopterEngineModule engine) {
        super(entity, engine);
        this.engine = engine;
        while (KeyHandler.KEY_HELICOPTER_PITCH_FORWARD.consumeClick()) ;
        while (KeyHandler.KEY_HELICOPTER_PITCH_BACKWARD.consumeClick()) ;
        while (KeyHandler.KEY_HELICOPTER_YAW_LEFT.consumeClick()) ;
        while (KeyHandler.KEY_HELICOPTER_YAW_RIGHT.consumeClick()) ;
        while (KeyHandler.KEY_LOCK_ROTATION.consumeClick()) ;
    }

    /**
     * <p>TODO port:1.20.1 - rebrancher sur un evenement client tick pour lire {@code mouseHandler.xpos/ypos}.</p>
     */
    @OnlyIn(Dist.CLIENT)
    public static void tickMouse(Object event) {
        // TODO port:1.20.1 - stub en attendant d'avoir le bon evenement source.
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void updateControls() {
        HelicopterEngineModule engine = entity.getModuleByType(HelicopterEngineModule.class);
        if (engine != null && engine.getEngineProperties() != null) {
            if (KeyHandler.KEY_POWERUP.consumeClick() && isEngineStarted) {
                engine.setPower(engine.getPower() + 0.05f);
            }
            if (KeyHandler.KEY_POWERDOWN.consumeClick() && isEngineStarted) {
                engine.setPower(engine.getPower() - 0.05f);
            }
            if (KeyHandler.KEY_LOCK_ROTATION.consumeClick()) {
                HelicopterEntity.setMouseLocked(!HelicopterEntity.isMouseLocked());
            }
            boolean rolling = false;
            if (KeyHandler.KEY_HELICOPTER_PITCH_FORWARD.isDown()) {
                engine.getRollControls().set(1, -25);
                rolling = true;
            }
            if (KeyHandler.KEY_HELICOPTER_PITCH_BACKWARD.isDown()) {
                engine.getRollControls().set(1, 25);
                rolling = true;
            }
            if (KeyHandler.KEY_HELICOPTER_YAW_LEFT.isDown()) {
                engine.getRollControls().set(0, -25);
                rolling = true;
            }
            if (KeyHandler.KEY_HELICOPTER_YAW_RIGHT.isDown()) {
                engine.getRollControls().set(0, 25);
                rolling = true;
            }
            handbraking = !rolling && KeyHandler.KEY_HANDBRAKE.isDown();

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
                int x = screenWidth - 120;
                int y = screenHeight - 50;
                if (engineProperties != null) {
                    String speedTxt = engine.isEngineStarted()
                            ? String.valueOf((int) engineProperties[VehicleEntityProperties.EnumEngineProperties.SPEED.ordinal()])
                            : "--";
                    graphics.drawString(font, speedTxt + " km/h", x, y, 0xFFFFFFFF, false);
                }
                graphics.drawString(font, String.format("Power %.2f", Math.abs(engine.getPower())), x, y + 12, 0xFFFFFFFF, false);
                graphics.drawString(font, "View locked " + HelicopterEntity.isMouseLocked(), 4, 4, 0xFFFFFFFF, false);
            }
        };
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<ResourceLocation> getHudCssStyles() {
        return Collections.singletonList(STYLE);
    }
}
