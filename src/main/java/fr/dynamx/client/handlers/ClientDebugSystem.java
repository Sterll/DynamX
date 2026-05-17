package fr.dynamx.client.handlers;

import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.client.renders.RenderFrame;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.network.packets.MessageDebugRequest;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.debug.*;
import fr.dynamx.utils.debug.renderer.PhysicsDebugRenderer;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

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

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void overlay(RenderGuiOverlayEvent.Post event) {
        if (!enableDebugDrawing) return;
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        GuiGraphics gui = event.getGuiGraphics();
        Font font = MC.font;
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();

        String s = "Drawing debug";
        gui.drawString(font, s, width - font.width(s) - 2, 2, 0xFFBC00, false);
        if (DynamXContext.getPhysicsWorld(MC.level) != null) {
            s = "Entities: " + DynamXContext.getPhysicsWorld(MC.level).getLoadedEntityCount();
        } else {
            s = "Not simulating...";
        }
        gui.drawString(font, s, width - font.width(s) - 2, 12, 0xFFBC00, false);

        if (DynamXDebugOptions.PROFILING.isActive()) {
            if (MC.player != null && MC.player.tickCount % 3 == 0) {
                ProfilingData d = Profiler.get().getData(Profiler.Profiles.BULLET_STEP_SIM);
                if (d != null)
                    physicsTicks.add(d.save());
            }
            if (!physicsTicks.isEmpty()) {
                if (physicsTicks.size() > 105)
                    physicsTicks.remove(0);
                int x = 2;
                int c = 0;
                for (ProfilingData.Measure d1 : physicsTicks) {
                    d1.draw(gui, x, height - 20, font, c);
                    x += 12;
                    c++;
                }
            }
        } else if (!physicsTicks.isEmpty()) {
            physicsTicks.clear();
        }
    }

    static BasePart<?> lastPart = null;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @OnlyIn(Dist.CLIENT)
    public static void worldRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        prevRigidBodyStatesIndex = (byte) (ClientDebugSystem.curRigidBodyStatesIndex - 1);
        if (prevRigidBodyStatesIndex < 0)
            prevRigidBodyStatesIndex = 1;
        if (!enableDebugDrawing) return;

        PoseStack stack = event.getPoseStack();
        Camera camera = event.getCamera();
        float partialTicks = event.getPartialTick();
        net.minecraft.world.phys.Vec3 cam = camera.getPosition();

        MultiBufferSource.BufferSource buffers = MC.renderBuffers().bufferSource();
        RenderFrame.push(stack, buffers, 0xF000F0);
        stack.pushPose();
        stack.translate(-cam.x, -cam.y, -cam.z);

        try {
            drawDebug(DynamXDebugOptions.BLOCK_BOXES);
            drawDebug(DynamXDebugOptions.CLIENT_BLOCK_BOXES);
            drawDebug(DynamXDebugOptions.SLOPE_BOXES);
            drawDebug(DynamXDebugOptions.CLIENT_SLOPE_BOXES);

            if (DynamXDebugOptions.PHYSICS_DEBUG.isActive() && DynamXContext.getPhysicsWorld(MC.level) != null) {
                for (PhysicsRigidBody body : DynamXContext.getPhysicsWorld(MC.level).getDynamicsWorld().getRigidBodyList()) {
                    Vector3fPool.openPool();
                    QuaternionPool.openPool();
                    GlQuaternionPool.openPool();
                    PhysicsDebugRenderer.debugRigidBody(body, getPrevRigidBodyTransform(body.nativeId()), getCurrentRigidBodyTransform(body.nativeId()), partialTicks);
                    GlQuaternionPool.closePool();
                    Vector3fPool.closePool();
                    QuaternionPool.closePool();
                }
                Vector3fPool.openPool();
                QuaternionPool.openPool();
                GlQuaternionPool.openPool();
                DynamXContext.getPhysicsWorld(MC.level).getDynamicsWorld().getSoftBodyList().forEach(PhysicsDebugRenderer::debugSoftBody);
                Vector3fPool.closePool();
                QuaternionPool.closePool();

                Vector3fPool.openPool();
                QuaternionPool.openPool();
                for (PhysicsJoint physicsJoint : DynamXContext.getPhysicsWorld(MC.level).getDynamicsWorld().getJointList()) {
                    PhysicsDebugRenderer.debugConstraint(physicsJoint, partialTicks);
                }
                GlQuaternionPool.closePool();
                Vector3fPool.closePool();
                QuaternionPool.closePool();
            }
        } finally {
            stack.popPose();
            buffers.endBatch(RenderType.lines());
            RenderFrame.clear();
        }

        Vector3fPool.openPool();
        LocalPlayer rootPlayer = MC.player;
        if (rootPlayer != null && MC.hitResult instanceof EntityHitResult) {
            if (!rootPlayer.isShiftKeyDown()) {
                disableShapeDebug(lastPart);
                Vector3fPool.closePool();
                return;
            }
            EntityHitResult hit = (EntityHitResult) MC.hitResult;
            if (!(hit.getEntity() instanceof PackPhysicsEntity)) {
                disableShapeDebug(lastPart);
                Vector3fPool.closePool();
                return;
            }
            PackPhysicsEntity<?, ?> entityHit = (PackPhysicsEntity<?, ?>) hit.getEntity();
            if (!(entityHit.getPackInfo() instanceof IPartContainer)) {
                disableShapeDebug(lastPart);
                Vector3fPool.closePool();
                return;
            }
            Predicate<BasePart<?>> wantedShape = null;
            Optional<DynamXDebugOption> dynamXDebugOptions = DynamXDebugOptions.getAllOptions()
                    .stream()
                    .filter(dynamXDebugOption -> !dynamXDebugOption.equals(DynamXDebugOptions.DEBUG_RENDER)
                            && dynamXDebugOption.isActive()).findFirst();
            if (dynamXDebugOptions.isPresent()) {
                wantedShape = basePart -> {
                    if (basePart.getDebugOption() != null) {
                        return basePart.getDebugOption().equals(dynamXDebugOptions.get());
                    }
                    return false;
                };
            }
            BasePart<?> basePart = DynamXUtils.rayTestPart(rootPlayer, entityHit, (IPartContainer<?>) entityHit.getPackInfo(), wantedShape);
            if (basePart == null) {
                disableShapeDebug(lastPart);
                Vector3fPool.closePool();
                return;
            }
            if (lastPart != null && lastPart.getDebugOption() != null)
                ((DynamXDebugOption) lastPart.getDebugOption()).disable();
            if (basePart.getDebugOption() != null)
                ((DynamXDebugOption) basePart.getDebugOption()).enable();
            if (wantedShape == null)
                lastPart = basePart;
        }
        Vector3fPool.closePool();
    }

    private static void disableShapeDebug(BasePart<?> basePart) {
        if (basePart == null || basePart.getDebugOption() == null)
            return;
        ((DynamXDebugOption) basePart.getDebugOption()).disable();
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawDebug(DynamXDebugOption.TerrainDebugOption option) {
        if (option.getDataIn().isEmpty() || !option.isActive()) return;
        try {
            for (Map.Entry<Integer, TerrainDebugData> pos : option.getDataIn().entrySet()) {
                switch (pos.getValue().getRenderer()) {
                    case BLOCKS:
                    case DYNAMXBLOCKS:
                        drawAABBDebug(pos.getValue());
                        break;
                    case STAIRS:
                        drawSlopeDebug(pos.getValue().getData(), pos.getValue().getRenderer().getR(), pos.getValue().getRenderer().getG(), pos.getValue().getRenderer().getB(), 0.2f);
                        break;
                    case CUSTOM_SLOPE:
                        if (pos.getValue().getRenderer() == TerrainDebugRenderer.CUSTOM_SLOPE) {
                            float margin = 0.02f;
                            float[] p = pos.getValue().getData();
                            drawAABBDebug(new float[]{p[p.length - 3] - margin, p[p.length - 2] - margin, p[p.length - 1] - margin,
                                    p[p.length - 3] + margin, p[p.length - 2] + margin, p[p.length - 1] + margin,
                                    pos.getValue().getRenderer().getR(), pos.getValue().getRenderer().getG(), pos.getValue().getRenderer().getB()});
                        }
                    case SLOPES:
                        drawSlopeDebug(pos.getValue().getData(), pos.getValue().getRenderer().getR(), pos.getValue().getRenderer().getG(), pos.getValue().getRenderer().getB(), 0.5f);
                        break;
                }
            }
        } catch (ConcurrentModificationException ignored) {
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void drawSlopeDebug(float[] pos, float r, float g, float b, float a) {
        RenderFrame.Frame frame = RenderFrame.current();
        if (frame == null) return;
        com.mojang.blaze3d.vertex.VertexConsumer consumer = frame.bufferSource().getBuffer(RenderType.lines());
        org.joml.Matrix4f mat = frame.poseStack().last().pose();
        if (pos.length == 16) {
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) % 4;
                consumer.vertex(mat, pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2]).color(r, g, b, a).normal(0, 1, 0).endVertex();
                consumer.vertex(mat, pos[j * 3], pos[j * 3 + 1], pos[j * 3 + 2]).color(r, g, b, a).normal(0, 1, 0).endVertex();
            }
        } else {
            int tris = pos.length / 3;
            for (int i = 0; i < tris - 1; i++) {
                consumer.vertex(mat, pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2]).color(1f, 0f, 0f, 1f).normal(0, 1, 0).endVertex();
                consumer.vertex(mat, pos[(i + 1) * 3], pos[(i + 1) * 3 + 1], pos[(i + 1) * 3 + 2]).color(1f, 0f, 0f, 1f).normal(0, 1, 0).endVertex();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void drawAABBDebug(TerrainDebugData debugData) {
        RenderFrame.Frame frame = RenderFrame.current();
        if (frame == null) return;
        float[] pos = debugData.getData();
        com.mojang.blaze3d.vertex.VertexConsumer consumer = frame.bufferSource().getBuffer(RenderType.lines());
        net.minecraft.client.renderer.LevelRenderer.renderLineBox(frame.poseStack(), consumer,
                pos[0], pos[1], pos[2], pos[3], pos[4], pos[5],
                debugData.getRenderer().getR(), debugData.getRenderer().getG(), debugData.getRenderer().getB(), 1f);
    }

    @OnlyIn(Dist.CLIENT)
    public static void drawAABBDebug(float[] pos) {
        if (pos.length != 9)
            throw new IllegalStateException("Pos must have 9 floats !");
        RenderFrame.Frame frame = RenderFrame.current();
        if (frame == null) return;
        com.mojang.blaze3d.vertex.VertexConsumer consumer = frame.bufferSource().getBuffer(RenderType.lines());
        net.minecraft.client.renderer.LevelRenderer.renderLineBox(frame.poseStack(), consumer,
                pos[0], pos[1], pos[2], pos[3], pos[4], pos[5],
                pos[6], pos[7], pos[8], 1f);
    }

    @OnlyIn(Dist.CLIENT)
    public static void fillFaceBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        // No longer used after the LevelRenderer.renderLineBox migration; kept for API compatibility.
    }

    @OnlyIn(Dist.CLIENT)
    public static void drawFaceBoxBorders(Object buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float red, float green, float blue, float alpha) {
        // No longer used after the LevelRenderer.renderLineBox migration; kept for API compatibility.
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
