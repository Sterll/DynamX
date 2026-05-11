package fr.dynamx.client.handlers;

import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.client.camera.CameraSystem;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.BasePartSeat;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.network.packets.MessageDebugRequest;
import fr.dynamx.common.network.sync.variables.NetworkActivityTracker;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.*;
import fr.dynamx.utils.debug.renderer.PhysicsDebugRenderer;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Client-side debug overlay + world-render hooks.
 *
 * <p>TODO port:1.20.1 - extensive rendering rewrite required:</p>
 * <ul>
 *   <li>{@code @Mod.EventBusSubscriber(value = Side.CLIENT)} -> {@code @Mod.EventBusSubscriber(value = Dist.CLIENT)}.</li>
 *   <li>{@code @SideOnly(Side.CLIENT)} -> {@code @OnlyIn(Dist.CLIENT)}.</li>
 *   <li>{@code RenderGameOverlayEvent.Post(TEXT)} -> {@code RenderGuiOverlayEvent.Post} with {@code VanillaGuiOverlay.HOTBAR}/etc. Pre-1.20 ElementType.TEXT now lives across multiple overlays.</li>
 *   <li>{@code RenderWorldLastEvent} -> {@code RenderLevelStageEvent}.</li>
 *   <li>{@code GlStateManager.*} immediate-mode + {@code GL11.GL_TRIANGLES/GL_LINE_STRIP/etc.} drawing -> {@code Tesselator}/{@code BufferBuilder} with {@code DefaultVertexFormat.POSITION_COLOR} and an explicit {@code RenderType}.</li>
 *   <li>{@code event.getResolution().getScaledWidth/Height} -> {@code GuiGraphics.guiWidth()}/{@code guiHeight()}.</li>
 *   <li>{@code MC.fontRenderer} -> {@code MC.font}; {@code Font#getStringWidth} -> {@code Font#width}; {@code FONT_HEIGHT} -> {@code lineHeight}.</li>
 *   <li>{@code rootPlayer.posX/lastTickPosX/etc.} -> {@code rootPlayer.getX()/xOld/etc.}.</li>
 *   <li>{@code rootPlayer.isSneaking()} -> {@code rootPlayer.isShiftKeyDown()}.</li>
 *   <li>{@code MC.objectMouseOver} -> {@code MC.hitResult}; {@code entityHit} -> {@code ((EntityHitResult) hit).getEntity()}.</li>
 *   <li>{@code rootPlayer.rotationYaw/Pitch} -> {@code rootPlayer.getYRot()/getXRot()}.</li>
 *   <li>{@code MC.world} -> {@code MC.level}.</li>
 * </ul>
 * The class keeps its tracked-rigid-body bookkeeping intact; render hooks are stubbed.
 */
@Mod.EventBusSubscriber(modid = DynamXConstants.ID, value = Dist.CLIENT)
public class ClientDebugSystem {
    private static final List<ProfilingData.Measure> physicsTicks = new ArrayList<>();
    public static boolean enableDebugDrawing;

    public static final Map<Long, PhysicsRigidBody> trackedRigidBodies = new ConcurrentHashMap<>();
    public static final Map<Long, RigidBodyTransform>[] prevRigidBodyStates = new Map[]{new HashMap<>(), new HashMap<>()};

    private static byte curRigidBodyStatesIndex;
    private static byte prevRigidBodyStatesIndex;

    private static final Minecraft MC = Minecraft.getInstance();

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void tickEvent(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            enableDebugDrawing = DynamXDebugOptions.DEBUG_RENDER.isActive();
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.tickCount % 5 == 0) {
                int request = 0;
                for (DynamXDebugOptions.DebugCategories categories : DynamXDebugOptions.DebugCategories.values()) {
                    for (DynamXDebugOption o : categories.getOptions()) {
                        if (o.serverRequestMask() != 0 && o.isActive()) {
                            request = request | o.serverRequestMask();
                            break;
                        }
                    }
                }
                if (request != 0) {
                    DynamXContext.getNetwork().sendToServer(new MessageDebugRequest(request));
                }
            }

            if (MC.level != null && DynamXContext.getPhysicsWorld(MC.level) != null) {
                curRigidBodyStatesIndex++;
                if (curRigidBodyStatesIndex > 1) {
                    curRigidBodyStatesIndex = 0;
                }
                prevRigidBodyStates[curRigidBodyStatesIndex].keySet().removeIf(aLong -> !trackedRigidBodies.containsKey(aLong));
                for (Map.Entry<Long, PhysicsRigidBody> e : trackedRigidBodies.entrySet()) {
                    prevRigidBodyStates[curRigidBodyStatesIndex].compute(e.getKey(), (k, v) -> {
                        if (v == null)
                            return new RigidBodyTransform(e.getValue());
                        v.set(e.getValue());
                        return v;
                    });
                }
            }
        }
    }

    /**
     * <p>TODO port:1.20.1 - replace with {@code RenderGuiOverlayEvent.Post} listening on the
     * {@code VanillaGuiOverlay.HOTBAR} layer. Use the event's {@code GuiGraphics} for text rendering.</p>
     */
    @OnlyIn(Dist.CLIENT)
    public static void overlay(/* RenderGuiOverlayEvent.Post */ Object event) {
        // TODO port:1.20.1 - @SubscribeEvent removed; restore once parameter is a real Event subtype.
        // TODO port:1.20.1 - body fully stubbed pending RenderGuiOverlayEvent integration.
        // Original logic: draws "Drawing debug", entity count, physics-tick bars, network-activity panel
        // on the top-right of the HUD using fontRenderer + Profiler data.
    }

    static BasePart<?> lastPart = null;

    /**
     * <p>TODO port:1.20.1 - replace with {@code RenderLevelStageEvent} (stage =
     * {@code Stage.AFTER_TRANSLUCENT_BLOCKS} or {@code AFTER_PARTICLES}). Use
     * {@code event.getPoseStack()} + {@code MultiBufferSource} + an appropriate
     * {@code RenderType} for line/box draws. {@code GlStateManager} immediate-mode is gone.</p>
     */
    @OnlyIn(Dist.CLIENT)
    public static void worldRender(/* RenderLevelStageEvent */ Object event) {
        // TODO port:1.20.1 - @SubscribeEvent(priority = EventPriority.HIGHEST) removed; restore once parameter is a real Event subtype.
        prevRigidBodyStatesIndex = (byte) (ClientDebugSystem.curRigidBodyStatesIndex - 1);
        if (prevRigidBodyStatesIndex < 0)
            prevRigidBodyStatesIndex = 1;
        // TODO port:1.20.1 - body fully stubbed (immediate-mode GL11 + GlStateManager calls
        // unavailable in core profile). Need PoseStack/MultiBufferSource rewrite, including:
        //  - drawDebug(...) helpers (block boxes / slope boxes / custom slopes).
        //  - PhysicsDebugRenderer.debugRigidBody/debugSoftBody/debugConstraint.
        //  - CameraSystem.drawDebug().
        //  - rayTested part nameplate via DynamXRenderUtils.drawNameplate.
    }

    private static void disableShapeDebug(BasePart<?> basePart) {
        if (basePart == null || basePart.getDebugOption() == null)
            return;
        // TODO port:1.20.1 - BasePart.getDebugOption returns Object pending part-API typing pass.
        ((fr.dynamx.utils.debug.DynamXDebugOption) basePart.getDebugOption()).disable();
    }

    /**
     * <p>TODO port:1.20.1 - stub, see worldRender. The helper drew filled + outlined AABB-like shapes
     * for block / slope debug data.</p>
     */
    @OnlyIn(Dist.CLIENT)
    private static void drawDebug(DynamXDebugOption.TerrainDebugOption option) {
        // TODO port:1.20.1 - stubbed.
    }

    /**
     * <p>TODO port:1.20.1 - replaces immediate-mode GL_QUADS / GL_TRIANGLES / GL_LINES drawing
     * with BufferBuilder.begin(VertexFormat.Mode.QUADS, POSITION_COLOR) + GameRenderer.getPositionColorShader().</p>
     */
    @OnlyIn(Dist.CLIENT)
    public static void drawSlopeDebug(float[] pos, float r, float g, float b, float a) {
        // TODO port:1.20.1 - stubbed.
    }

    /**
     * <p>TODO port:1.20.1 - see drawSlopeDebug. Renders a filled+outlined AABB.</p>
     */
    @OnlyIn(Dist.CLIENT)
    public static void drawAABBDebug(TerrainDebugData debugData) {
        // TODO port:1.20.1 - stubbed.
    }

    /**
     * <p>TODO port:1.20.1 - see drawSlopeDebug. Variant accepting a 9-float (xyz min, xyz max, rgb) array.</p>
     */
    @OnlyIn(Dist.CLIENT)
    public static void drawAABBDebug(float[] pos) {
        if (pos.length != 9)
            throw new IllegalStateException("Pos must have 9 floats !");
        // TODO port:1.20.1 - stubbed.
    }

    /**
     * <p>TODO port:1.20.1 - was a stream of {@code GlStateManager.glVertex3f} calls inside a {@code GL_QUADS} block.
     * Replace with explicit {@code BufferBuilder.vertex(...)} calls using POSITION_COLOR vertex format.</p>
     */
    @OnlyIn(Dist.CLIENT)
    public static void fillFaceBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        // TODO port:1.20.1 - stubbed.
    }

    /**
     * <p>TODO port:1.20.1 - was a {@code BufferBuilder} GL_LINE_STRIP polyline. Keep the same shape but
     * call {@code buffer.vertex(...).color(...).endVertex()} on a 1.20 BufferBuilder. The {@code BufferBuilder}
     * type moved from {@code net.minecraft.client.renderer} (1.12) to {@code com.mojang.blaze3d.vertex} (1.20).</p>
     */
    @OnlyIn(Dist.CLIENT)
    public static void drawFaceBoxBorders(/* BufferBuilder */ Object buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float red, float green, float blue, float alpha) {
        // TODO port:1.20.1 - stubbed.
    }

    public static Vector3f getInterpolatedTranslation(PhysicsRigidBody rigidBody, float partialTicks) {
        RigidBodyTransform prevTransform = ClientDebugSystem.getPrevRigidBodyTransform(rigidBody.nativeId());
        RigidBodyTransform curTransform = ClientDebugSystem.getCurrentRigidBodyTransform(rigidBody.nativeId());
        if (prevTransform == null || curTransform == null) {
            return Vector3fPool.get();
        }
        return DynamXMath.interpolateLinear(partialTicks, prevTransform.getPosition(), curTransform.getPosition());
    }

    public static com.jme3.math.Quaternion getInterpolatedRotation(PhysicsRigidBody rigidBody, float partialTicks) {
        RigidBodyTransform prevTransform = ClientDebugSystem.getPrevRigidBodyTransform(rigidBody.nativeId());
        RigidBodyTransform curTransform = ClientDebugSystem.getCurrentRigidBodyTransform(rigidBody.nativeId());
        if (prevTransform == null || curTransform == null) {
            return QuaternionPool.get();
        }
        return DynamXMath.slerp(partialTicks, prevTransform.getRotation(), curTransform.getRotation());
    }

    public static RigidBodyTransform getCurrentRigidBodyTransform(long nativeBodyId) {
        return ClientDebugSystem.prevRigidBodyStates[ClientDebugSystem.curRigidBodyStatesIndex].get(nativeBodyId);
    }

    public static RigidBodyTransform getPrevRigidBodyTransform(long nativeBodyId) {
        return ClientDebugSystem.prevRigidBodyStates[ClientDebugSystem.prevRigidBodyStatesIndex].get(nativeBodyId);
    }
}
