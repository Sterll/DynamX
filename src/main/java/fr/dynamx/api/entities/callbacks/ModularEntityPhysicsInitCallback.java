package fr.dynamx.api.entities.callbacks;

import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;

import javax.annotation.Nullable;

/**
 * Init callback for ModularEntityPhysicsInitCallback <br>
 * The physics handler is initialized in the first tick after the entity init
 */
// TODO port:1.20.1 - ModularPhysicsEntity is not yet ported (Phase 6 entities); the first parameter
// uses Object until that class is available.
public interface ModularEntityPhysicsInitCallback {

    /**
     * Fired when the entity physics handler has been initialized
     *
     * @param modularEntity  The entity
     * @param physicsHandler The created physics handler, or null if physics are not simulated on this side
     */
    void onPhysicsInit(Object modularEntity, @Nullable AbstractEntityPhysicsHandler<?, ?> physicsHandler);
}
