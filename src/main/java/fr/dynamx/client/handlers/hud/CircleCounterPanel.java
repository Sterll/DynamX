package fr.dynamx.client.handlers.hud;

import fr.aym.acsguis.component.panel.GuiPanel;
import fr.aym.acsguis.utils.ComponentRenderContext;
import net.minecraft.resources.ResourceLocation;

/**
 * Circular gauge panel used by the speedometer.
 *
 * <p>TODO port:1.20.1 - the original used immediate-mode {@code GL_TRIANGLE_FAN} with
 * {@code Tessellator}/{@code BufferBuilder} + {@code POSITION_TEX} vertex format,
 * bound the texture via {@code TextureManager#bindTexture}, and used {@code GlStateManager}
 * scale/translate. All of that is gone in core profile. The replacement is:</p>
 * <ul>
 *   <li>{@code RenderSystem.setShader(GameRenderer::getPositionTexShader)} +
 *       {@code RenderSystem.setShaderTexture(0, texture)}.</li>
 *   <li>{@code Tesselator.getInstance()} + {@code BufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, POSITION_TEX)}.</li>
 *   <li>{@code poseStack.pushPose() / .translate() / .scale()} from the GuiGraphics provided by
 *       ComponentRenderContext.</li>
 *   <li>{@code GL11.glDisable(GL_SCISSOR_TEST)} -> {@code RenderSystem.disableScissor()}.</li>
 * </ul>
 * The full drawing routine is stubbed; the field values + signature are preserved.
 */
public class CircleCounterPanel extends GuiPanel {
    protected final ResourceLocation texture;
    protected final boolean rightToLeft;
    protected final int width, height;
    protected final float scale;
    protected final float maxValue;
    protected float prevValue, value;

    public CircleCounterPanel(ResourceLocation texture, boolean rightToLeft, int width, int height, float scale, float maxValue) {
        this.texture = texture;
        this.rightToLeft = rightToLeft;
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.maxValue = maxValue;
    }

    @Override
    public void drawBackground(int mouseX, int mouseY, float partialTicks, ComponentRenderContext renderContext) {
        super.drawBackground(mouseX, mouseY, partialTicks, renderContext);
        // TODO port:1.20.1 - re-implement the 3-quadrant triangle-fan disc fill on top of
        // ComponentRenderContext's GuiGraphics. See class javadoc for the full migration plan.
    }
}
