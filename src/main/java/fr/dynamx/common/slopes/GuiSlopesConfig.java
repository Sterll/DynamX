package fr.dynamx.common.slopes;

import fr.aym.acsguis.api.ACsGuiFrame;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * Slopes configuration GUI.
 *
 * <p>TODO port:1.20.1 - This screen is heavily coupled to ACsGuis and client-side classes; the
 * body has been stubbed to keep the public API (constructor + style location) so {@code ItemSlopes}
 * still compiles. The full GUI will be wired in Phase 7/8 along with the rest of the client.
 */
@ACsGuiFrame
public class GuiSlopesConfig extends GuiFrame {
    @ACsGuiFrame.RegisteredStyleSheet
    public static final ResourceLocation STYLE = new ResourceLocation(DynamXConstants.ID, "css/slope_generator.css");

    public GuiSlopesConfig(ItemStack stack) {
        super(new GuiScaler.Identity());
        // TODO port:1.20.1 - re-implement once client gui pipeline is ported (Phase 7/8).
    }

    @Override
    public void guiClose() {
        super.guiClose();
    }

    @Override
    public List<ResourceLocation> getCssStyles() {
        return Collections.singletonList(STYLE);
    }
}
