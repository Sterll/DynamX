package fr.dynamx.utils.debug.renderer;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.math.Vector3f;
import fr.dynamx.common.physics.utils.RigidBodyTransform;

import java.awt.*;

/**
 * <p>TODO port:1.20.1 - This renderer relied on:
 * <ul>
 *   <li>{@code GlStateManager.pushMatrix/translate/rotate/color/glBegin/glVertex3f/glEnd}
 *       (gone in core-profile 1.20.1).</li>
 *   <li>{@code org.lwjgl.util.glu.Sphere} / {@code GLU.GLU_LINE} (lwjgl-util removed in MC 1.17+).</li>
 *   <li>{@code DynamXRenderUtils.drawConvexHull/drawSphere/drawBoundingBox} immediate-mode helpers.</li>
 * </ul>
 * All draw methods are stubbed for now; signatures kept so callers (ClientDebugSystem) keep compiling.</p>
 */
public class PhysicsDebugRenderer {
    public static void debugSoftBody(PhysicsSoftBody physicsSoftBody) {
        // TODO port:1.20.1 - rewrite soft-body face draw with MultiBufferSource/RenderType.LINES.
    }

    public static void debugRigidBody(PhysicsRigidBody physicsRigidBody, RigidBodyTransform prevTransform, RigidBodyTransform curTransform, float partialTicks) {
        // TODO port:1.20.1 - rewrite using PoseStack + LevelRenderer.renderLineBox /
        // convex-hull lines via VertexConsumer. State-collection logic dropped (no-op).
    }

    public static void debugSphereCollisionShape(SphereCollisionShape sphereCollisionShape, int resolution, float red, float green, float blue, float alpha) {
        // TODO port:1.20.1 - lwjgl-util GLU.Sphere is gone. Replace with a custom line-sphere
        // generator and a MultiBufferSource/RenderType.LINES buffer.
    }

    public static void debugBoxCollisionShape(BoxCollisionShape boxCollisionShape, float red, float green, float blue, float alpha) {
        // TODO port:1.20.1 - LevelRenderer.renderLineBox.
    }

    public static void debugConstraint(PhysicsJoint joint, float partialTicks) {
        // TODO port:1.20.1 - constraint pivots line+sphere draw stubbed.
    }

    public static void drawSingleEndedConstraint(PhysicsRigidBody rigidBody, Vector3f pivot, Color lineColor, Color endA, Color endB, float partialTicks) {
        // TODO port:1.20.1 - stubbed.
    }

    public static void drawDoubleEndedConstraint(PhysicsRigidBody bodyA, PhysicsRigidBody bodyB, Vector3f pivotA, Vector3f pivotB, Color lineColor, float partialTicks) {
        // TODO port:1.20.1 - stubbed.
    }

    public static void drawJointLine(Vector3f pivotA, Vector3f pivotB, Color lineColor) {
        // TODO port:1.20.1 - stubbed (GL_LINE_STRIP immediate-mode gone).
    }
}
