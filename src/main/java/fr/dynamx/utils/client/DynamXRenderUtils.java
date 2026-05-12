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
        // TODO port:1.20.1 - GlStateManager.color/translate/scale removed. Stub.
    }

    public static void glTranslate(Vector3f translation) {
        // TODO port:1.20.1 - GlStateManager.translate gone. Use PoseStack.translate at the call site.
    }

    public static void drawConvexHull(List<Vector3f> vectorBuffer, boolean wireframe) {
        // TODO port:1.20.1 - glPolygonMode / glBegin(GL_TRIANGLES) gone. Rewrite with VertexConsumer.
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
        // TODO port:1.20.1 - World.spawnParticle -> Level.addParticle(ParticleOptions, x, y, z, vx, vy, vz).
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
