package fr.dynamx.client.handlers.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.dynamx.client.gui.VehicleHudPart;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Jauge circulaire utilisee par le compteur. Le portage 1.20.1 abandonne le
 * triangle-fan immediate-mode et dessine une texture pleine masquee par un
 * rectangle qui suit la progression de la valeur courante.
 */
public class CircleCounterPanel implements VehicleHudPart {
    protected final ResourceLocation texture;
    protected final boolean rightToLeft;
    protected final int width;
    protected final int height;
    protected final float scale;
    protected final float maxValue;
    protected float prevValue;
    protected float value;
    protected int posX;
    protected int posY;

    public CircleCounterPanel(ResourceLocation texture, boolean rightToLeft, int width, int height, float scale, float maxValue) {
        this.texture = texture;
        this.rightToLeft = rightToLeft;
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.maxValue = maxValue;
    }

    public void setPosition(int x, int y) {
        this.posX = x;
        this.posY = y;
    }

    @Override
    public void render(GuiGraphics graphics, int screenWidth, int screenHeight, float partialTicks) {
        int w = Math.max(1, (int) (width * scale));
        int h = Math.max(1, (int) (height * scale));
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        graphics.blit(texture, posX, posY, 0, 0, w, h, w, h);
        float ratio = maxValue <= 0 ? 0 : Math.min(1f, Math.max(0f, value / maxValue));
        int filled = (int) (w * ratio);
        if (rightToLeft) {
            graphics.fill(posX, posY, posX + (w - filled), posY + h, 0x80000000);
        } else {
            graphics.fill(posX + filled, posY, posX + w, posY + h, 0x80000000);
        }
        RenderSystem.disableBlend();
    }
}
