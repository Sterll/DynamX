package fr.dynamx.common.physics.player;

import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.IRotatedCollisionHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

/**
 * Responsible to update a walking player <br>
 * A walking player is a player standing on the top of a {@link PhysicsEntity} <br>
 * This controller teleport the player each tick at his standing position, relative to the entity, computed when he landed on the entity <br>
 * WalkingOnPlayerControllers are added by the {@link IRotatedCollisionHandler}, and removed when the player moves
 */
public class WalkingOnPlayerController {
    public static WalkingOnPlayerController controller;

    public Player player;
    public PhysicsEntity<?> entity;
    public Direction face;
    public Vector3f offset;

    public WalkingOnPlayerController(Player player, PhysicsEntity<?> entity, Direction face, Vector3f offset) {
        this.player = player;
        this.entity = entity;
        this.face = face;
        this.offset = offset;
        if (DynamXContext.getPlayerToCollision().containsKey(player)) {
            DynamXContext.getPlayerToCollision().get(player).removeFromWorld(false, player.level());
        }
    }

    /**
     * Teleport the player to the right pos and disables arms animation
     */
    public void applyOffset() {
        Vector3f newPos = Vector3fPool.get((float) entity.getX(), (float) entity.getY(), (float) entity.getZ());
        newPos.addLocal(DynamXGeometry.rotateVectorByQuaternion(offset, entity.physicsRotation));//PhysicsHelper.getRotatedPoint(offset, -entity.rotationPitch, entity.rotationYaw, entity.rotationRoll));
        player.xo = player.getX();
        player.yo = player.getY();
        player.zo = player.getZ();
        player.setPos(newPos.x, newPos.y, newPos.z);
        player.walkAnimation.setSpeed(0);
    }

    /**
     * Should be called on the client of the player holding this controller <br>
     * Syncs the state of the controller, and restores player rigid body
     */
    public void disable() {
        controller = null;
        entity.walkingOnPlayers.remove(player);
        DynamXContext.getWalkingPlayers().remove(player);
        entity.getSynchronizer().onWalkingPlayerChange(player.getId(), offset, (byte) -1);
        if (!player.isPassenger() && DynamXContext.getPlayerToCollision().containsKey(player)) {
            DynamXContext.getPlayerToCollision().get(player).addToWorld();
        }
    }
}
