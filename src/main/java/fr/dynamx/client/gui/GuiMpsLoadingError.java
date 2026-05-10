package fr.dynamx.client.gui;

import fr.dynamx.common.DynamXMain;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/**
 * Loading-error screen shown after the main menu when DynamX MPS reports a fatal load failure.
 *
 * <p>TODO port:1.20.1 - {@code GuiCustomModLoadingErrorScreen} / {@code CustomModLoadingErrorDisplayException}
 * are gone in NeoForge 1.20.1. The new loading error pipeline is {@code LoadingErrorScreen} and uses
 * {@code ModLoadingException} / {@code ModLoadingWarning}. To keep behaviour for the user we ship a placeholder
 * {@link Screen} that simply forwards to the parent title screen when ESC is pressed.</p>
 */
public class GuiMpsLoadingError extends Screen {
    private final TitleScreen parent;

    public GuiMpsLoadingError(TitleScreen parent) {
        super(Component.literal("DynamX loading error"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        // TODO port:1.20.1 - rebuild the legacy "Ignore and continue" button + body as Button.builder(...).
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            DynamXMain.memoizedLoadingError = null;
            this.minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
