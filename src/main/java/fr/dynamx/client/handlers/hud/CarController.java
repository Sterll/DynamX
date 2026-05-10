package fr.dynamx.client.handlers.hud;

import fr.aym.acsguis.api.ACsGuiFrame;
import fr.aym.acsguis.component.GuiComponent;
import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.aym.acsguis.component.textarea.UpdatableGuiLabel;
import fr.aym.acsguis.cssengine.style.EnumCssStyleProperty;
import fr.aym.acsguis.utils.GuiConstants;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.client.handlers.KeyHandler;
import fr.dynamx.common.contentpack.type.vehicle.CarEngineInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.engines.CarEngineModule;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.client.ClientDynamXUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;

import java.util.Collections;
import java.util.List;

/**
 * <p>TODO port:1.20.1 - direct port; only renames:</p>
 * <ul>
 *   <li>{@code TextFormatting} -> {@code ChatFormatting}.</li>
 *   <li>{@code MinecraftForge.EVENT_BUS} -> {@code MinecraftForge.EVENT_BUS}.</li>
 *   <li>{@code @SideOnly(Side.CLIENT)} -> {@code @OnlyIn(Dist.CLIENT)}.</li>
 *   <li>{@code KeyBinding.isPressed/isKeyDown} -> {@code KeyMapping.consumeClick/isDown}.</li>
 *   <li>{@code net.minecraft.util.ResourceLocation} -> {@code net.minecraft.resources.ResourceLocation}.</li>
 * </ul>
 */
@ACsGuiFrame
public class CarController extends BaseController {
    @ACsGuiFrame.RegisteredStyleSheet
    public static final ResourceLocation STYLE = new ResourceLocation(DynamXConstants.ID, "css/vehicle_hud.css");

    @Getter
    @Setter
    private static HudIcons hudIcons;

    protected final CarEngineModule engine;

    @Getter
    @Setter
    private float speedLimit;

    /**
     * @param entity is assumed to implement {@link IModuleContainer.ISeatsContainer}
     */
    public CarController(BaseVehicleEntity<?> entity, CarEngineModule engine) {
        super(entity, engine);
        this.engine = engine;
        speedLimit = engine.getSpeedLimit();
    }

    @Override
    protected void updateControls() {
        if (engine.getEngineProperties() != null) {
            if (engine.getEngineProperties()[VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR.ordinal()] != 0)
                handbraking = KeyHandler.KEY_HANDBRAKE.isDown();
            if (KeyHandler.KEY_SPEED_LIMITIER.consumeClick()) {
                if (speedLimit == Float.MAX_VALUE)
                    speedLimit = Math.abs(engine.getEngineProperties()[0]);
                else
                    speedLimit = Float.MAX_VALUE;
            }
            if (KeyHandler.KEY_ATTACH_TRAILER.consumeClick())
                ClientDynamXUtils.attachTrailer();
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
            engine.setSpeedLimit(speedLimit);
        }
    }

    //HUD

