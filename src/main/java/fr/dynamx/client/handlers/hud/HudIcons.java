package fr.dynamx.client.handlers.hud;

import fr.dynamx.client.gui.VehicleHudPart;

/**
 * API publique : permet aux addons d'afficher des icones personnalisees sur le HUD vehicule.
 * Un seul addon peut definir les icones via {@link CarController#setHudIcons(HudIcons)}.
 */
public interface HudIcons {
    int iconCount();

    void initIcon(int componentId, VehicleHudPart component);

    void tick(VehicleHudPart[] components);

    boolean isVisible(int componentId);
}
