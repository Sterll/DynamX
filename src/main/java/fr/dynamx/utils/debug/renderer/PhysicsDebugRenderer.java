package fr.dynamx.utils.debug.renderer;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.bullet.joints.Constraint;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.client.renders.RenderFrame;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;

import java.awt.Color;

public class PhysicsDebugRenderer {
    public static void debugSoftBody(PhysicsSoftBody physicsSoftBody) {
        // Soft-body face rendering needs a triangle pipeline; debug-only and stubbed for now.
    }

    public static void debugRigidBody(PhysicsRigidBody physicsRigidBody, RigidBodyTransform prevTransform, RigidBodyTransform curTransform, float partialTicks) {
        RenderFrame.Frame frame = RenderFrame.current();
        if (frame == null) return;
        PoseStack stack = frame.poseStack();

        Object userObject = physicsRigidBody.getUserObject();
        Vector3f physicsLocation = Vector3fPool.get();
        Quaternion physicsRotation = QuaternionPool.get();
        int greenColor = physicsRigidBody.getActivationState() == 2 ? 1 : 0;
        float blueColor = physicsRigidBody.getActivationState() == 2 ? 0 : 0.8f;

        stack.pushPose();
        if (userObject instanceof BulletShapeType) {
            if (curTransform != null) {
                BulletShapeType<?> shapeType = (BulletShapeType<?>) userObject;
                if (!shapeType.getType().isTerrain()) {
                    physicsLocation = curTransform.getPosition();
                    physicsRotation = curTransform.getRotation();
                    if (prevTransform != null) {
                        stack.translate(
                                prevTransform.getPosition().x + (physicsLocation.x - prevTransform.getPosition().x) * partialTicks,
                                prevTransform.getPosition().y + (physicsLocation.y - prevTransform.getPosition().y) * partialTicks,
                                prevTransform.getPosition().z + (physicsLocation.z - prevTransform.getPosition().z) * partialTicks);
                        Quaternion interp = DynamXMath.slerp(partialTicks, prevTransform.getRotation(), physicsRotation, QuaternionPool.get());
                        stack.mulPose(new org.joml.Quaternionf(interp.getX(), interp.getY(), interp.getZ(), interp.getW()));
                    } else {
                        stack.translate(physicsLocation.x, physicsLocation.y, physicsLocation.z);
                        stack.mulPose(new org.joml.Quaternionf(physicsRotation.getX(), physicsRotation.getY(), physicsRotation.getZ(), physicsRotation.getW()));
                    }
                    if (DynamXDebugOptions.RENDER_WIREFRAME.isActive()) {
                        // Convex-hull wireframe draw still requires a triangle / line pipeline rewrite.
                        // Outlined AABB of the collision shape stands in as a visible placeholder.
                        if (physicsRigidBody.getCollisionShape() instanceof BoxCollisionShape) {
                            debugBoxCollisionShape((BoxCollisionShape) physicsRigidBody.getCollisionShape(), 1f, greenColor, blueColor, 1f);
                        }
                    }
                }
            }
        } else {
            physicsRigidBody.getPhysicsLocation(physicsLocation);
            physicsRigidBody.getPhysicsRotation(physicsRotation);
            stack.translate(physicsLocation.x, physicsLocation.y, physicsLocation.z);
            stack.mulPose(new org.joml.Quaternionf(physicsRotation.getX(), physicsRotation.getY(), physicsRotation.getZ(), physicsRotation.getW()));

            CollisionShape collisionShape = physicsRigidBody.getCollisionShape();
            if (collisionShape instanceof BoxCollisionShape) {
                debugBoxCollisionShape((BoxCollisionShape) collisionShape, 1f, greenColor, blueColor, 1f);
            } else if (collisionShape instanceof SphereCollisionShape) {
                debugSphereCollisionShape((SphereCollisionShape) collisionShape, 10, 1f, greenColor, blueColor, 1f);
            } else if (collisionShape instanceof CompoundCollisionShape) {
                for (ChildCollisionShape childCollisionShape : ((CompoundCollisionShape) collisionShape).listChildren()) {
                    if (childCollisionShape.getShape() instanceof BoxCollisionShape) {
                        Vector3f off = childCollisionShape.copyOffset(Vector3fPool.get());
                        stack.pushPose();
                        stack.translate(off.x, off.y, off.z);
                        debugBoxCollisionShape((BoxCollisionShape) childCollisionShape.getShape(), 1f, greenColor, blueColor, 1f);
                        stack.popPose();
                    }
                }
            }
        }
        stack.popPose();
    }

