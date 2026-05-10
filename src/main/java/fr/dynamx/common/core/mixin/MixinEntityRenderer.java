package fr.dynamx.common.core.mixin;

import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * First step for our stencil test: clear and enable the stencil buffer before the entity render pass.
 *
 * <p>TODO port:1.20.1 - In 1.12 this mixin {@code @Inject}-ed inside
 * {@code EntityRenderer#renderWorldPass(IFJ)V} just before
 * {@code RenderGlobal#renderEntities(Lnet/minecraft/entity/Entity;...)V} and configured
 * the {@code GL11.GL_STENCIL_*} state, then called {@code ClientEventHandler.resetBigEntities()}.
 *
 * <p>In 1.20.1 ({@link GameRenderer}) the render loop is split between {@code renderLevel},
 * {@code LevelRenderer#renderLevel}, and {@code RenderSystem}/{@code RenderTarget}-managed
 * framebuffers. The MC main framebuffer also no longer guarantees a stencil attachment unless
 * we explicitly request one (Forge has an {@code enableStencil()} hook). The old descriptor
 * {@code renderWorldPass(IFJ)V} no longer exists, and direct {@code GL11.glStencil*} calls are
 * fragile under {@code RenderSystem}/core profile.
 *
 * <p>Body stubbed: re-derive the mojmap target + use the new RenderTarget stencil API once
 * stencil rendering is needed.
 */
@Mixin(value = GameRenderer.class, remap = DynamXConstants.REMAP)
public abstract class MixinEntityRenderer {
    // TODO port:1.20.1 - re-implement stencil enter-pass hook against GameRenderer#renderLevel.
}
