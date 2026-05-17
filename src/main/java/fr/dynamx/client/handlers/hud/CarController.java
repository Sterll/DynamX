package fr.dynamx.client.handlers.hud;

import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.gui.VehicleHudPart;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;

import java.util.Collections;
import java.util.List;

/**
 * Controleur voiture (port 1.20.1). Lecture des touches identique au legacy.
 * {@link #createHud()} renvoie un {@link VehicleHudPart} dessine via {@link GuiGraphics}
 * (compteur + textes vitesse/rapport/handbrake).
 */
public class CarController extends BaseController {
    public static final ResourceLocation STYLE = new ResourceLocation(DynamXConstants.ID, "css/vehicle_hud.css");

    @Getter
    @Setter
    private static HudIcons hudIcons;

    protected final CarEngineModule engine;

    @Getter
    @Setter
    private float speedLimit;

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

    @Override
    @OnlyIn(Dist.CLIENT)
    public VehicleHudPart createHud() {
        float maxRpm = entity.getPackInfo().getSubPropertyByType(CarEngineInfo.class).getMaxRevs() + 3000;
        float scale = 90f / 300;
        SpeedometerPanel speed = new SpeedometerPanel(this, scale, maxRpm);

        VehicleHudPart[] icons;
        if (hudIcons != null) {
            icons = new VehicleHudPart[hudIcons.iconCount()];
            for (int i = 0; i < icons.length; i++) {
                icons[i] = new VehicleHudPart() {
                    @Override
                    public void render(GuiGraphics g, int sw, int sh, float partial) {
                    }
                };
                hudIcons.initIcon(i, icons[i]);
            }
        } else {
            icons = new VehicleHudPart[0];
        }

        return new VehicleHudPart() {
            @Override
            public void tick() {
                speed.tick();
                if (hudIcons != null)
                    hudIcons.tick(icons);
            }

            @Override
            public void render(GuiGraphics graphics, int screenWidth, int screenHeight, float partialTicks) {
                int gaugeW = Math.max(1, (int) (300 * scale));
                int gaugeH = Math.max(1, (int) (300 * scale));
                int gaugeX = screenWidth - gaugeW - 20;
                int gaugeY = screenHeight - gaugeH - 20;
                speed.setPosition(gaugeX, gaugeY);
                speed.render(graphics, screenWidth, screenHeight, partialTicks);

                Font font = Minecraft.getInstance().font;
                float[] engineProperties = engine.getEngineProperties();
                if (engineProperties != null) {
                    String speedTxt = engine.isEngineStarted()
                            ? String.valueOf(Math.abs((int) engineProperties[VehicleEntityProperties.EnumEngineProperties.SPEED.ordinal()]))
                            : "--";
                    graphics.drawCenteredString(font, speedTxt, gaugeX + gaugeW / 2, gaugeY + gaugeH / 2 - 6, 0xFFFFFFFF);

                    String gearTxt = getGearString((int) engineProperties[VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR.ordinal()]);
                    graphics.drawCenteredString(font, gearTxt, gaugeX + gaugeW / 2, gaugeY + gaugeH / 2 + 6, 0xFFFFFFFF);
                }

                if (speedLimit != Float.MAX_VALUE) {
                    graphics.drawString(font, Component.literal((int) speedLimit + " km/h"), gaugeX, gaugeY - 12, 0xFFE2E2E2, false);
                }

                if (ClientDebugSystem.enableDebugDrawing) {
                    int debugY = 4;
                    graphics.drawString(font, "Handbrake : " + (engine.isHandBraking() ? ChatFormatting.RED + "ON" : ChatFormatting.GREEN + "OFF"), 4, debugY, 0xFFFFFFFF);
                    String soundName = engine.getCurrentEngineSound() == null ? "none" : engine.getCurrentEngineSound().getSoundName();
                    graphics.drawString(font, "Sounds : " + soundName, 4, debugY + 10, 0xFFFFFFFF);
                }

                for (int i = 0; i < icons.length; i++) {
                    if (hudIcons != null && hudIcons.isVisible(i))
                        icons[i].render(graphics, screenWidth, screenHeight, partialTicks);
                }
            }
        };
    }

    @Override
    public List<ResourceLocation> getHudCssStyles() {
        return Collections.singletonList(STYLE);
    }

    protected String getGearString(int gear) {
        return gear == -1 ? "R" : gear == 0 ? (engine.isHandBraking() ? ChatFormatting.RED + "P" : "N") : "" + gear;
    }
}
