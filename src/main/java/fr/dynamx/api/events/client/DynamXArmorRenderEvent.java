package fr.dynamx.api.events.client;

import fr.dynamx.api.events.EventPhase;
import lombok.Getter;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * Fired around armor rendering.
 *
 * TODO port:1.20.1 - BaseRenderContext.ArmorRenderContext and SceneNode live in Phase 7;
 *   typed as Object until then.
 */
@Getter
@Cancelable
public class DynamXArmorRenderEvent extends Event {
    private final Object context;     // TODO port:1.20.1 - BaseRenderContext.ArmorRenderContext
    private final Object sceneGraph;  // TODO port:1.20.1 - SceneNode<?, ?>
    private final EventPhase eventPhase;

    public DynamXArmorRenderEvent(Object renderContext, Object sceneGraph, EventPhase eventPhase) {
        this.context = renderContext;
        this.sceneGraph = sceneGraph;
        this.eventPhase = eventPhase;
    }
}
