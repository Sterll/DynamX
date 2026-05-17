package fr.dynamx.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A button that renders a custom texture on top of the vanilla button skin.
 *
 * <p>TODO port:1.20.1 - Heavy rewrite:</p>
 * <ul>
 *   <li>{@code GuiButton} -> {@code Button} (Builder pattern).</li>
 *   <li>{@code mc.getTextureManager().bindTexture(...)} + {@code drawTexturedModalRect} are gone;
 *       use {@link GuiGraphics#blit(ResourceLocation, int, int, int, int, int, int)} or
 *       {@code blitNineSliced} for the button background, and
 *       {@code GuiGraphics#blit(ResourceLocation, int, int, int, int, int, int, int, int)} for the icon overlay.</li>
 *   <li>{@code GlStateManager.color/blendFunc/enableBlend} is gone; rendering uses RenderType + shaders.</li>
 *   <li>{@code mc.currentScreen.drawHoveringText} -> {@code GuiGraphics.renderTooltip}.</li>
 * </ul>
 * Constructor signature kept compatible; body of the render is stubbed to let {@code Button#renderWidget}
 * handle the standard button look.
 */
public class GuiTexturedButton extends Button {
    private final ResourceLocation texture;

    public GuiTexturedButton(int x, int y, int widthIn, int heightIn, String buttonText, ResourceLocation texture) {
        super(Button.builder(Component.literal(buttonText), btn -> {
        }).bounds(x, y, widthIn, heightIn));
        this.texture = texture;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        int ix = getX() + 2;
        int iy = getY() + 2;
        int iw = Math.max(0, width - 4);
        int ih = Math.max(0, height - 4);
        if (iw > 0 && ih > 0) {
            guiGraphics.blit(texture, ix, iy, 0, 0, iw, ih, iw, ih);
        }
    }
}
