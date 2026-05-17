package fr.dynamx.api.events;

import fr.dynamx.client.renders.TESRDynamXBlock;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;

@Getter
public class DynamXBlockEvent extends Event {
    private final Dist side;
    @Nullable
    private final DynamXBlock<?> block;
    private final Level world;

    public DynamXBlockEvent(Dist side, DynamXBlock<?> dynamXBlock, Level world) {
        this.block = dynamXBlock;
        this.side = side;
        this.world = world;
    }

    @Setter
    @Getter
    public static class CreateTileEntity extends DynamXBlockEvent {
        private TEDynamXBlock tileEntity;

        public CreateTileEntity(Dist side, DynamXBlock<?> dynamXBlock, Level world, TEDynamXBlock tileEntity) {
            super(side, dynamXBlock, world);
            this.tileEntity = tileEntity;
        }
    }

    @Getter
    @Cancelable
    public static class RenderTileEntity extends DynamXBlockEvent {
        private final BaseRenderContext.BlockRenderContext renderContext;
        private final SceneNode<BaseRenderContext.BlockRenderContext, BlockObject<?>> sceneNode;
        private final TESRDynamXBlock<?> renderer;
        private final int destroyStage;
        private final float alpha;
        private final EventPhase eventPhase;

        public RenderTileEntity(DynamXBlock<?> dynamXBlock, BaseRenderContext.BlockRenderContext renderContext, SceneNode<BaseRenderContext.BlockRenderContext, BlockObject<?>> sceneNode, TESRDynamXBlock<?> renderer, int destroyStage, float alpha, EventPhase eventPhase) {
            super(Dist.CLIENT, dynamXBlock, renderContext.getTileEntity() != null ? renderContext.getTileEntity().getLevel() : null);
            this.renderContext = renderContext;
            this.sceneNode = sceneNode;
            this.renderer = renderer;
            this.destroyStage = destroyStage;
            this.alpha = alpha;
            this.eventPhase = eventPhase;
        }
    }
}
