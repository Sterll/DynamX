package fr.dynamx.utils.debug.renderer;

import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.IRotatedCollisionHandler;
import fr.dynamx.client.network.ClientPhysicsEntitySynchronizer;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.contentpack.parts.PartPropsContainer;
import fr.dynamx.common.contentpack.type.vehicle.TrailerAttachInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Contains all {@link DebugRenderer}s for vehicles.
 *
 * <p>TODO port:1.20.1 - All draws used {@code RenderGlobal.drawBoundingBox},
 * {@code RenderGlobal.drawSelectionBoundingBox}, {@code GlStateManager.push/pop/translate},
 * {@code GlStateManager.glBegin/glVertex3f/glEnd}. These are all gone in 1.20.1 core-profile.
 * Rewrite using {@code LevelRenderer.renderLineBox(PoseStack, VertexConsumer, ...)} with a
 * {@code RenderType.LINES} buffer. All draw bodies are stubbed but state-collection logic remains.</p>
 *
 * @see BoatDebugRenderer
 */
public class VehicleDebugRenderer {
    public static <T extends PhysicsEntity<?>> void addAll(RenderPhysicsEntity<T> to, boolean hasSeats) {
        to.addDebugRenderers(
                new FrictionPointsDebug(),
                new TrailerPointsDebug(),
                new PlayerCollisionsDebug(),
                new NetworkDebug(),
                new PropsContainerDebug());
        if (hasSeats)
            to.addDebugRenderers(new DebugRenderer.StoragesDebug());
    }

    public static class FrictionPointsDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.FRICTION_POINTS.isActive();
        }

        @Override
        public boolean hasEntityRotation(BaseVehicleEntity<?> entity) {
            return false;
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, RenderPhysicsEntity<BaseVehicleEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            // TODO port:1.20.1 - draws stubbed (GL_LINES and bounding-box immediate-mode gone).
        }
    }

    public static class TrailerPointsDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.TRAILER_ATTACH_POINTS.isActive() && entity.getPackInfo().getSubPropertyByType(TrailerAttachInfo.class) != null;
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, RenderPhysicsEntity<BaseVehicleEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            // TODO port:1.20.1 - replace RenderGlobal.drawBoundingBox with LevelRenderer.renderLineBox.
            Vector3f p1 = entity.getPackInfo().getSubPropertyByType(TrailerAttachInfo.class).getAttachPoint();
            // stub draw at p1 (size 0.05f) magenta (0.5,0,1,1)
            //noinspection ResultOfMethodCallIgnored
            p1.hashCode();
        }
    }

    /**
     * Player collision debug : shows un-rotated and rotated boxes of player and vehicle
     *
     * @see IRotatedCollisionHandler
     */
    public static class PlayerCollisionsDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        public static AABB lastTemp;
        public static Vec3 pos;
        public static Vector3f motion;
        public static Vector3f rotatedmotion;
        public static Vector3f realmotionrot;
        public static Vector3f realmotion;

        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.PLAYER_COLLISIONS.isActive();
        }

        @Override
        public boolean hasEntityRotation(BaseVehicleEntity<?> entity) {
            return false;
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, RenderPhysicsEntity<BaseVehicleEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            // TODO port:1.20.1 - all RenderGlobal.drawBoundingBox/drawSelectionBoundingBox draws stubbed.
            // Keep iteration logic so collision-box state still gets touched if needed.
            try {
                for (MutableBoundingBox bb : entity.getCollisionBoxes()) {
                    //noinspection ResultOfMethodCallIgnored
                    bb.hashCode();
                }
            } catch (java.util.ConcurrentModificationException e) {
                e.printStackTrace();
            }
        }
    }

    public static class PropsContainerDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.PROPS_CONTAINERS.isActive();
        }

        @Override
        public boolean hasEntityRotation(BaseVehicleEntity<?> entity) {
            return false;
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, RenderPhysicsEntity<BaseVehicleEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            // TODO port:1.20.1 - DynamXRenderUtils.drawBoundingBox immediate-mode gone; stub.
            List<PartPropsContainer> containers = entity.getPackInfo().getPartsByType(PartPropsContainer.class);
            //noinspection ResultOfMethodCallIgnored
            containers.size();
        }
    }

    /**
     * Network debug : render previous entity states
     */
    public static class NetworkDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.FULL_NETWORK_DEBUG.isActive() && entity.getSynchronizer() instanceof ClientPhysicsEntitySynchronizer;
        }

        @Override
        public boolean hasEntityRotation(BaseVehicleEntity<?> entity) {
            return false;
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, RenderPhysicsEntity<BaseVehicleEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            // TODO port:1.20.1 - second-pass entity render at the server position needs the new
            // BaseRenderContext + PoseStack pipeline. Stubbed for now.
        }
    }
}
