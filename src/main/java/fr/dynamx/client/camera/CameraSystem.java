package fr.dynamx.client.camera;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ViewportEvent;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles camera rotation and zoom while in a vehicle.
 *
 * <p>TODO port:1.20.1 - The original used:
 * <ul>
 *   <li>{@code GlStateManager.rotate/translate/color/disableTexture2D/disableDepth/...} -- all gone
 *       in core profile. The whole "rotate the modelview around the vehicle" pipeline has to be
 *       moved into the {@code RenderLevelStageEvent.AFTER_ENTITIES} stage using {@code PoseStack}.</li>
 *   <li>{@code GL11.glTranslated/glBegin/glVertex3f/glEnd} -- gone, use {@code MultiBufferSource}.</li>
 *   <li>{@code EntityViewRenderEvent.CameraSetup} -> {@link ViewportEvent.ComputeCameraAngles}
 *       (event provides yaw/pitch/roll setters but no longer wraps the GL stack).</li>
 *   <li>{@code Minecraft.getMinecraft().gameSettings.thirdPersonView} (int) ->
 *       {@code Minecraft.getInstance().options.getCameraType().ordinal()}.</li>
 *   <li>{@code RayTraceResult/Vec3d/world.rayTraceBlocks} -> {@code HitResult/Vec3/level.clip(ClipContext)}.</li>
 *   <li>{@code org.lwjgl.input.Keyboard.isKeyDown(KEY_B)} -> NeoForge {@code InputConstants} +
 *       a {@code KeyMapping}.</li>
 * </ul>
 * Public entry points are kept; bodies are stubbed where they would require the new render
 * pipeline.</p>
 */
public class CameraSystem {
    private static Map<CameraMode, CameraMode> preferredCameraMode = new HashMap<>();
    private static CameraMode cameraMode = CameraMode.AUTO;

    private static int zoomLevel = 4;
    private static float cameraPositionY;
    private static boolean watchingBehind = false;

    private static final Quaternionf glQuatCache = new Quaternionf();
    private static final com.jme3.math.Quaternion jmeQuatCache = new com.jme3.math.Quaternion();
    private static com.jme3.math.Quaternion lastCameraQuat;

    /**
     * Computes a smooth interpolated camera rotation. Public state in {@link #jmeQuatCache}
     * and {@link #glQuatCache} is updated for callers (debug rendering, raycast).
     */
    private static void animateCameraRotation(com.jme3.math.Quaternion prevRotation, com.jme3.math.Quaternion rotation, float step, float animLength) {
        DynamXMath.slerp(step, prevRotation, rotation, jmeQuatCache);
        DynamXGeometry.inverseQuaternion(jmeQuatCache, jmeQuatCache);
        // TODO port:1.20.1 - options.getCameraType().ordinal() instead of gameSettings.thirdPersonView.
        int cameraTypeOrdinal = Minecraft.getInstance().options.getCameraType().ordinal();
        cameraMode.rotator.apply(cameraTypeOrdinal, jmeQuatCache);

        jmeQuatCache.normalizeLocal();

        if (lastCameraQuat == null)
            lastCameraQuat = new com.jme3.math.Quaternion(jmeQuatCache.getX(), jmeQuatCache.getY(), jmeQuatCache.getZ(), jmeQuatCache.getW());
        else
            DynamXMath.slerp(animLength, lastCameraQuat, jmeQuatCache, lastCameraQuat);
        lastCameraQuat.normalizeLocal();
        glQuatCache.set(lastCameraQuat.getX(), lastCameraQuat.getY(), lastCameraQuat.getZ(), lastCameraQuat.getW());
    }

