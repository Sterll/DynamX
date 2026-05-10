package fr.dynamx.common.handlers;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Legacy 1.12 {@code IGuiHandler} bridge for entity/block container UIs.
 *
 * <p>TODO port:1.20.1 - {@code IGuiHandler} no longer exists. GUI dispatch in 1.20.1 is done via
 * {@code MenuType} + {@code MenuScreens.register} on the client. The 1.12 ID-based scheme
 * (ID == 1 -> entity storage, ID >= 2 -> block storage) needs to be reimplemented as a network
 * packet that requests the server-side open of a {@code MenuType}. The class is preserved as a
 * placeholder so {@code DynamXMain} keeps compiling; both methods return null.
 */
public class DynamXGuiHandler {

    @Nullable
    public Object getServerGuiElement(int ID, Player player, Level world, int x, int y, int z) {
        // TODO port:1.20.1 - dispatch via MenuType / NetworkHooks.openScreen() (Phase 5+ network).
        return null;
    }

    @Nullable
    public Object getClientGuiElement(int ID, Player player, Level world, int x, int y, int z) {
        // TODO port:1.20.1 - register screen via MenuScreens.register on client init (Phase 7/8).
        return null;
    }
}
