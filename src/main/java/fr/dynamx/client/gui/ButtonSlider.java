package fr.dynamx.client.gui;

import fr.dynamx.client.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.client.gui.narration.NarrationElementOutput;

/**
 * Custom slider used in the Sound Options menu to control DynamX master sound volume.
 *
 * <p>TODO port:1.20.1 - The whole legacy {@code GuiButton}/{@code mouseDragged}/{@code drawTexturedModalRect}
 * pipeline is gone. In 1.20.1 sliders extend {@code AbstractSliderButton} and render through
 * {@code GuiGraphics}. Rather than recreating the exact look-and-feel here, we expose the same API as before
 * (categoryName, volume, displayString) and stub the rendering to defer to the parent button. The integration
 * point in {@code ClientEventHandler#initMainMenu} must be rewritten against {@code ScreenEvent.Init.Post}
 * with a {@code Screens.getButtons} lookup or {@code addButton} replacement.</p>
 */
public class ButtonSlider extends AbstractButton {
    private final String categoryName;
    private float volume = 1.0F;
    private final String offDisplayString = I18n.get("options.off");

    public ButtonSlider(int x, int y, boolean isMaster, String buttonText) {
        super(x, y, isMaster ? 310 : 150, 20, Component.literal(buttonText));
        this.categoryName = buttonText;
        this.volume = ClientProxy.SOUND_HANDLER.getMasterVolume();
        setMessage(Component.literal(categoryName + ": " + getDisplayString()));
    }

    @Override
    public void onPress() {
        // TODO port:1.20.1 - the legacy press recorded a "pressed" flag and then mouseDragged tracked the slider.
        // Re-implement as AbstractSliderButton or use a custom click+drag handler when the screen is rewritten.
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // TODO port:1.20.1 - draw the slider knob via GuiGraphics.blit on WIDGETS_LOCATION sprite. Stubbed: parent draws background only.
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        // Reflect current value:
        setMessage(Component.literal(categoryName + ": " + getDisplayString()));
    }

    /**
     * <p>TODO port:1.20.1 - mouseDragged is no longer called via positional args; AbstractSliderButton drives this through
     * onDrag. Public method kept for API compatibility but currently a no-op.</p>
     */
    public void mouseDragged(double mouseX) {
        volume = (float) (mouseX - (getX() + 4)) / (float) (width - 8);
        volume = Mth.clamp(volume, 0.0F, 1.0F);
        ClientProxy.SOUND_HANDLER.setMasterVolume(volume);
        setMessage(Component.literal(categoryName + ": " + getDisplayString()));
    }

    protected String getDisplayString() {
        return volume == 0.0F ? offDisplayString : (int) (volume * 100.0F) + "%";
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
