package fr.dynamx.api.events.client;

import lombok.Getter;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired around item rendering.
 *
 * TODO port:1.20.1 - BaseRenderContext.ItemRenderContext and AbstractItemNode live in Phase 7;
 *   typed as Object until then.
 */
@Getter
public class DynamXRenderItemEvent extends Event implements ICancellableEvent {
    private final Object context;     // TODO port:1.20.1 - BaseRenderContext.ItemRenderContext
    private final Object sceneGraph;  // TODO port:1.20.1 - AbstractItemNode<?, ?>
    private final EventStage stage;

    public DynamXRenderItemEvent(Object context, Object sceneGraph, EventStage stage) {
        this.context = context;
        this.sceneGraph = sceneGraph;
        this.stage = stage;
    }

    public enum EventStage {
        PRE,
        RENDER,
        TRANSFORM,
        POST
    }
}
