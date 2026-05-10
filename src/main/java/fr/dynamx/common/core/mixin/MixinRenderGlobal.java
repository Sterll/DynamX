package fr.dynamx.common.core.mixin;

import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Final step for our stencil test, and Optifine-shader-pass hook for rendering big DynamX entities
 * that live in not-rendered chunks (1.12 ASM target: {@code RenderGlobal} -> 1.20.1 {@link LevelRenderer}).
 *
 * <p>TODO port:1.20.1 - The 1.12 mixin injected twice in
 * {@code renderBlockLayer(BlockRenderLayer;DILnet/minecraft/entity/Entity;)I} (before/after the
 * inner call to {@code renderBlockLayer(BlockRenderLayer)V}) to flip the
 * {@code GL11.GL_STENCIL_*} state around the translucent pass, and once in {@code renderEntities}
 * just before {@code Shaders#endEntities()V} to call
 * {@code ClientEventHandler.renderBigEntities(partialTicks, false)}.
 *
 * <p>In 1.20.1, {@code BlockRenderLayer} is gone (now {@code RenderType}), and
 * {@link LevelRenderer} no longer exposes a {@code renderBlockLayer(BlockRenderLayer, ...)I}
 * with that descriptor. The new flow uses {@code renderSectionLayer(RenderType, ...)} on the
 * chunk-section render data. The Optifine shaders class {@code net.optifine.shaders.Shaders}
 * is also not stable in mojmap, so the {@code endEntities()V} injection point needs to be
 * re-pinned against the NeoForge+Optifine combo we ship with.
 *
 * <p>Body stubbed; the {@code @Mixin} target is kept so the mixin config still binds.
 */
@Mixin(value = LevelRenderer.class, remap = DynamXConstants.REMAP)
public abstract class MixinRenderGlobal {
    // TODO port:1.20.1 - re-inject around LevelRenderer#renderSectionLayer(RenderType.translucent())
    //   to manage the stencil buffer, and re-inject in renderEntities for the Optifine pass.
}
