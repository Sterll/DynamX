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
        // TODO port:1.20.1 - draw the custom texture overlay:
        //   guiGraphics.blit(texture, getX() + 2, getY() + 2, 0, 0, width - 4, height - 4, 128, 128);
        // and on hover, guiGraphics.renderTooltip(font, getMessage(), mouseX, mouseY).
    }
}
