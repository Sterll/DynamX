package fr.dynamx.common.core.mixin;

import fr.dynamx.utils.DynamXConstants;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Hooks {@link Entity#move} to mix DynamX collisions into vanilla motion.
 *
 * <p>TODO port:1.20.1 - The 1.12 mixin {@code @Inject}-s and {@code @ModifyVariable}-s against
 * obfuscated targets:
 *   - {@code method = "move"} ({@code Lnet/minecraft/entity/Entity;move(...)V}) — in 1.20.1 the
 *     method exists as {@code Entity#move(MoverType, Vec3)} (mojmap), so the {@code method} string
 *     and the {@code @At target} descriptors must be re-derived against mojmap.
 *   - The 1.12 implementation walked the local axis-aligned-box variables (ordinal 0/1/2 doubles)
 *     to inject DynamX collision deltas; the 1.20.1 method works on a {@code Vec3} (no separate
 *     x/y/z locals) so the whole intercept needs to be rewritten as a {@code @ModifyVariable} on
 *     the {@code Vec3} or a {@code Vec3 #collide(Vec3)} wrap.
 *
 * <p>Mixin target preserved so the mixin config still binds; body cleared until the new injection
 * points are pinned.
 */
@Mixin(value = Entity.class, priority = 800, remap = DynamXConstants.REMAP)
public abstract class MixinEntity {
    // TODO port:1.20.1 - re-implement the move-collision intercept against 1.20.1 mojmap Entity#move.
}
