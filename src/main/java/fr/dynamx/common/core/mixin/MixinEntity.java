package fr.dynamx.common.core.mixin;

import fr.dynamx.api.physics.IRotatedCollisionHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Hooks {@link Entity#move} to feed DynamX rotated-entity collisions into the motion vector before
 * vanilla's {@code collide(Vec3)} runs.
 *
 * <p>The 1.12 implementation injected after several {@code getEntityBoundingBox()} calls and walked
 * three double locals (x/y/z); in 1.20.1 the entire collision pipeline runs on a single {@code Vec3}
 * returned by {@code Entity#collide(Vec3)}. Intercepting the argument going INTO {@code collide}
 * lets us clamp the motion against DynamX physics entities while still letting vanilla apply its
 * block-AABB clamp on top, matching the legacy stacking order.
 *
 * <p>The intercept is a no-op for {@link fr.dynamx.common.entities.PhysicsEntity} themselves (they
 * own their own collision via Bullet) and for entities matching the configured ignore patterns.
 */
@Mixin(value = Entity.class, priority = 800, remap = DynamXConstants.REMAP)
public abstract class MixinEntity {
    @ModifyArg(method = "move", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 dynamx$applyRotatedCollisions(Vec3 motion) {
        Entity self = (Entity) (Object) this;
        IRotatedCollisionHandler handler = DynamXContext.getCollisionHandler();
        if (handler == null) {
            return motion;
        }
        double[] clamped = handler.handleCollisionWithBulletEntities(self, MoverType.SELF, motion.x, motion.y, motion.z);
        if (!handler.motionHasChanged()) {
            return motion;
        }
        return new Vec3(clamped[0], clamped[1], clamped[2]);
    }
}
