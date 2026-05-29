package fr.dynamx.common.core;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.common.contentpack.parts.BasePartSeat;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * Teleports the player to a valid position next to the seat when dismounting from a vehicle.
 *
 * <p>port:1.20.1 - re-implemented from the 1.12 version. API renames applied:
 * {@code world} -&gt; {@code level()}, {@code world.collidesWithAnyBlock(aabb)} -&gt;
 * {@code !level().noCollision(aabb)}, {@code setPositionAndUpdate} -&gt; {@code teleportTo},
 * {@code AxisAlignedBB} -&gt; {@code AABB}. Only the DynamX seat-container path is ported (the
 * vanilla boat/horse fallback is handled by Minecraft itself).
 */
public class DismountHelper {
    public static void preDismount(LivingEntity dismounter, Entity entityIn) {
        if (!(entityIn instanceof IModuleContainer.ISeatsContainer)) {
            return;
        }
        IModuleContainer.ISeatsContainer vehicleEntity = (IModuleContainer.ISeatsContainer) entityIn;
        SeatsModule seats = (SeatsModule) vehicleEntity.getSeats();
        if (seats == null) {
            return;
        }
        // The player is still riding at HEAD of stopRiding(), so the current seat is the right one;
        // fall back to the last ridden seat if it has already been cleared.
        BasePartSeat seat = seats.getRidingSeat(dismounter);
        if (seat == null) {
            seat = seats.getLastRiddenSeat();
        }
        if (seat == null) {
            return;
        }
        PhysicsEntity<?> physics = (PhysicsEntity<?>) vehicleEntity.cast();
        Vector3fPool.openPool();
        try {
            // Drop the player one block to the seat's side (outwards), rotated with the vehicle.
            Vector3f side = Vector3fPool.get(seat.getPosition().x > 0 ? 1 : -1, 0, 0);
            Vector3f dismountPosition = DynamXGeometry.rotateVectorByQuaternion(seat.getPosition().add(side), physics.physicsRotation)
                    .addLocal(physics.physicsPosition);
            AABB box = new AABB(dismountPosition.x, dismountPosition.y + 1, dismountPosition.z,
                    dismountPosition.x + 1, dismountPosition.y + 2, dismountPosition.z + 1);
            if (dismounter.level().noCollision(box)) {
                dismounter.teleportTo(dismountPosition.x, box.minY, dismountPosition.z);
            } else {
                // Side blocked: try the opposite side instead.
                Vector3f otherSide = Vector3fPool.get(seat.getPosition().x > 0 ? -2 : 2, 0, 0);
                dismountPosition = DynamXGeometry.rotateVectorByQuaternion(seat.getPosition().add(otherSide), physics.physicsRotation)
                        .addLocal(physics.physicsPosition);
                box = new AABB(dismountPosition.x, dismountPosition.y + 1, dismountPosition.z,
                        dismountPosition.x + 1, dismountPosition.y + 2, dismountPosition.z + 1);
                dismounter.teleportTo(dismountPosition.x, box.minY, dismountPosition.z);
            }
        } finally {
            Vector3fPool.closePool();
        }
    }
}
