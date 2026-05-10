package fr.dynamx.common.core.mixin;

import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Second step for our stencil test: render the "AntiWater" mask meshes of {@code BaseVehicleEntity}
 * into the stencil buffer (1.12 ASM target: {@code RenderManager} -> 1.20.1 {@link EntityRenderDispatcher}).
 *
 * <p>TODO port:1.20.1 - In 1.12 this would have been a {@code @Inject} after
 * {@code Render#doRender(Entity, double, double, double, float, float)V} inside
 * {@code RenderManager#renderEntity}. The body uses fixed-function GL ({@code GL11.glColorMask},
 * {@code GlStateManager.translate/rotate}, etc.) and the deprecated {@code GlQuaternionPool}.
 *
 * <p>In 1.20.1:
 * <ul>
 *   <li>The closest method is {@code EntityRenderDispatcher#render(Entity, ..., PoseStack, MultiBufferSource, int)};
 *       there is no longer a single {@code doRender(Entity, ...)V} call.</li>
 *   <li>Fixed-function GL is unsupported under {@code RenderSystem}; the stencil state has to be
 *       set via {@code com.mojang.blaze3d.platform.GlStateManager} (or via a
 *       {@code RenderType} that owns the stencil setup).</li>
 *   <li>{@code GlQuaternionPool} / {@code QuaternionPool} are part of the old 1.12 client utils;
 *       the new path is {@code com.mojang.math.Quaternionf} / {@code PoseStack#mulPose(Quaternionf)}.</li>
 * </ul>
 *
 * <p>Body stubbed (the legacy body was already commented out as "todo Yanis"); the {@code @Mixin}
 * target is kept so the mixin config still binds.
 */
@Mixin(value = EntityRenderDispatcher.class, remap = DynamXConstants.REMAP)
public abstract class MixinRenderManager {
    // TODO port:1.20.1 - re-inject the AntiWater stencil-mask pass into EntityRenderDispatcher#render
    //   using PoseStack / RenderType-based stencil instead of GL11/GlStateManager.
}
