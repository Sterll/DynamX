package fr.dynamx.client.handlers.hud;

import fr.aym.acsguis.component.GuiComponent;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.textarea.UpdatableGuiLabel;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.handlers.KeyHandler;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.engines.HelicopterEngineModule;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;

import java.util.Collections;
import java.util.List;

/**
 * <p>TODO port:1.20.1 - migration:</p>
 * <ul>
 *   <li>{@code @Mod.EventBusSubscriber(value = Side.CLIENT)} -> {@code @Mod.EventBusSubscriber(value = Dist.CLIENT)}.</li>
 *   <li>{@code MouseEvent} mouse-move data (event.getDx/getDy) -> {@code InputEvent.MouseScrollingEvent}
 *       doesn't carry dx/dy; the heli mouse rotation needs to be read from {@code Minecraft.getInstance().mouseHandler}
 *       via {@code MouseHandler#xpos}, {@code ypos} and tick-deltas, or from {@code MovementInputUpdateEvent}.</li>
 *   <li>{@code MC.gameSettings.invertMouse} -> {@code MC.options.invertYMouse().get()}.</li>
 *   <li>{@code MC.player.getRidingEntity()} -> {@code MC.player.getVehicle()}.</li>
 *   <li>{@code MinecraftForge.EVENT_BUS} -> {@code MinecraftForge.EVENT_BUS}; KeyBinding -> KeyMapping renames.</li>
 * </ul>
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
     * <p>TODO port:1.20.1 - was {@code MouseEvent} which carried dx/dy. 1.20's
     * {@code InputEvent.MouseScrollingEvent} only has the scroll delta. The mouse-pitch/roll
     * for the heli needs to be read from {@code MovementInputUpdateEvent} or directly polled
     * from {@code Minecraft.getInstance().mouseHandler}.</p>
     */
    @OnlyIn(Dist.CLIENT)
    public static void tickMouse(/* InputEvent.MouseScrollingEvent */ Object event) {
        // TODO port:1.20.1 - @SubscribeEvent removed; restore once parameter is a real Event subtype.
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void updateControls() {
        HelicopterEngineModule engine = entity.getModuleByType(HelicopterEngineModule.class);
        if (engine.getEngineProperties() != null && engine != null) {
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

    //HUD

    @Override
    @OnlyIn(Dist.CLIENT)
    public GuiComponent createHud() {
        GuiPanel panel = new GuiPanel();
        GuiPanel speed = new GuiPanel();
        speed.setCssClass("speed_pane");
        float[] engineProperties = engine.getEngineProperties();
        speed.add(new UpdatableGuiLabel("%s", (UpdatableGuiLabel.LabelValueFunction) val -> val.set(engine.isEngineStarted() ? (int) engineProperties[VehicleEntityProperties.EnumEngineProperties.SPEED.ordinal()] : "--", "")).setCssId("engine_speed"));
        speed.add(new UpdatableGuiLabel("Power %.2f", (UpdatableGuiLabel.LabelValueFunction) val -> val.set(Math.abs(engine.getPower()))).setCssId("engine_gear"));
        panel.add(new UpdatableGuiLabel("View locked %b", (UpdatableGuiLabel.LabelValueFunction) val -> val.set(HelicopterEntity.isMouseLocked())).setCssId("engine_gear"));
        panel.setCssId("engine_hud");
        panel.add(speed);
        return panel;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<ResourceLocation> getHudCssStyles() {
        return Collections.singletonList(STYLE);
    }
}
