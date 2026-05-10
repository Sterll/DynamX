package fr.dynamx.common.core.mixin;

import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fix look-vector when riding a vehicle. 1.12 {@code EntityPlayerSP} -> 1.20.1 {@link LocalPlayer}.
 *
 * <p>TODO port:1.20.1 - The 1.12 mixin {@code @Overwrite}-d {@code getLook(float)} to ignore partial
 * ticks. In 1.20.1 the analogous method is {@code Entity#getViewVector(float)}, which lives on
 * {@link net.minecraft.world.entity.Entity} (not on {@link LocalPlayer}). Direct {@code @Overwrite}
 * here would shadow the parent — using a soft {@code @Inject} on {@code aiStep()} or
 * {@code tick()} is the way to go once we know exactly what we want. Body stubbed.
 */
@Mixin(value = LocalPlayer.class, remap = DynamXConstants.REMAP)
public abstract class MixinEntityPlayerSP {
    // TODO port:1.20.1 - resolve mojmap target for the riding view-vector override.
    @Inject(method = "tick", at = @At("HEAD"), cancellable = false)
    private void dynamX$tick(CallbackInfoReturnable<Vec3> cir) {
        // intentionally empty until the view-vector hook is reimplemented.
    }
}