    public static void debugSphereCollisionShape(SphereCollisionShape sphereCollisionShape, int resolution, float red, float green, float blue, float alpha) {
        RenderFrame.Frame frame = RenderFrame.current();
        if (frame == null) return;
        PoseStack pose = frame.poseStack();
        VertexConsumer consumer = frame.bufferSource().getBuffer(RenderType.lines());
        float radius = sphereCollisionShape.getRadius();

        // Latitude / longitude line sphere.
        int rings = Math.max(3, resolution);
        for (int i = 0; i <= rings; i++) {
            double lat0 = Math.PI * (-0.5 + (double) i / rings);
            double y0 = radius * Math.sin(lat0);
            double r0 = radius * Math.cos(lat0);
            for (int j = 0; j < rings; j++) {
                double lng0 = 2.0 * Math.PI * j / rings;
                double lng1 = 2.0 * Math.PI * (j + 1) / rings;
                float x1 = (float) (r0 * Math.cos(lng0));
                float z1 = (float) (r0 * Math.sin(lng0));
                float x2 = (float) (r0 * Math.cos(lng1));
                float z2 = (float) (r0 * Math.sin(lng1));
                VehicleDebugRenderer.drawLine(pose, consumer, x1, (float) y0, z1, x2, (float) y0, z2, red, green, blue, alpha);
            }
        }
        for (int j = 0; j < rings; j++) {
            double lng = 2.0 * Math.PI * j / rings;
            float cx = (float) Math.cos(lng);
            float cz = (float) Math.sin(lng);
            for (int i = 0; i < rings; i++) {
                double lat0 = Math.PI * (-0.5 + (double) i / rings);
                double lat1 = Math.PI * (-0.5 + (double) (i + 1) / rings);
                float y1 = (float) (radius * Math.sin(lat0));
                float r1 = (float) (radius * Math.cos(lat0));
                float y2 = (float) (radius * Math.sin(lat1));
                float r2 = (float) (radius * Math.cos(lat1));
                VehicleDebugRenderer.drawLine(pose, consumer,
                        cx * r1, y1, cz * r1,
                        cx * r2, y2, cz * r2,
                        red, green, blue, alpha);
            }
        }
    }

    public static void debugBoxCollisionShape(BoxCollisionShape boxCollisionShape, float red, float green, float blue, float alpha) {
        RenderFrame.Frame frame = RenderFrame.current();
        if (frame == null) return;
        Vector3f halfExtent = Vector3fPool.get();
        boxCollisionShape.getHalfExtents(halfExtent);
        VertexConsumer consumer = frame.bufferSource().getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(frame.poseStack(), consumer,
                -halfExtent.x, -halfExtent.y, -halfExtent.z,
                halfExtent.x, halfExtent.y, halfExtent.z,
                red, green, blue, alpha);
    }

    public static void debugConstraint(PhysicsJoint joint, float partialTicks) {
        if (!(joint instanceof Constraint)) return;
        Constraint constraint = (Constraint) joint;
        Vector3f pivotA = Vector3fPool.get();
        Vector3f pivotB = Vector3fPool.get();
        if (constraint.getBodyA() != null) {
            constraint.getPivotA(pivotA);
            drawSingleEndedConstraint(constraint.getBodyA(), pivotA, new Color(255, 0, 0, 255), new Color(255, 0, 0, 128), new Color(255, 0, 0, 255), partialTicks);
        }
        if (constraint.getBodyB() != null) {
            constraint.getPivotB(pivotB);
            drawSingleEndedConstraint(constraint.getBodyB(), pivotB, new Color(0, 255, 0, 255), new Color(255, 255, 0, 128), new Color(0, 255, 0, 255), partialTicks);
        }
        if (constraint.getBodyA() != null && constraint.getBodyB() != null) {
            constraint.getPivotA(pivotA);
            constraint.getPivotB(pivotB);
            drawDoubleEndedConstraint(constraint.getBodyA(), constraint.getBodyB(), pivotA, pivotB, new Color(128, 0, 128, 255), partialTicks);
        }
    }

    public static void drawSingleEndedConstraint(PhysicsRigidBody rigidBody, Vector3f pivot, Color lineColor, Color endA, Color endB, float partialTicks) {
        Vector3f bodyPos = ClientDebugSystem.getInterpolatedTranslation(rigidBody, partialTicks);
        Quaternion bodyRot = ClientDebugSystem.getInterpolatedRotation(rigidBody, partialTicks);
        Vector3f rotatedPosA = DynamXGeometry.rotateVectorByQuaternion(pivot, bodyRot);
        Vector3f translatedPosA = rotatedPosA.add(bodyPos);
        drawJointLine(bodyPos, translatedPosA, lineColor);
    }

    public static void drawDoubleEndedConstraint(PhysicsRigidBody bodyA, PhysicsRigidBody bodyB, Vector3f pivotA, Vector3f pivotB, Color lineColor, float partialTicks) {
        Vector3f posA = ClientDebugSystem.getInterpolatedTranslation(bodyA, partialTicks);
        Quaternion rotA = ClientDebugSystem.getInterpolatedRotation(bodyA, partialTicks);
        Vector3f posB = ClientDebugSystem.getInterpolatedTranslation(bodyB, partialTicks);
        Quaternion rotB = ClientDebugSystem.getInterpolatedRotation(bodyB, partialTicks);
        Vector3f translatedPosA = DynamXGeometry.rotateVectorByQuaternion(pivotA, rotA).add(posA);
        Vector3f translatedPosB = DynamXGeometry.rotateVectorByQuaternion(pivotB, rotB).add(posB);
        drawJointLine(translatedPosA, translatedPosB, lineColor);
    }

    public static void drawJointLine(Vector3f pivotA, Vector3f pivotB, Color lineColor) {
        RenderFrame.Frame frame = RenderFrame.current();
        if (frame == null) return;
        VertexConsumer consumer = frame.bufferSource().getBuffer(RenderType.lines());
        VehicleDebugRenderer.drawLine(frame.poseStack(), consumer,
                pivotA.x, pivotA.y, pivotA.z,
                pivotB.x, pivotB.y, pivotB.z,
                lineColor.getRed() / 255f, lineColor.getGreen() / 255f, lineColor.getBlue() / 255f, lineColor.getAlpha() / 255f);
    }
}