    /**
     * Makes the camera follow the ridden vehicle. Called from {@link ViewportEvent.ComputeCameraAngles}.
     *
     * <p>port:1.20.1 - the 1.12 version drove the whole camera through the GL matrix stack (gone in
     * core profile); the 1.20.1 event only exposes yaw/pitch/roll. Two things are done here:</p>
     * <ul>
     *   <li><b>Yaw smoothing</b>: the seated player's yaw is force-followed to the vehicle each tick
     *   ({@code SeatsModule#updatePassenger}), and the vehicle yaw advances unevenly per tick (physics
     *   runs on its own thread), so the third-person camera jitters in turns. We rebuild the camera
     *   yaw from the vehicle's <i>interpolated</i> yaw plus the player's free-look offset, which is
     *   smooth. Uses {@link Mth#rotLerp} so it stays in Minecraft's yaw convention (no flip risk).</li>
     *   <li><b>Tilt</b>: roll (banking) and pitch (incline) are derived from the vehicle's basis
     *   vectors (yaw-independent, so a flat turn adds no tilt) and applied only in first-person or
     *   FIXED mode - AUTO third person stays untilted like the original.</li>
     * </ul>
     */
    public static void rotateVehicleCamera(ViewportEvent.ComputeCameraAngles event) {
        if (!(event.getCamera().getEntity().getVehicle() instanceof PhysicsEntity)) {
            return;
        }
        net.minecraft.world.entity.Entity camEntity = event.getCamera().getEntity();
        PhysicsEntity<?> vehicle = (PhysicsEntity<?>) camEntity.getVehicle();
        if (cameraMode == CameraMode.FREE) {
            return; // free look: leave the camera entirely to vanilla
        }
        float partial = (float) event.getPartialTick();
        Vector3fPool.openPool();
        QuaternionPool.openPool();
        try {
            // Smooth yaw follow: swap the jittery per-tick vehicle yaw baked into the player's look
            // for the interpolated vehicle yaw. The free-look offset is taken from the (non-interp)
            // player vs vehicle yaw so both sides use the same frame of reference.
            float smoothVehicleYaw = net.minecraft.util.Mth.rotLerp(partial, vehicle.yRotO, vehicle.getYRot());
            float relativeYaw = net.minecraft.util.Mth.wrapDegrees(camEntity.getYRot() - vehicle.getYRot());
            event.setYaw(smoothVehicleYaw + relativeYaw + (watchingBehind ? 180f : 0f));

            // Tilt only in first person / FIXED, like the original CameraMode rotator.
            boolean firstPerson = Minecraft.getInstance().options.getCameraType().ordinal() == 0;
            if (cameraMode == CameraMode.FIXED || (cameraMode == CameraMode.AUTO && firstPerson)) {
                com.jme3.math.Quaternion rot = DynamXMath.slerp(partial, vehicle.prevRenderRotation, vehicle.renderRotation, jmeQuatCache);
                Vector3f forward = DynamXGeometry.getRotationColumn(rot, 2, Vector3fPool.get());
                Vector3f left = DynamXGeometry.getRotationColumn(rot, 0, Vector3fPool.get());
                event.setPitch(event.getPitch() + DynamXGeometry.getPitchFromRotationVector(forward));
                event.setRoll(event.getRoll() + DynamXGeometry.getRollFromRotationVector(left, forward));
            }
        } finally {
            QuaternionPool.closePool();
            Vector3fPool.closePool();
        }
    }

    private static final Vector3f pt0 = new Vector3f();
    private static final Vector3f pt1 = new Vector3f();
    private static final Vector3f pt2 = new Vector3f();
    private static final Vector3f pt3 = new Vector3f();

    private static final Map<Vector3f, Vector3f> cameraRadius = new HashMap<>();

    /**
     * <p>TODO port:1.20.1 - immediate-mode GL_LINES debug draws gone. Replace with
     * {@code MultiBufferSource} + {@code RenderType.LINES} in a {@code RenderLevelStageEvent} handler.</p>
     */
    public static void drawDebug() {
        // TODO port:1.20.1 - all draws stubbed; state collection kept.
    }

    /**
     * Zooms using the by the zoomLevel amount, raycasting around the camera to avoid having the
     * camera inside of a block.
     *
     * <p>TODO port:1.20.1 - world.rayTraceBlocks(Vec3d, Vec3d, ...) -> level.clip(new ClipContext(...))
     * and {@code Entity.prevPosX/Y/Z/posX/Y/Z} -> {@code xOld/yOld/zOld/getX/getY/getZ}.</p>
     */
    @SuppressWarnings("unused")
    private static void performZoomAction(net.minecraft.world.entity.Entity entity, float yaw, float pitch, double partialTicks, com.jme3.math.Quaternion vRotation) {
        // TODO port:1.20.1 - body stubbed. Raycast + GL_translated re-implementation required.
    }

    public static void changeCameraZoom(boolean zoomOut) {
        if (zoomOut) {
            if (zoomLevel < DynamXConfig.maxZoomOut)
                zoomLevel += 2;
        } else {
            if (zoomLevel > 4) {
                zoomLevel -= 2;
            }
        }
    }

    public static void setupCamera(IModuleContainer.ISeatsContainer entity) {
        // TODO port:1.20.1 - IModuleContainer.cast() returns Object pending Phase 6 entity port.
        //   Cast to PackPhysicsEntity to access getPackInfo(); seats helper is also typed as Object.
        fr.dynamx.common.entities.PackPhysicsEntity<?, ?> ent = (fr.dynamx.common.entities.PackPhysicsEntity<?, ?>) entity.cast();
        zoomLevel = ent.getPackInfo() instanceof ModularVehicleInfo ? ((ModularVehicleInfo) ent.getPackInfo()).getDefaultZoomLevel() : 4;
        CameraMode mode = ((fr.dynamx.common.entities.modules.SeatsModule) entity.getSeats()).getPreferredCameraMode();
        if (!preferredCameraMode.containsKey(mode))
            preferredCameraMode.put(mode, mode);
        cameraMode = preferredCameraMode.get(mode);
    }

    public static CameraMode cycleCameraMode(IModuleContainer.ISeatsContainer entity) {
        switch (cameraMode) {
            case AUTO:
                cameraMode = CameraMode.FIXED;
                break;
            case FIXED:
                cameraMode = CameraMode.FREE;
                break;
            case FREE:
                cameraMode = CameraMode.AUTO;
                break;
        }
        // TODO port:1.20.1 - cast to SeatsModule (Object placeholder in IModuleContainer).
        preferredCameraMode.put(((fr.dynamx.common.entities.modules.SeatsModule) entity.getSeats()).getPreferredCameraMode(), cameraMode);
        return cameraMode;
    }

    public static void setWatchingBehind(boolean watchingBehind) {
        CameraSystem.watchingBehind = watchingBehind;
    }
}
