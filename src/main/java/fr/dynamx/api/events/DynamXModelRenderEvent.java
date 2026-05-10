package fr.dynamx.api.events;

import lombok.Getter;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Model render events.
 *
 * TODO port:1.20.1 - Originally referenced ObjModelRenderer, ObjObjectRenderer (OBJ loader,
 *   dropped in 1.20.1) and IModelTextureVariantsSupplier (fr.dynamx.api.dxmodel, not yet ported).
 *   Fields are typed as Object to keep API surface stable until rendering is reimplemented.
 */
@Getter
public class DynamXModelRenderEvent extends Event {
    private final EventPhase stage;
    private final Object model;            // TODO port:1.20.1 - was ObjModelRenderer (dropped)
    private final Object textureSupplier;  // TODO port:1.20.1 - was IModelTextureVariantsSupplier
    private final byte textureId;

    public DynamXModelRenderEvent(EventPhase stage, Object model, Object textureSupplier, byte textureId) {
        this.stage = stage;
        this.model = model;
        this.textureSupplier = textureSupplier;
        this.textureId = textureId;
    }

    public static class RenderFullModel extends DynamXModelRenderEvent implements ICancellableEvent {
        public RenderFullModel(EventPhase stage, Object model, Object textureSupplier, byte textureId) {
            super(stage, model, textureSupplier, textureId);
        }
    }

    public static class RenderMainParts extends DynamXModelRenderEvent implements ICancellableEvent {
        public RenderMainParts(EventPhase stage, Object model, Object textureSupplier, byte textureId) {
            super(stage, model, textureSupplier, textureId);
        }
    }

    public static class RenderPart extends DynamXModelRenderEvent implements ICancellableEvent {
        @Getter
        private final Object objObjectRenderer; // TODO port:1.20.1 - was ObjObjectRenderer (dropped)

        public RenderPart(EventPhase stage, Object model, Object textureSupplier, byte textureId, Object objObjectRenderer) {
            super(stage, model, textureSupplier, textureId);
            this.objObjectRenderer = objObjectRenderer;
        }
    }
}
