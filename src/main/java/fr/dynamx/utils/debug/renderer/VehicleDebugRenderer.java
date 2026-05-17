package fr.dynamx.utils.debug.renderer;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.physics.IRotatedCollisionHandler;
import fr.dynamx.client.network.ClientPhysicsEntitySynchronizer;
import fr.dynamx.client.renders.RenderFrame;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.PartPropsContainer;
import fr.dynamx.common.contentpack.type.vehicle.FrictionPoint;
import fr.dynamx.common.contentpack.type.vehicle.TrailerAttachInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.BoundingBoxPool;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.SubClassPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ConcurrentModificationException;
import java.util.List;

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

    static void drawLine(PoseStack pose, VertexConsumer consumer,
                         float x1, float y1, float z1, float x2, float y2, float z2,
                         float r, float g, float b, float a) {
        org.joml.Matrix4f mat = pose.last().pose();
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-4f) len = 1.0e-4f;
        float nx = dx / len, ny = dy / len, nz = dz / len;
        consumer.vertex(mat, x1, y1, z1).color(r, g, b, a).normal(pose.last().normal(), nx, ny, nz).endVertex();
        consumer.vertex(mat, x2, y2, z2).color(r, g, b, a).normal(pose.last().normal(), nx, ny, nz).endVertex();
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
            if (entity.getPackInfo().getFrictionPoints().isEmpty()) return;
            RenderFrame.Frame frame = RenderFrame.current();
            if (frame == null) return;
            PoseStack pose = frame.poseStack();
            VertexConsumer consumer = frame.bufferSource().getBuffer(RenderType.lines());

            Vec3 mv = entity.getDeltaMovement();
            float horizSpeed = Vector3fPool.get((float) mv.x, 0, (float) mv.z).length();
            for (FrictionPoint f : entity.getPackInfo().getFrictionPoints()) {
                Vector3f pushDown = new Vector3f((float) -mv.x, -horizSpeed, (float) -mv.z);
                pushDown.multLocal(f.getIntensity());
                Vector3f pos = f.getPosition();
                pos = DynamXGeometry.rotateVectorByQuaternion(pos, entity.renderRotation);

                LevelRenderer.renderLineBox(pose, consumer,
                        pos.x - 0.04f, pos.y - 0.04f, pos.z - 0.04f,
                        pos.x + 0.04f, pos.y + 0.04f, pos.z + 0.04f,
                        0f, 1f, 0f, 1f);
                drawLine(pose, consumer, pos.x, pos.y, pos.z,
                        pos.x + pushDown.x, pos.y + pushDown.y, pos.z + pushDown.z,
                        1f, 0f, 0f, 1f);
            }
        }
    }

    public static class TrailerPointsDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.TRAILER_ATTACH_POINTS.isActive() && entity.getPackInfo().getSubPropertyByType(TrailerAttachInfo.class) != null;
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, RenderPhysicsEntity<BaseVehicleEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            RenderFrame.Frame frame = RenderFrame.current();
            if (frame == null) return;
            VertexConsumer consumer = frame.bufferSource().getBuffer(RenderType.lines());
            Vector3f p1 = entity.getPackInfo().getSubPropertyByType(TrailerAttachInfo.class).getAttachPoint();
            LevelRenderer.renderLineBox(frame.poseStack(), consumer,
                    p1.x, p1.y - 0.05f, p1.z - 0.05f,
                    p1.x + 0.05f, p1.y + 0.05f, p1.z + 0.05f,
                    0.5f, 0f, 1f, 1f);
        }
    }

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
            RenderFrame.Frame frame = RenderFrame.current();
            if (frame == null) return;
            PoseStack stack = frame.poseStack();
            VertexConsumer consumer = frame.bufferSource().getBuffer(RenderType.lines());

            stack.pushPose();
            stack.translate(-entity.getX(), -entity.getY(), -entity.getZ());
            try {
                for (MutableBoundingBox bb : entity.getCollisionBoxes()) {
                    LevelRenderer.renderLineBox(stack, consumer,
                            bb.minX, bb.minY, bb.minZ,
                            bb.maxX, bb.maxY, bb.maxZ,
                            1f, 1f, 0f, 1f);
                }
            } catch (ConcurrentModificationException e) {
                e.printStackTrace();
            }

            if (lastTemp != null) {
                LevelRenderer.renderLineBox(stack, consumer, lastTemp, 0f, 1f, 1f, 1f);
            }
            if (motion != null && pos != null) {
                drawLine(stack, consumer, (float) pos.x, (float) pos.y, (float) pos.z,
                        motion.x * 10f + (float) pos.x, motion.y * 10f + (float) pos.y, motion.z * 10f + (float) pos.z,
                        1f, 0f, 0f, 1f);
            }
            if (rotatedmotion != null && pos != null) {
                drawLine(stack, consumer, (float) pos.x, (float) pos.y, (float) pos.z,
                        rotatedmotion.x * 10f + (float) pos.x, rotatedmotion.y * 10f + (float) pos.y, rotatedmotion.z * 10f + (float) pos.z,
                        0f, 1f, 0f, 1f);
            }
            if (realmotionrot != null && pos != null) {
                drawLine(stack, consumer, (float) pos.x, (float) pos.y, (float) pos.z,
                        realmotionrot.x * 10f + (float) pos.x, realmotionrot.y * 10f + (float) pos.y, realmotionrot.z * 10f + (float) pos.z,
                        1f, 0f, 1f, 1f);
            }
            if (realmotion != null && pos != null) {
                drawLine(stack, consumer, (float) pos.x, (float) pos.y, (float) pos.z,
                        realmotion.x * 10f + (float) pos.x, realmotion.y * 10f + (float) pos.y, realmotion.z * 10f + (float) pos.z,
                        0f, 0f, 1f, 1f);
            }

            BoundingBoxPool.getPool().openSubPool(SubClassPool.BOUNDING_BOX_DEFAULT);
            DynamXContext.getPlayerToCollision().forEach((player, playerPhysicsHandler) -> {
                if (playerPhysicsHandler.getBodyIn() != null) {
                    BoundingBox bb = playerPhysicsHandler.getBodyIn().boundingBox(BoundingBoxPool.get());
                    Vector3f min = bb.getMin(Vector3fPool.get());
                    Vector3f max = bb.getMax(Vector3fPool.get());
                    LevelRenderer.renderLineBox(stack, consumer,
                            min.x, min.y, min.z, max.x, max.y, max.z,
                            0.2f, 0.5f, 0.7f, 1f);
                }
            });
            BoundingBoxPool.getPool().closeSubPool();
            stack.popPose();
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
            List<PartPropsContainer> containers = entity.getPackInfo().getPartsByType(PartPropsContainer.class);
            if (containers.isEmpty()) return;
            RenderFrame.Frame frame = RenderFrame.current();
            if (frame == null) return;
            PoseStack stack = frame.poseStack();
            VertexConsumer consumer = frame.bufferSource().getBuffer(RenderType.lines());

            for (PartPropsContainer container : containers) {
                stack.pushPose();
                stack.translate(-entity.getX(), -entity.getY(), -entity.getZ());

                Vector3f cpos = DynamXGeometry.rotateVectorByQuaternion(container.getPosition(), entity.physicsRotation);
                MutableBoundingBox rotatedSize = DynamXContext.getCollisionHandler().rotateBB(Vector3fPool.get(0, 0, 0), container.getBoundingBox(), entity.physicsRotation);
                rotatedSize = rotatedSize.offset(cpos);
                rotatedSize = rotatedSize.offset(entity.physicsPosition);
                LevelRenderer.renderLineBox(stack, consumer,
                        rotatedSize.minX, rotatedSize.minY, rotatedSize.minZ,
                        rotatedSize.maxX, rotatedSize.maxY, rotatedSize.maxZ,
                        1f, 0f, 0f, 1f);

                stack.popPose();
            }
        }
    }

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
            Vector3f epos = entity.physicsPosition;
            Vector3f serverPos = ((ClientPhysicsEntitySynchronizer) entity.getSynchronizer()).getServerPos();
            if (serverPos == null) return;
            RenderFrame.Frame frame = RenderFrame.current();
            if (frame == null) return;
            VertexConsumer consumer = frame.bufferSource().getBuffer(RenderType.lines());

            double dx = -epos.x + serverPos.x;
            double dy = -epos.y + serverPos.y;
            double dz = -epos.z + serverPos.z;
            boolean driver = entity.getSynchronizer().getSimulationHolder() == SimulationHolder.DRIVER;
            AABB box = entity.getBoundingBox()
                    .move(dx, dy, dz)
                    .move(-entity.getX(), -entity.getY(), -entity.getZ());
            LevelRenderer.renderLineBox(frame.poseStack(), consumer, box,
                    driver ? 0.9f : 0.1f, 0.1f, 0.8f, 1f);
        }
    }
}
