package fr.dynamx.api.entities.modules;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Reads inputs of vehicle controls from the user and add elements to the HUD while driving <br>
 * Used on client side
 *
 * @see IPhysicsModule
 * @see fr.dynamx.common.entities.modules.engines.CarEngineModule
 */
// TODO port:1.20.1 - ACsGuis (fr.aym.acsguis.component.GuiComponent) is not ported and may not have a
// 1.20.1 equivalent. createHud() returns Object as a placeholder so the API stays stable until a HUD
// solution is chosen.
public interface IVehicleController {
    /**
     * Called each tick to read controls
     */
    void update();

    /**
     * Called to create the vehicle HUD when the driver mounts, on client side <br>
     * Nullable : display nothing for this controller
     */
    // TODO port:1.20.1 - Return type was fr.aym.acsguis.component.GuiComponent.
    @Nullable
    @OnlyIn(Dist.CLIENT)
    Object createHud();

    /**
     * Called to get the vehicle HUD style when the driver mounts, on client side <br>
     * Nullable : display nothing for this controller
     */
    @Nullable
    @OnlyIn(Dist.CLIENT)
    List<ResourceLocation> getHudCssStyles();
}
