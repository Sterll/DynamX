// TODO port:1.20.1 stub - GuiFrame top-level GUI class
package fr.aym.acsguis.component.panel;

import fr.aym.acsguis.component.layout.GuiScaler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;

public abstract class GuiFrame extends GuiPanel {
    protected final Minecraft mc = Minecraft.getInstance();
    private boolean pauseGame = true;

    public GuiFrame() {}

    public GuiFrame(GuiScaler scaler) {}

    public void setPauseGame(boolean pause) { this.pauseGame = pause; }

    public boolean doesPauseGame() { return pauseGame; }

    public boolean needsCssReload() { return true; }

    public boolean usesDefaultStyle() { return true; }

    public List<ResourceLocation> getCssStyles() { return Collections.emptyList(); }

    public Screen getGuiScreen() {
        // TODO port:1.20.1 stub - return a dummy Screen
        return new Screen(Component.literal("Stub")) {};
    }
}
