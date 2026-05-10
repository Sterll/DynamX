package fr.dynamx.common.core.mixin;

import fr.dynamx.common.core.DismountHelper;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks the legacy {@code EntityLivingBase#dismountEntity} (1.12) — in 1.20.1 the equivalent is
 * {@link LivingEntity#stopRiding()}.
 *
 * <p>TODO port:1.20.1 - The 1.12 mixin {@code @Overwrite}-d {@code dismountEntity(Entity)}; in 1.20.1
 * the dismount path goes through {@code Entity#stopRiding()} -> {@code Entity#removeVehicle()} ->
 * {@code Entity#dismountTo(double,double,double)}, none of which take an {@link Entity} parameter.
 * We swap to a non-destructive {@code @Inject} at {@code HEAD} of {@code stopRiding()} and pass
 * the current vehicle to {@code DismountHelper.preDismount}.
 */
@Mixin(value = LivingEntity.class, remap = DynamXConstants.REMAP)
public abstract class MixinEntityLivingBase {

    @Inject(method = "stopRiding", at = @At("HEAD"))
    private void dynamX$preDismount(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        Entity vehicle = self.getVehicle();
        if (vehicle != null) {
            DismountHelper.preDismount(self, vehicle);
        }
    }
}
