package fr.dynamx.utils.client;

import com.jme3.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.maths.DynamXGeometry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;

/**
 * Provides some useful methods for rendering dynamx objects/debug.
 *
 * <p>TODO port:1.20.1 - This file is heavily stubbed. The original used:
 * <ul>
 *   <li>{@code GlStateManager.translate/rotate/scale/color/glBegin/glVertex3f/glNormal3f/glEnd/depthMask/disableLighting/...} -
 *       all gone in core-profile 1.20.1.</li>
 *   <li>{@code RenderGlobal.drawBoundingBox} - use {@code LevelRenderer.renderLineBox(PoseStack, VertexConsumer, ...)}.</li>
 *   <li>{@code Tessellator.getInstance().getBuffer().begin(...)} immediate-mode - rewrite as
 *       {@code MultiBufferSource.getBuffer(RenderType.LINES)} or similar.</li>
 *   <li>{@code FontRenderer.drawString} -> {@code GuiGraphics.drawString(font, text, x, y, color)}.</li>
 *   <li>{@code APPLEVertexArrayObject} - the Mac-specific VAO path is gone; just use {@code GL30}.</li>
 *   <li>{@code Minecraft.IS_RUNNING_ON_MAC} - gone, use {@code DistPlatform} / {@code Util.getPlatform()}.</li>
 *   <li>{@code GL11.glPushAttrib(GL_ALL_ATTRIB_BITS)} - removed in OpenGL 3.2+ core profile; the
 *       push/pop attrib pair must be replaced by an explicit RenderSystem save/restore.</li>
 *   <li>{@code World.spawnParticle} -> {@code Level.addParticle}.</li>
 * </ul>
 * Public method signatures are kept; bodies are stubbed.</p>
 *
 * @see ClientDynamXUtils
 */
public class DynamXRenderUtils {
    private static boolean glAllAttribBitsEnabled;

    // TODO port:1.20.1 - GridGLMesh/ArrowMesh/OctasphereMesh ported by client/renders agent (Phase 7).
    // Until those classes land in main, the static mesh fields are typed as Object to keep this file compiling.
    public static Object gridMesh;
    public static Object arrowMeshX;
    public static Object arrowMeshY;
    public static Object arrowMeshZ;
    public static Object sphereMesh;

    public static void initGlMeshes() {
        // TODO port:1.20.1 - rewire to ported Grid/Arrow/Octasphere meshes once Phase 7 lands.
    }

