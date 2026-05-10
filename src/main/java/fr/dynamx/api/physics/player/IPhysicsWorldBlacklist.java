package fr.dynamx.api.physics.player;

import net.minecraft.world.entity.player.Player;

/**
 * Interface for PhysicsWorld Blacklist
 */
public interface IPhysicsWorldBlacklist {

    /**
     * @param player - The Player that will be added in Physics World
     * @return isBlacklisted from PhysicsWorld
     */
    boolean isBlacklisted(Player player);

}
