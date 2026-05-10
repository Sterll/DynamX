package fr.dynamx.client.handlers.hud;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.client.handlers.KeyHandler;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.engines.BasicEngineModule;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Common base for vehicle controllers (car, boat, helicopter).
 *
 * <p>TODO port:1.20.1 - only annotation/API renames:</p>
 * <ul>
 *   <li>{@code @SideOnly(Side.CLIENT)} -> {@code @OnlyIn(Dist.CLIENT)}.</li>
 *   <li>{@code Minecraft.getMinecraft()} -> {@code Minecraft.getInstance()}.</li>
 *   <li>{@code KeyBinding.isPressed()} -> {@code KeyMapping.consumeClick()}; {@code isKeyDown()} -> {@code isDown()}.</li>
 *   <li>{@code mc.gameSettings.keyBindForward} -> {@code mc.options.keyUp}; same for back/left/right.</li>
 * </ul>
 */
public abstract class BaseController implements IVehicleController {
    @OnlyIn(Dist.CLIENT)
    protected static final Minecraft MC = Minecraft.getInstance();

    protected final BaseVehicleEntity<?> entity;

    @Getter
    @Setter
    protected boolean accelerating, reversing;
    @Getter
    @Setter
    protected boolean handbraking;
    @Getter
    @Setter
    protected boolean turningLeft, turningRight, isEngineStarted;
    @Getter
    @Setter
    protected byte onCooldown;

    /**
     * @param entity is assumed to implement {@link IModuleContainer.ISeatsContainer}
     */
    @OnlyIn(Dist.CLIENT)
    public BaseController(BaseVehicleEntity<?> entity, BasicEngineModule engine) {
        this.entity = entity;
        isEngineStarted = engine.isEngineStarted();
        handbraking = engine.isHandBraking();
        while (KeyHandler.KEY_HANDBRAKE.consumeClick()) ;
        while (KeyHandler.KEY_SPEED_LIMITIER.consumeClick()) ;
        while (KeyHandler.KEY_ENGINE_ON.consumeClick()) ;
        while (KeyHandler.KEY_LOCK_DOOR.consumeClick()) ;
    }

    protected abstract void updateControls();

    @Override
    @OnlyIn(Dist.CLIENT)
    public void update() {
        if (((IModuleContainer.ISeatsContainer) entity).getSeats().isLocalPlayerDriving()) {
            accelerating = MC.options.keyUp.isDown();
            reversing = MC.options.keyDown.isDown();
            turningLeft = MC.options.keyLeft.isDown();
            turningRight = MC.options.keyRight.isDown();
            if (KeyHandler.KEY_HANDBRAKE.consumeClick())
                handbraking = !handbraking;
            if (onCooldown > 0)
                onCooldown--;
            if (KeyHandler.KEY_ENGINE_ON.consumeClick()) {
                if (onCooldown == 0) {
                    isEngineStarted = !isEngineStarted;
                    onCooldown = 40;
                }
            }
            updateControls();
        }
    }
}
