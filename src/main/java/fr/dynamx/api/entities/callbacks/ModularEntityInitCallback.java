package fr.dynamx.api.entities.callbacks;

import fr.dynamx.api.entities.modules.IPhysicsModule;

import java.util.List;

/**
 * Init callback for ModularPhysicsEntity <br>
 * The entity is initialized during the first tick of its existence
 */
// TODO port:1.20.1 - ModularPhysicsEntity is not yet ported (Phase 6 entities); the first parameter
// uses Object until that class is available, to preserve the callback shape.
public interface ModularEntityInitCallback {
    /**
     * Fired when the entity modules has been initialized
     *
     * @param modularEntity The modular entity
     * @param modules       The entity modules list, modifiable
     */
    void onEntityInit(Object modularEntity, List<IPhysicsModule<?>> modules);
}
