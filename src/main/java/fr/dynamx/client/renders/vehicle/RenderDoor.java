package fr.dynamx.client.renders.vehicle;

import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.contentpack.parts.PartDoor;
import fr.dynamx.common.entities.vehicles.DoorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import javax.annotation.Nullable;

/**
 * Renderer for vehicle door entities.
 *
 * <p>TODO port:1.20.1 -
 * <ul>
 *   <li>Constructor switched from {@code RenderManager} to {@link EntityRendererProvider.Context}.</li>
 *   <li>{@code DynamXContext.getDxModelRegistry().getModel(...)} - DynamXContext isn't ported yet
 *       (Phase 5/8); {@link #getRenderContext} returns null until then.</li>
 *   <li>{@code MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(...))} -&gt; NeoForge bus.</li>
 *   <li>{@code GlStateManager.pushMatrix / scale / popMatrix} -&gt; {@code PoseStack#pushPose / scale / popPose}.</li>
 *   <li>{@code DynamXRenderUtils.popGlAllAttribBits()} - removed in core profile.</li>
 *   <li>{@code setupRenderTransform / renderModelGroup} are inherited from {@link RenderPhysicsEntity}
 *       but their bodies are stubbed; rebuild on top of PoseStack + GLTF render pipeline.</li>
 * </ul>
 *
 * @param <T> The door entity type
 */
public class RenderDoor<T extends DoorEntity<?>> extends RenderPhysicsEntity<T> {
    protected final BaseRenderContext.EntityRenderContext context = new BaseRenderContext.EntityRenderContext(this);

    public RenderDoor(EntityRendererProvider.Context context) {
        super(context);
        // TODO port:1.20.1 - was: MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(DoorEntity.class, this));
    }

    @Override
    @Nullable
    public BaseRenderContext.EntityRenderContext getRenderContext(T entity) {
        // TODO port:1.20.1 - DynamXContext.getDxModelRegistry() isn't ported yet. Was:
        //   if (entity.getPackInfo() == null) return null;
        //   DxModelRenderer modelRenderer = DynamXContext.getDxModelRegistry().getModel(entity.getPackInfo().getOwner().getModel());
        //   if (modelRenderer == null) return null;
        //   return context.setModelParams(entity, modelRenderer, entity.getEntityTextureId());
        return null;
    }

    @Override
    public void renderEntity(T entity, BaseRenderContext.EntityRenderContext context) {
        // TODO port:1.20.1 - TODO USE SCENE GRAPH (already-flagged in legacy). Was:
        //   Vector3f scale = entity.getPackInfo().getScaleModifier();
        //   GlStateManager.pushMatrix();
        //   setupRenderTransform(entity, context.getRenderPosition(), context.getPartialTicks());
        //   GlStateManager.scale(scale.x, scale.y, scale.z);
        //   renderModelGroup(context.getModel(), entity.getPackInfo().getObjectName(), entity, context.getTextureId(), false);
        //   DynamXRenderUtils.popGlAllAttribBits();
        //   GlStateManager.popMatrix();
        // Rewrite on PoseStack + GLTF renderer.
    }

    @Override
    public void renderEntityDebug(T entity, BaseRenderContext.EntityRenderContext context) {
        Object sceneGraph = entity.getPackInfo().getSceneGraph();
        if (sceneGraph instanceof SceneNode) {
            ((SceneNode<BaseRenderContext.EntityRenderContext, PartDoor>) sceneGraph).renderDebug(context, entity.getPackInfo());
        }
    }
}
