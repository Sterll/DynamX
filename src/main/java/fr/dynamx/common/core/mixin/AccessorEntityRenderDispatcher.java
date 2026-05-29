package fr.dynamx.common.core.mixin;

import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the private {@code shouldRenderShadow} flag of {@link EntityRenderDispatcher}. It is
 * {@code true} during world rendering and set to {@code false} by the inventory/GUI entity renderer,
 * so it lets us tell a world render from a GUI render (used to avoid hiding a seated player's model
 * in the inventory while cancelling its duplicate world render).
 */
@Mixin(value = EntityRenderDispatcher.class, remap = DynamXConstants.REMAP)
public interface AccessorEntityRenderDispatcher {
    @Accessor("shouldRenderShadow")
    boolean dynamx$shouldRenderShadow();
}
