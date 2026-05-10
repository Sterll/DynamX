package fr.dynamx.api.events.client;

import lombok.Getter;
import net.neoforged.bus.api.Event;

import javax.annotation.Nullable;

/**
 * Fired when rendering a PhysicsEntity.
 *
 * TODO port:1.20.1 - PhysicsEntity (fr.dynamx.common.entities) and BaseRenderContext.EntityRenderContext
 *   (fr.dynamx.client.renders.scene) are not yet ported (Phases 6/7); typed as Object here.
 *   Cancellation per phase used to be expressed via @HasResult; in NeoForge 1.20.1 there is no
 *   direct equivalent, so addons should switch on {@link Type} themselves and stop processing
 *   manually.
 */
@Getter
public class DynamXEntityRenderEvent extends Event {
    /**
     * The entity being rendered
     */
    @Nullable
    private final Object entity; // TODO port:1.20.1 - PhysicsEntity<?>

    /**
     * The render context
     */
    private final Object context; // TODO port:1.20.1 - BaseRenderContext.EntityRenderContext

    /**
     * The render type
     */
    private final Type renderType;

    /**
     * The render pass.
     */
    private final int renderPass;

    public DynamXEntityRenderEvent(Object entity, Object context, Type renderType, int renderPass) {
        this.entity = entity;
        this.context = context;
        this.renderType = renderType;
        this.renderPass = renderPass;
    }

    public enum Type {
        /**
         * Fired before rendering the entity. Cancellable.
         */
        ENTITY,
        /**
         * Fired before spawning particles. Cancellable.
         */
        PARTICLES,
        /**
         * Fired before rendering debug. Cancellable.
         */
        DEBUG,
        /**
         * Fired after rendering the entity. Not cancellable.
         */
        POST
    }
}
