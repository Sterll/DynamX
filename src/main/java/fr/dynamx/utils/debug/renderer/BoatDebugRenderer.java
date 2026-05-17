package fr.dynamx.utils.debug.renderer;

import com.jme3.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.dynamx.client.renders.RenderFrame;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.entities.PackEntityPhysicsHandler;
import fr.dynamx.common.physics.entities.modules.FloatPhysicsHandler;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;

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
            RenderFrame.Frame frame = RenderFrame.current();
            if (frame == null) return;
            PoseStack pose = frame.poseStack();
            VertexConsumer consumer = frame.bufferSource().getBuffer(RenderType.lines());

            PackEntityPhysicsHandler<?, ?> physicsHandler = (PackEntityPhysicsHandler<?, ?>) entity.physicsHandler;
            if (physicsHandler == null) return;

            int i = 0;
            for (FloatPhysicsHandler f : physicsHandler.getFloatList()) {
                Vector3f floater = f.getPosition();
                float halfSize = f.getSize() / 2f;
                float halfY = f.getScale().y / 2f;
                LevelRenderer.renderLineBox(pose, consumer,
                        floater.x - halfSize, floater.y - halfY, floater.z - halfSize,
                        floater.x + halfSize, floater.y + halfY, floater.z + halfSize,
                        0f, 1f, 0f, 1f);
                if (physicsHandler.getDebugBuoyForces() != null && i < physicsHandler.getDebugBuoyForces().size()) {
                    drawForce(pose, consumer, floater, physicsHandler.getDebugBuoyForces().get(i), 1f, 0f, 0f);
                }
                if (physicsHandler.getDebugDragForces() != null && i < physicsHandler.getDebugDragForces().size()) {
                    drawForce(pose, consumer, floater, physicsHandler.getDebugDragForces().get(i), 1f, 1f, 0f);
                }
                i++;
            }
        }

        private void drawForce(PoseStack pose, VertexConsumer consumer, Vector3f pos, Vector3f force,
                               float red, float green, float blue) {
            VehicleDebugRenderer.drawLine(pose, consumer,
                    pos.x, pos.y, pos.z,
                    pos.x + force.x, pos.y + force.y, pos.z + force.z,
                    red, green, blue, 1f);
        }
    }
}