    @Override
    @OnlyIn(Dist.CLIENT)
    public GuiComponent createHud() {
        GuiPanel panel = new GuiPanel();
        float maxRpm = entity.getPackInfo().getSubPropertyByType(CarEngineInfo.class).getMaxRevs() + 3000;
        float scale = 90f / 300;
        GuiPanel speed = new SpeedometerPanel(this, scale, maxRpm);
        speed.setCssClass("speed_pane");
        speed.setCssId("speedometer_texture");

        float[] engineProperties = engine.getEngineProperties();
        speed.add(new UpdatableGuiLabel("%s", (UpdatableGuiLabel.LabelValueFunction) val ->
                val.set(engine.isEngineStarted() ? Math.abs((int) engineProperties[VehicleEntityProperties.EnumEngineProperties.SPEED.ordinal()]) : "--", ""))
            .setCssId("engine_speed"));
        speed.add(new UpdatableGuiLabel("%s", (UpdatableGuiLabel.LabelValueFunction) val ->
                val.set(getGearString((int) engineProperties[VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR.ordinal()])))
            .setCssId("engine_gear"));
        addRpmCounter(speed, scale, maxRpm);

        if (hudIcons != null) {
            GuiComponent[] icons = new GuiComponent[hudIcons.iconCount()];
            for (int i = 0; i < icons.length; i++) {
                int finalI = i;
                speed.add(icons[i] = new GuiPanel() {
                    @Override
                    public boolean isVisible() {
                        return hudIcons.isVisible(finalI);
                    }
                }.setCssId("icon_" + i).setCssClass("hud_icon"));
                hudIcons.initIcon(i, icons[i]);
            }
            speed.addTickListener(() -> hudIcons.tick(icons));
        } else {
            speed.add(new GuiPanel() {
                @Override
                public boolean isVisible() {
                    if (entity.getModuleByType(CarEngineModule.class) != null) {
                        return entity.getModuleByType(CarEngineModule.class).getSpeedLimit() != Float.MAX_VALUE;
                    }
                    return false;
                }
            }.setCssId("icon_1").setCssClass("hud_icon"));
        }
        panel.add(speed);

        speed.add(new UpdatableGuiLabel("%s", val -> {
            if (speedLimit != Float.MAX_VALUE) {
                val.set((int) speedLimit);
            } else {
                val.set("");
            }
        }).setCssId("speed_limit"));

        String cclass = ClientDebugSystem.enableDebugDrawing ? "hud_label_debug" : "hud_label_hidden";
        panel.add(new UpdatableGuiLabel("Handbrake : %s", (UpdatableGuiLabel.LabelValueFunction) val -> val.set((engine.isHandBraking() ? "§cON" : "§aOFF"))).setCssId("handbrake_state").setCssClass(cclass));
        panel.add(new UpdatableGuiLabel("Sounds : %s", (UpdatableGuiLabel.LabelValueFunction) val -> val.set((engine.getCurrentEngineSound() == null ? "none" : engine.getCurrentEngineSound().getSoundName()))).setCssId("engine_sounds").setCssClass(cclass));
        panel.setCssId("engine_hud");

        return panel;
    }

    @Override
    public List<ResourceLocation> getHudCssStyles() {
        return Collections.singletonList(STYLE);
    }

    protected String getGearString(int gear) {
        return gear == -1 ? "R" : gear == 0 ? (engine.isHandBraking() ? ChatFormatting.RED + "P" : "N") : "" + gear;
    }

    protected void addRpmCounter(GuiPanel speedometer, float scale, float maxRpm) {
        int nmL = (int) (maxRpm / 1000);

        for (int i = 1; i <= nmL; i++) {
            double angle = ((i * 3f / 2 * Math.PI) / nmL) - Math.PI / 3;

            float r = (150 - 29) * scale;

            double halfLetter = 6 * scale;

            double x = (45 - Math.cos(angle) * r) - Math.abs(halfLetter * Math.cos(angle));
            double y = (45 - Math.sin(angle) * r) - Math.abs(halfLetter * Math.sin(angle)) - 2;

            speedometer.add(new GuiLabel("" + i).setCssClass("rpm_letter").getStyleCustomizer().withAutoStyles((property, context, target) -> {
                if (property == EnumCssStyleProperty.LEFT) {
                    target.getXPos().setAbsolute(-(float) x, GuiConstants.ENUM_RELATIVE_POS.END);
                    return true;
                }
                if (property == EnumCssStyleProperty.TOP) {
                    target.getYPos().setAbsolute((float) y);
                    return true;
                }
                if (property == EnumCssStyleProperty.COLOR) {
                    if (angle > Math.PI - Math.PI / 3) {
                        target.setForegroundColor(0xFFE23F3F);
                    }
                    return true;
                }
                return false;
            }, EnumCssStyleProperty.LEFT, EnumCssStyleProperty.TOP, EnumCssStyleProperty.COLOR).getOwner());
        }
    }
}
