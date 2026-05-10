package fr.dynamx.api.events.client;

import fr.dynamx.api.events.EventPhase;
import lombok.Getter;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired around armor rendering.
 *
 * TODO port:1.20.1 - BaseRenderContext.ArmorRenderContext and SceneNode live in Phase 7;
 *   typed as Object until then.
 */
@Getter
public class DynamXArmorRenderEvent extends Event implements ICancellableEvent {
    private final Object context;     // TODO port:1.20.1 - BaseRenderContext.ArmorRenderContext
    private final Object sceneGraph;  // TODO port:1.20.1 - SceneNode<?, ?>
    private final EventPhase eventPhase;

    public DynamXArmorRenderEvent(Object renderContext, Object sceneGraph, EventPhase eventPhase) {
        this.context = renderContext;
        this.sceneGraph = sceneGraph;
        this.eventPhase = eventPhase;
    }
}
