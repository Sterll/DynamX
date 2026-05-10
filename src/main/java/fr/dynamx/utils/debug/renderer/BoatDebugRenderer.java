package fr.dynamx.utils.debug.renderer;

import com.jme3.math.Vector3f;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.entities.PackEntityPhysicsHandler;
import fr.dynamx.common.physics.entities.modules.FloatPhysicsHandler;
import fr.dynamx.utils.debug.DynamXDebugOptions;

/**
 * <p>TODO port:1.20.1 - All immediate-mode draws (GlStateManager push/pop/color/disableTexture +
 * Tessellator/BufferBuilder with GL_LINE_STRIP/POSITION_COLOR) must move to a
 * MultiBufferSource + RenderType.LINES based pipeline.</p>
 */
public class BoatDebugRenderer {
    public static <T extends PhysicsEntity<?>> void addAll(RenderPhysicsEntity<T> to) {
        to.addDebugRenderers(new FloatsDebug(), new DebugRenderer.StoragesDebug(), new VehicleDebugRenderer.PlayerCollisionsDebug(), new VehicleDebugRenderer.NetworkDebug());
    }

    public static class FloatsDebug implements DebugRenderer<PhysicsEntity<?>> {
        @Override
        public boolean shouldRender(PhysicsEntity<?> entity) {
            return DynamXDebugOptions.WHEELS.isActive();
        }

        @Override
        public void render(PhysicsEntity<?> entity, RenderPhysicsEntity<PhysicsEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            // TODO port:1.20.1 - rewrite without GlStateManager/Tessellator/BufferBuilder (core-profile).
            PackEntityPhysicsHandler<?, ?> physicsHandler = (PackEntityPhysicsHandler<?, ?>) entity.physicsHandler;
            int i = 0;
            for (FloatPhysicsHandler f : physicsHandler.getFloatList()) {
                Vector3f floater = f.getPosition();
                // immediate-mode bounding-box draw stubbed: green (0,1,0,1)
                //   from floater - (size/2, scale.y/2, size/2)
                //   to   floater + (size/2, scale.y/2, size/2)
                // drawForce(floater, getDebugBuoyForces()[i], red)
                // drawForce(floater, getDebugDragForces()[i], yellow)
                physicsHandler.getDebugBuoyForces().get(i);
                physicsHandler.getDebugDragForces().get(i);
                i++;
            }
        }

        @SuppressWarnings("unused")
        private void drawForce(Vector3f pos, Vector3f force, float red, float green, float blue) {
            // TODO port:1.20.1 - draw line from pos to (pos + force) with color (red, green, blue, 1)
        }
    }
}
