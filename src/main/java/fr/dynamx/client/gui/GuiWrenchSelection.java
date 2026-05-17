package fr.dynamx.client.gui;

import com.jme3.math.FastMath;
import fr.dynamx.common.items.tools.ItemWrench;
import fr.dynamx.common.items.tools.WrenchMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Selecteur radial de mode pour la cle a molette. Port 1.20.1 : remplace l'ancien
 * {@code GuiFrame} ACsGuis par un {@link Screen} vanilla. Le disque rendu en triangle-fan
 * est remplace par un fond semi-transparent et un libelle centre.
 */
public class GuiWrenchSelection extends Screen {
    private WrenchMode currentMode;

    public GuiWrenchSelection() {
        super(Component.literal("Wrench selection"));
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft != null && minecraft.player != null) {
            ItemStack itemStack = minecraft.player.getMainHandItem();
            if (itemStack.getItem() instanceof ItemWrench) {
                currentMode = WrenchMode.getCurrentMode(itemStack);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            WrenchMode wrenchMode = getModeWithMousePos((int) mouseX, (int) mouseY);
            WrenchMode.sendWrenchMode(wrenchMode);
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(0, 0, this.width, this.height, 0x88000000);
        int cx = this.width / 2;
        int cy = this.height / 2;
        graphics.fill(cx - 110, cy - 110, cx + 110, cy + 110, 0x60000000);
        WrenchMode mode = getModeWithMousePos(mouseX, mouseY);
        String label = I18n.get(mode.getLabel());
        graphics.drawCenteredString(font, label, cx, cy - font.lineHeight / 2, 0xFFFFFFFF);
        if (currentMode != null) {
            graphics.drawCenteredString(font, I18n.get("wrench.current") + " : " + I18n.get(currentMode.getLabel()), cx, cy + 20, 0xFFCCCCCC);
        }
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private WrenchMode getModeWithMousePos(int mouseX, int mouseY) {
        float mx = mouseX - this.width / 2f;
        float my = mouseY - this.height / 2f;
        int maxModes = WrenchMode.getWrenchModes().size() - 1;
        if (maxModes <= 0)
            return WrenchMode.getWrenchModes().get(0);
        double theta = FastMath.atan2(my, mx);
        theta += FastMath.PI / maxModes;
        theta += FastMath.PI;
        theta = theta % (FastMath.PI * 2);

        int mode = (int) (theta / (2 * FastMath.PI / maxModes));
        int index = mode == 5 ? 6 : maxModes - mode - 1;
        index = Math.max(0, Math.min(WrenchMode.getWrenchModes().size() - 1, index));
        return WrenchMode.getWrenchModes().get(index);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
