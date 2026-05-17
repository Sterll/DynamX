package fr.dynamx.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Element de HUD dessine par un controleur de vehicule.
 * Remplace l'ancien {@code fr.aym.acsguis.component.GuiComponent} dans le pipeline HUD vehicule.
 */
public interface VehicleHudPart {
    default void tick() {}

    void render(GuiGraphics graphics, int screenWidth, int screenHeight, float partialTicks);
}
