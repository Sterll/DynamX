package fr.dynamx.client.gui;

import fr.aym.acsguis.component.GuiComponent;
import fr.aym.acsguis.component.layout.GuiScaler;
import fr.aym.acsguis.component.panel.GuiFrame;
import fr.aym.acsguis.component.textarea.GuiLabel;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.sync.ClientEntityNetHandler;
import fr.dynamx.client.camera.CameraSystem;
import fr.dynamx.client.network.ClientPhysicsEntitySynchronizer;
import fr.dynamx.client.network.ClientPhysicsSyncManager;
import fr.dynamx.common.entities.PackPhysicsEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

/**
 * Vehicle HUD frame.
 *
 * <p>TODO port:1.20.1 - Surface changes:</p>
 * <ul>
 *   <li>{@code MinecraftForge.EVENT_BUS} -> {@code MinecraftForge.EVENT_BUS}.</li>
 *   <li>{@code Gui.ICONS} -> {@code GuiComponent.GUI_ICONS_LOCATION} -> {@code Gui.GUI_ICONS_LOCATION}.</li>
 *   <li>{@code mc.getTextureManager().bindTexture(loc)} + {@code GuiTextureSprite.drawScaledCustomSizeModalRect}
 *       -> {@code GuiGraphics.blit(ResourceLocation, int, int, int, int, int, int, int, int)}.</li>
 *   <li>{@code Minecraft.getSystemTime()} -> {@code Util.getMillis()}.</li>
 *   <li>{@code riddenEntity.ticksExisted} -> {@code riddenEntity.tickCount}.</li>
 * </ul>
 * Public surface preserved; the network-warning icon draw is stubbed.
 */
public class VehicleHud extends GuiFrame {
    private final PackPhysicsEntity<?, ?> riddenEntity;
    private GuiLabel netWarning;
    private final List<ResourceLocation> styleSheets = new ArrayList<>();

    public VehicleHud(IModuleContainer.ISeatsContainer entity) {
        super(new GuiScaler.Identity());
        // TODO port:1.20.1 - IModuleContainer.cast() and IVehicleController/Hud methods all return
        // Object pending Phase 6/7. We cast through to keep the public surface; reflective access
        // for getSynchronizer()/getControllers().
        PackPhysicsEntity<?, ?> ent = (PackPhysicsEntity<?, ?>) entity.cast();
        this.riddenEntity = ent;
        CameraSystem.setupCamera(entity);
        setCssClass("root");
        List<IVehicleController> controllers = new ArrayList<>(((ClientEntityNetHandler) ent.getSynchronizer()).getControllers());
        VehicleEntityEvent.CreateHud event = new VehicleEntityEvent.CreateHud(this, styleSheets,
                ((fr.dynamx.common.entities.modules.SeatsModule) entity.getSeats()).isLocalPlayerDriving(),
                this.riddenEntity, controllers);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return;
        }
        controllers.forEach(c -> {
            List<ResourceLocation> hudStyle = c.getHudCssStyles();
            if (hudStyle != null)
                styleSheets.addAll(hudStyle);
            GuiComponent hud = (GuiComponent) c.createHud();
            if (hud != null) {
                add(hud);
            }
        });
        if (!(ent.getSynchronizer() instanceof ClientPhysicsEntitySynchronizer)) {
            return;
        }
        // TODO port:1.20.1 - drawTexturedBackground hook didn't survive the GuiLabel stub. Re-add when GuiLabel is fully ported.
        netWarning = new GuiLabel("");
        netWarning.setCssId("network_warning");
        netWarning.getStyleCustomizer().setPaddingLeft(14);
        add(netWarning);
    }

    @Override
    public boolean tick() {
        if (!super.tick()) {
            return false;
        }
        if (netWarning != null && ClientPhysicsSyncManager.pingMs > 100 && riddenEntity.tickCount % (20 * 3) < (20 * 2))
            netWarning.setText(ClientPhysicsSyncManager.getPingMessage());
        else if (netWarning != null && !netWarning.getText().isEmpty())
            netWarning.setText("");
        return true;
    }

    @Override
    public List<ResourceLocation> getCssStyles() {
        return styleSheets;
    }
}
