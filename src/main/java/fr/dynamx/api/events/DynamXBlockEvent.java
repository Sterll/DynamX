package fr.dynamx.api.events;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.Cancelable;

import javax.annotation.Nullable;

/**
 * Block-related DynamX events.
 *
 * TODO port:1.20.1 - Originally referenced DynamXBlock / TEDynamXBlock / TESRDynamXBlock /
 *   BaseRenderContext.BlockRenderContext / BlockObject / SceneNode. All those live in
 *   fr.dynamx.common.blocks, fr.dynamx.client.renders.* and fr.dynamx.common.contentpack.type.objects
 *   (Phases 3b, 5, 7). They are typed as Object here to keep the API surface compilable;
 *   they should be re-tightened as their packages are ported.
 * TODO port:1.20.1 - Side -&gt; Dist (physical side; events historically used logical Side).
 */
@Getter
@Cancelable
public class DynamXBlockEvent extends Event {
    private final Dist side;
    @Nullable
    private final Object block; // TODO port:1.20.1 - DynamXBlock<?>
    private final Level world;

    public DynamXBlockEvent(Dist side, Object dynamXBlock, Level world) {
        this.block = dynamXBlock;
        this.side = side;
        this.world = world;
    }

    @Setter
    @Getter
    public static class CreateTileEntity extends DynamXBlockEvent {
        private Object tileEntity; // TODO port:1.20.1 - TEDynamXBlock

        public CreateTileEntity(Dist side, Object dynamXBlock, Level world, Object tileEntity) {
            super(side, dynamXBlock, world);
            this.tileEntity = tileEntity;
        }
    }

    @Getter
    public static class RenderTileEntity extends DynamXBlockEvent {
        private final Object renderContext;     // TODO port:1.20.1 - BaseRenderContext.BlockRenderContext
        private final Object sceneNode;         // TODO port:1.20.1 - SceneNode<BlockRenderContext, BlockObject<?>>
        private final Object renderer;          // TODO port:1.20.1 - TESRDynamXBlock<?>
        private final int destroyStage;
        private final float alpha;
        private final EventPhase eventPhase;

        public RenderTileEntity(Object dynamXBlock, Object renderContext, Object sceneNode, Object renderer, int destroyStage, float alpha, EventPhase eventPhase) {
            // TODO port:1.20.1 - world used to come from renderContext.getTileEntity().getWorld(); pass null until BlockRenderContext is ported.
            super(Dist.CLIENT, dynamXBlock, null);
            this.renderContext = renderContext;
            this.sceneNode = sceneNode;
            this.renderer = renderer;
            this.destroyStage = destroyStage;
            this.alpha = alpha;
            this.eventPhase = eventPhase;
        }
    }
}
