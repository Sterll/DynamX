package fr.dynamx.api.events.client;

import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.common.entities.PhysicsEntity;
import lombok.Getter;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;

/**
 * Fired when rendering a PhysicsEntity.
 *
 * <p>Cancellation per phase used to be expressed via @HasResult in 1.12; in NeoForge 1.20.1 there
 * is no direct equivalent, so addons should switch on {@link Type} themselves and stop processing
 * manually.
 */
@Getter
public class DynamXEntityRenderEvent extends Event {
    /**
     * The entity being rendered
     */
    @Nullable
    private final PhysicsEntity<?> entity;

    /**
     * The render context
     */
    private final BaseRenderContext.EntityRenderContext context;

    /**
     * The render type
     */
    private final Type renderType;

    /**
     * The render pass.
     */
    private final int renderPass;

    public DynamXEntityRenderEvent(@Nullable PhysicsEntity<?> entity, BaseRenderContext.EntityRenderContext context, Type renderType, int renderPass) {
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
