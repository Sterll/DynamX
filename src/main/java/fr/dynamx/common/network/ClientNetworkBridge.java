package fr.dynamx.common.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Isolates client-only class references so the dedicated-server classloader never
 * touches {@code net.minecraft.client.*} during packet dispatch wiring. Invocations
 * are routed through {@link net.minecraftforge.fml.DistExecutor} so this class is
 * only loaded on the physical client.
 */
public final class ClientNetworkBridge {
    private ClientNetworkBridge() {
    }

    public static Player getLocalPlayer() {
        return Minecraft.getInstance().player;
    }
}
