package fr.dynamx.api.network.sync;

import fr.dynamx.api.entities.modules.IVehicleController;

import java.util.List;

/**
 * Client PhysicsEntitySynchronizer, handles controls and hud via the {@link IVehicleController}s
 */
// TODO port:1.20.1 - fr.dynamx.common.network.sync.PhysicsEntitySynchronizer is not ported yet (Phase 5).
public interface ClientEntityNetHandler {
    /**
     * The list of {@link IVehicleController}s loaded on this entity, added when a player starts controlling the entity. <br>
     * They are responsible for handling player inputs (keybindings), and the user interface (hud)
     *
     * @return The list of {@link IVehicleController}s loaded on this entity
     */
    List<IVehicleController> getControllers();
}
