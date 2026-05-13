package fr.dynamx.client.renders.vehicle;

import fr.dynamx.api.events.client.DynamXEntityRenderEvent;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.IRenderContext;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.utils.debug.renderer.BoatDebugRenderer;
import fr.dynamx.utils.debug.renderer.VehicleDebugRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;

/**
 * Renderer for {@link BaseVehicleEntity} (cars, trailers, boats, helicopters).
 *
 * <p>TODO port:1.20.1 -
 * <ul>
 *   <li>Constructor switched from {@code RenderManager} to {@link EntityRendererProvider.Context}.</li>
 *   <li>{@code MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(...))} -&gt;
 *       {@code MinecraftForge.EVENT_BUS.post(...)} ; commented out because the event class isn't ported.</li>
 *   <li>{@code DynamXContext.getDxModelRegistry().getModel(...)} - DynamXContext isn't ported yet
 *       (Phase 5/8); {@link #getRenderContext} returns null until then.</li>
 *   <li>{@code WheelsModule#spawnPropulsionParticles(...)} - WheelsModule lives in Phase 6.</li>
 * </ul>
 *
 * @param <T> The vehicle entity type
 */
public class RenderBaseVehicle<T extends BaseVehicleEntity<?>> extends RenderPhysicsEntity<T> {
    protected final BaseRenderContext.EntityRenderContext context = new BaseRenderContext.EntityRenderContext(this);

    public RenderBaseVehicle(EntityRendererProvider.Context context) {
        super(context);
        addDebugRenderers(new BoatDebugRenderer.FloatsDebug());
        // TODO port:1.20.1 - was: MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(BaseVehicleEntity.class, this));
    }

    @Override
    public void spawnParticles(T carEntity, BaseRenderContext.EntityRenderContext context) {
        super.spawnParticles(carEntity, context);
        DynamXEntityRenderEvent particlesEvent = new DynamXEntityRenderEvent(carEntity, context, DynamXEntityRenderEvent.Type.PARTICLES, 0);
        MinecraftForge.EVENT_BUS.post(particlesEvent);
        // TODO port:1.20.1 - WheelsModule isn't ported yet (Phase 6). Was:
        //   if (carEntity.hasModuleOfType(WheelsModule.class)) {
        //       carEntity.getModuleByType(WheelsModule.class).spawnPropulsionParticles(this, context.getPartialTicks());
        //   }
    }

    @Override
    @Nullable
    public BaseRenderContext.EntityRenderContext getRenderContext(T entity) {
        if (entity.getPackInfo() == null) return null;
        Object raw = fr.dynamx.common.DynamXContext.getDxModelRegistry().getModel(entity.getPackInfo().getModel());
        if (!(raw instanceof fr.dynamx.client.renders.model.renderer.DxModelRenderer modelRenderer)) return null;
        if (modelRenderer.isEmpty()) return null;
        return context.setModelParams(entity, modelRenderer, entity.getEntityTextureId());
    }

    @Override
    public void renderEntity(T entity, BaseRenderContext.EntityRenderContext context) {
        Object sceneGraph = entity.getPackInfo().getSceneGraph();
        if (sceneGraph instanceof SceneNode) {
            ((SceneNode<BaseRenderContext.EntityRenderContext, ModularVehicleInfo>) sceneGraph).render(context, entity.getPackInfo(), null);
        }
    }

    @Override
    public void renderEntityDebug(T entity, BaseRenderContext.EntityRenderContext context) {
        Object sceneGraph = entity.getPackInfo().getSceneGraph();
        if (sceneGraph instanceof SceneNode) {
            ((SceneNode<BaseRenderContext.EntityRenderContext, ModularVehicleInfo>) sceneGraph).renderDebug(context, entity.getPackInfo());
        }
    }

    /**
     * Renders the entity with the given texture id (used for item/GUI rendering).
     *
     * <p>TODO port:1.20.1 - DxModelRegistry isn't ported, no-op until then.
     */
    public void renderEntity(ModularVehicleInfo packInfo, byte textureId) {
        // TODO port:1.20.1 - was:
        //   DxModelRenderer modelRenderer = DynamXContext.getDxModelRegistry().getModel(packInfo.getModel());
        //   if (modelRenderer == null) return;
        //   ((SceneNode<IRenderContext, ModularVehicleInfo>) packInfo.getSceneGraph())
        //       .render(context.setRenderParams(0,0,0,1,true).setModelParams(modelRenderer, textureId), packInfo, null);
        Object sceneGraph = packInfo.getSceneGraph();
        if (sceneGraph instanceof SceneNode) {
            BaseRenderContext.EntityRenderContext entCtx = context.setRenderParams(0, 0, 0, 1, true);
            entCtx.setModelParams(null, null, textureId);
            ((SceneNode<IRenderContext, ModularVehicleInfo>) sceneGraph).render(entCtx, packInfo, null);
        }
    }

    /**
     * Renders a car entity with wheel debug overlays.
     *
     * @param <T> The car entity type
     */
    public static class RenderCar<T extends fr.dynamx.common.entities.vehicles.CarEntity<?>> extends RenderBaseVehicle<T> {
        public RenderCar(EntityRendererProvider.Context manager) {
            super(manager);
            VehicleDebugRenderer.addAll(this, true);
        }
    }

    /**
     * Renders a trailer entity.
     *
     * @param <T> The trailer entity type
     */
    public static class RenderTrailer<T extends fr.dynamx.common.entities.vehicles.TrailerEntity<?>> extends RenderBaseVehicle<T> {
        public RenderTrailer(EntityRendererProvider.Context manager) {
            super(manager);
            VehicleDebugRenderer.addAll(this, false);
        }
    }

    /**
     * Renders a boat entity with floats debug.
     *
     * @param <T> The boat entity type
     */
    public static class RenderBoat<T extends fr.dynamx.common.entities.vehicles.BoatEntity<?>> extends RenderBaseVehicle<T> {
        public RenderBoat(EntityRendererProvider.Context manager) {
            super(manager);
            BoatDebugRenderer.addAll(this);
        }
    }

    /**
     * Renders a helicopter entity.
     *
     * @param <T> The helicopter entity type
     */
    public static class RenderHelicopter<T extends fr.dynamx.common.entities.vehicles.HelicopterEntity<?>> extends RenderBaseVehicle<T> {
        public RenderHelicopter(EntityRendererProvider.Context manager) {
            super(manager);
            VehicleDebugRenderer.addAll(this, true);
        }
    }
}
