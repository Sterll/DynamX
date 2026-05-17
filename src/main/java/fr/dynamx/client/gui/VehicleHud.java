package fr.dynamx.client.gui;

import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.sync.ClientEntityNetHandler;
import fr.dynamx.client.camera.CameraSystem;
import fr.dynamx.client.network.ClientPhysicsEntitySynchronizer;
import fr.dynamx.client.network.ClientPhysicsSyncManager;
import fr.dynamx.common.entities.PackPhysicsEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD vehicule (port 1.20.1). Remplace l'ancien {@code GuiFrame} ACsGuis par un
 * {@link Screen} vanilla qui delegue le dessin aux {@link VehicleHudPart} fournis par
 * les controleurs.
 */
public class VehicleHud extends Screen {
    private final PackPhysicsEntity<?, ?> riddenEntity;
    private final List<VehicleHudPart> parts = new ArrayList<>();
    private String pingWarning = "";

    public VehicleHud(IModuleContainer.ISeatsContainer entity) {
        super(Component.literal("Vehicle HUD"));
        // TODO port:1.20.1 - IModuleContainer.cast() et IVehicleController.createHud() renvoient Object
        // en attendant la fin du portage des entites/modules.
        PackPhysicsEntity<?, ?> ent = (PackPhysicsEntity<?, ?>) entity.cast();
        this.riddenEntity = ent;
        CameraSystem.setupCamera(entity);

        List<IVehicleController> controllers = new ArrayList<>(((ClientEntityNetHandler) ent.getSynchronizer()).getControllers());
        VehicleEntityEvent.CreateHud event = new VehicleEntityEvent.CreateHud(this, new ArrayList<>(),
                ((fr.dynamx.common.entities.modules.SeatsModule) entity.getSeats()).isLocalPlayerDriving(),
                this.riddenEntity, controllers);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return;
        }
        for (IVehicleController c : controllers) {
            Object hud = c.createHud();
            if (hud instanceof VehicleHudPart) {
                parts.add((VehicleHudPart) hud);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        for (VehicleHudPart part : parts) {
            part.tick();
        }
        if (((PackPhysicsEntity<?, ?>) riddenEntity).getSynchronizer() instanceof ClientPhysicsEntitySynchronizer
                && ClientPhysicsSyncManager.pingMs > 100
                && riddenEntity.tickCount % (20 * 3) < (20 * 2)) {
            pingWarning = ClientPhysicsSyncManager.getPingMessage();
        } else {
            pingWarning = "";
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        for (VehicleHudPart part : parts) {
            part.render(graphics, this.width, this.height, partialTicks);
        }
        if (!pingWarning.isEmpty()) {
            Font font = Minecraft.getInstance().font;
            graphics.drawString(font, pingWarning, 14, this.height - 20, 0xFFFF5555, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // HUD overlay : pas de fond opaque.
    }
}