    public static void drawBoundingBox(PoseStack poseStack, MultiBufferSource bufferSource,
                                       Vector3f halfExtent, float red, float green, float blue, float alpha) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, consumer,
                -halfExtent.x, -halfExtent.y, -halfExtent.z,
                halfExtent.x, halfExtent.y, halfExtent.z,
                red, green, blue, alpha);
    }

    public static void drawBoundingBox(PoseStack poseStack, MultiBufferSource bufferSource,
                                       Vector3f min, Vector3f max, float red, float green, float blue, float alpha) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, consumer,
                min.x, min.y, min.z,
                max.x, max.y, max.z,
                red, green, blue, alpha);
    }

    public static void drawBoundingBox(PoseStack poseStack, MultiBufferSource bufferSource,
                                       AABB aabb, float red, float green, float blue, float alpha) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, consumer, aabb, red, green, blue, alpha);
    }

    /**
     * @deprecated You should render the entity using its SceneGraph (see Phase 7 port).
     */
    @Deprecated
    public static void renderCar(ModularVehicleInfo car, byte textureId) {
        // TODO port:1.20.1 - depends on RenderBaseVehicle (ported by client/renders agent).
    }

    public static void drawSphere(Vector3f translation, float radius, @Nullable Color sphereColor) {
        // Conservee pour compatibilite API : en core profile 1.20.1, l'etat GL global a disparu.
        // Utiliser la surcharge prenant PoseStack + MultiBufferSource pour un rendu effectif.
    }

    /**
     * Trace une approximation de sphere en wireframe (trois cercles orthogonaux XY/XZ/YZ).
     * Suffisant pour du debug, sans dependance a un mesh Octasphere dedie.
     */
    public static void drawSphere(PoseStack poseStack, MultiBufferSource bufferSource,
                                  Vector3f translation, float radius,
                                  float red, float green, float blue, float alpha) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        poseStack.pushPose();
        poseStack.translate(translation.x, translation.y, translation.z);
        var pose = poseStack.last();
        org.joml.Matrix4f m = pose.pose();
        org.joml.Matrix3f n = pose.normal();
        final int segments = 24;
        for (int plane = 0; plane < 3; plane++) {
            float prevA = radius;
            float prevB = 0f;
            for (int i = 1; i <= segments; i++) {
                float t = (float) (i * 2.0 * Math.PI / segments);
                float a = (float) Math.cos(t) * radius;
                float b = (float) Math.sin(t) * radius;
                float x1, y1, z1, x2, y2, z2;
                switch (plane) {
                    case 0 -> { x1 = prevA; y1 = prevB; z1 = 0; x2 = a; y2 = b; z2 = 0; }
                    case 1 -> { x1 = prevA; y1 = 0; z1 = prevB; x2 = a; y2 = 0; z2 = b; }
                    default -> { x1 = 0; y1 = prevA; z1 = prevB; x2 = 0; y2 = a; z2 = b; }
                }
                consumer.vertex(m, x1, y1, z1).color(red, green, blue, alpha).normal(n, 0, 1, 0).endVertex();
                consumer.vertex(m, x2, y2, z2).color(red, green, blue, alpha).normal(n, 0, 1, 0).endVertex();
                prevA = a;
                prevB = b;
            }
        }
        poseStack.popPose();
    }

    public static void glTranslate(Vector3f translation) {
        // Conservee pour compatibilite API : GlStateManager.translate a disparu en core profile.
        // Translater via PoseStack#translate au point d'appel a la place.
    }

    public static void drawConvexHull(List<Vector3f> vectorBuffer, boolean wireframe) {
        // Conservee pour compatibilite API : le pipeline immediate mode est mort. Utiliser
        // la surcharge prenant PoseStack + MultiBufferSource.
    }

    /**
     * Emet une enveloppe convexe en wireframe : chaque triplet de vertices forme un triangle
     * dont les trois aretes sont emises via {@link RenderType#lines()}.
     */
    public static void drawConvexHull(PoseStack poseStack, MultiBufferSource bufferSource,
                                      List<Vector3f> vectorBuffer,
                                      float red, float green, float blue, float alpha) {
        if (vectorBuffer == null || vectorBuffer.size() < 3) {
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        var pose = poseStack.last();
        org.joml.Matrix4f m = pose.pose();
        org.joml.Matrix3f nm = pose.normal();
        int triCount = vectorBuffer.size() / 3;
        for (int t = 0; t < triCount; t++) {
            Vector3f a = vectorBuffer.get(t * 3);
            Vector3f b = vectorBuffer.get(t * 3 + 1);
            Vector3f c = vectorBuffer.get(t * 3 + 2);
            emitLine(consumer, m, nm, a, b, red, green, blue, alpha);
            emitLine(consumer, m, nm, b, c, red, green, blue, alpha);
            emitLine(consumer, m, nm, c, a, red, green, blue, alpha);
        }
    }

    private static void emitLine(VertexConsumer consumer, org.joml.Matrix4f m, org.joml.Matrix3f n,
                                 Vector3f a, Vector3f b,
                                 float red, float green, float blue, float alpha) {
        consumer.vertex(m, a.x, a.y, a.z).color(red, green, blue, alpha).normal(n, 0, 1, 0).endVertex();
        consumer.vertex(m, b.x, b.y, b.z).color(red, green, blue, alpha).normal(n, 0, 1, 0).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    public static void drawNameplate(Font fontRendererIn, String str, float x, float y, float z, PhysicsEntity<?> entity, int verticalShift, float viewerYaw, float viewerPitch, boolean isThirdPersonFrontal) {
        // TODO port:1.20.1 - use EntityRenderer.renderNameTag(...) or GuiGraphics-based pipeline.
    }

    @OnlyIn(Dist.CLIENT)
    public static void drawNameplate(Font fontRendererIn, String str, float x, float y, float z, org.joml.Quaternionf rotation, int verticalShift, float viewerYaw, float viewerPitch, boolean isThirdPersonFrontal) {
        // TODO port:1.20.1 - same as above.
    }

    public static void spawnParticles(ParticleEmitterInfo.IParticleEmitterContainer particleEmitterInfo, Level world, Vector3f initialPos, Vector3f initialRot) {
        particleEmitterInfo.getParticleEmitters()
                .forEach(emitterInfo -> {
                    Vector3f rotatedPoint = DynamXGeometry.getRotatedPoint(emitterInfo.position, initialRot.x, initialRot.y, initialRot.z);
                    world.addParticle(emitterInfo.particleType,
                            initialPos.x + rotatedPoint.x,
                            initialPos.y + rotatedPoint.y,
                            initialPos.z + rotatedPoint.z,
                            emitterInfo.velocity.x,
                            emitterInfo.velocity.y,
                            emitterInfo.velocity.z);
                });
    }

    public static void spawnParticles(ParticleEmitterInfo.IParticleEmitterContainer particleEmitterInfo, Level world, Vector3f initialPos, com.jme3.math.Quaternion initialRot) {
        particleEmitterInfo.getParticleEmitters()
                .forEach(emitterInfo -> {
                    Vector3f rotatedPoint = DynamXGeometry.rotateVectorByQuaternion(emitterInfo.position, initialRot);
                    world.addParticle(emitterInfo.particleType,
                            initialPos.x + rotatedPoint.x,
                            initialPos.y + rotatedPoint.y,
                            initialPos.z + rotatedPoint.z,
                            emitterInfo.velocity.x,
                            emitterInfo.velocity.y,
                            emitterInfo.velocity.z);
                });
    }

    public static int genVertexArrays() {
        // TODO port:1.20.1 - APPLEVertexArrayObject path removed; GL30 is available on all platforms.
        return GL30.glGenVertexArrays();
    }

    public static void bindVertexArray(int vaoID) {
        GL30.glBindVertexArray(vaoID);
    }

    public static void checkForOglError() {
        int errorCode = GL11.glGetError();
        if (errorCode != GL11.GL_NO_ERROR) {
            DynamXMain.log.warn("errorCode = " + errorCode);
        }
    }

    /**
     * <p>TODO port:1.20.1 - {@code GL_ALL_ATTRIB_BITS} is unavailable in core profile.
     * Push/pop pair stubbed to no-op; replace with RenderSystem state-save semantics.</p>
     */
    public static void pushGlAllAttribBits() {
        if (!glAllAttribBitsEnabled) {
            glAllAttribBitsEnabled = true;
            // GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); -- removed in core profile
        }
    }

    public static void popGlAllAttribBits() {
        if (glAllAttribBitsEnabled) {
            glAllAttribBitsEnabled = false;
            // GL11.glPopAttrib(); -- removed in core profile
        }
    }
}
