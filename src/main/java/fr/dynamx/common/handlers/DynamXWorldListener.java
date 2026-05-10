package fr.dynamx.common.handlers;

import fr.dynamx.common.DynamXContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Reacts to per-world events.
 *
 * <p>TODO port:1.20.1 - {@code IWorldEventListener} was removed in 1.18+. The relevant hook
 * ({@code onEntityRemoved}) is now provided by NeoForge {@code EntityLeaveLevelEvent}.
 * Class kept (no longer implements anything) so other classes can keep instantiating it.
 * Behavior should be migrated to a {@code @SubscribeEvent} on {@code EntityLeaveLevelEvent}.
 */
public class DynamXWorldListener {
    /**
     * Called from the event subscriber when an entity leaves the world.
     * Kept as a plain method so legacy code paths still compile.
     */
    public void onEntityRemoved(Entity entityIn) {
        if (entityIn instanceof Player player) {
            if (DynamXContext.getPlayerToCollision().containsKey(player)) {
                // TODO port:1.20.1 - PlayerPhysicsHandler API not ported yet; once available:
                // DynamXContext.getPlayerToCollision().get(player).removeFromWorld(true, player.level());
            }
        }
    }
}
