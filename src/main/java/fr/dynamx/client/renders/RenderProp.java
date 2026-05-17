package fr.dynamx.client.renders;

import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.entities.PropsEntity;
import fr.dynamx.utils.debug.renderer.BoatDebugRenderer;
import fr.dynamx.utils.debug.renderer.DebugRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import javax.annotation.Nullable;

/**
 * Renderer for prop entities.
 *
 * <p>TODO port:1.20.1 -
 * <ul>
 *   <li>{@code DynamXContext.getDxModelRegistry().getModel(...)} - DynamXContext isn't ported yet;
 *       getRenderContext returns null until that registry lands.</li>
 *   <li>{@code MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(...))} -
 *       {@code MinecraftForge.EVENT_BUS.post(...)} ; commented out because the event class isn't ported.</li>
 *   <li>Constructor switched from {@code RenderManager} to {@code EntityRendererProvider.Context}.</li>
 * </ul>
 *
 * @param <T> The prop entity type
 */
public class RenderProp<T extends PropsEntity<?>> extends RenderPhysicsEntity<T> {
    protected final BaseRenderContext.EntityRenderContext context = new BaseRenderContext.EntityRenderContext(this);

    public RenderProp(EntityRendererProvider.Context context) {
        super(context);
        addDebugRenderers(new BoatDebugRenderer.FloatsDebug(), new DebugRenderer.StoragesDebug());
        // TODO port:1.20.1 - was: MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(PropsEntity.class, this));
    }

    @Override
    @Nullable
    public BaseRenderContext.EntityRenderContext getRenderContext(T entity) {
        // TODO port:1.20.1 - DynamXContext.getDxModelRegistry() isn't ported yet (Phase 5/8).
        // Once it lands:
        //   if (entity.getPackInfo() == null) return null;
        //   DxModelRenderer modelRenderer = DynamXContext.getDxModelRegistry().getModel(entity.getPackInfo().getModel());
        //   if (modelRenderer == null) return null;
        //   return context.setModelParams(entity, modelRenderer, entity.getEntityTextureId());
        return null;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void renderEntity(T entity, BaseRenderContext.EntityRenderContext context) {
        SceneNode<?, ?> sceneGraph = entity.getPackInfo().getSceneGraph();
        if (sceneGraph != null) {
            ((SceneNode) sceneGraph).render(context, entity.getPackInfo(), null);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void renderEntityDebug(T entity, BaseRenderContext.EntityRenderContext context) {
        SceneNode<?, ?> sceneGraph = entity.getPackInfo().getSceneGraph();
        if (sceneGraph != null) {
            ((SceneNode) sceneGraph).renderDebug(context, entity.getPackInfo());
        }
    }
}
