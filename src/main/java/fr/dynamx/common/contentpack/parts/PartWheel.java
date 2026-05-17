package fr.dynamx.common.contentpack.parts;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.client.renders.scene.node.SimpleNode;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.maths.DynamXMath;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO port:1.20.1 - AxisAlignedBB -> net.minecraft.world.phys.AABB.
 * The InteractivePart generic A is relaxed to Object (was BaseVehicleEntity in 1.12).
 */
@Getter
@Setter
@RegisteredSubInfoType(name = "wheel", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartWheel extends InteractivePart<Object, ModularVehicleInfo> implements IDrawablePart<ModularVehicleInfo> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("isRight".equals(key))
            return new IPackFilePropertyFixer.FixResult("IsRight", true);
        return null;
    };

    @Accessors(fluent = true)
    @PackFileProperty(configNames = "IsRight", required = false, defaultValue = "True if name contains 'right'")
    protected boolean isRight;
    @PackFileProperty(configNames = "IsSteerable", required = false, defaultValue = "True if name contains 'front'")
    protected boolean wheelIsSteerable;
    @PackFileProperty(configNames = "MaxTurn", required = false, defaultValue = "0")
    protected float wheelMaxTurn;
    @PackFileProperty(configNames = "DrivingWheel")
    protected boolean drivingWheel;
    @PackFileProperty(configNames = "HandBrakingWheel", required = false)
    protected boolean handBrakingWheel;
    @PackFileProperty(configNames = "AttachedWheel")
    protected String defaultWheelName;
    @PackFileProperty(configNames = "Rim", required = false)
    protected String rimObjectName;
    @PackFileProperty(configNames = {"Tire", "Tyre"}, required = false)
    protected String tireObjectName;
    @PackFileProperty(configNames = "MudGuard", required = false)
    protected String mudGuardObjectName;
    @PackFileProperty(configNames = "RotationPoint", required = false, type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, defaultValue = "From model")
    protected Vector3f rotationPoint;
    @PackFileProperty(configNames = "SuspensionAxis", required = false, defaultValue = "From model")
    protected Quaternion suspensionAxis;

    protected PartWheelInfo defaultWheelInfo;

    public PartWheel(ModularVehicleInfo owner, String partName) {
        super(owner, partName, 0.75f, 0.75f);
        wheelIsSteerable = partName.toLowerCase().contains("front");
        isRight = partName.toLowerCase().contains("right");
    }

    /**
     * TODO port:1.20.1 - Original read the rotation point / suspension axis from the obj model via
     *   DxModelData. Stubbed; values must now be configured explicitly until the obj pipeline is ported.
     */
    protected void readMudguardPositionFromModel(net.minecraft.resources.ResourceLocation model) {
        // TODO port:1.20.1 - Restore DxModelData lookup once Phase 7 ports the obj pipeline.
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        if (getRimObjectName() != null)
            readPositionFromModel(owner.getModel(), getRimObjectName(), true, false);
        if (getMudGuardObjectName() != null)
            readMudguardPositionFromModel(owner.getModel());
        super.appendTo(owner);
        if (getRotationPoint() == null)
            rotationPoint = getPosition();
        else
            getRotationPoint().multLocal(getScaleModifier(owner));
        if (suspensionAxis != null && (suspensionAxis.inverse() == null || suspensionAxis.getW() == 0)) {
            DynamXErrorManager.addPackError(getPackName(), "wheel_invalid_suspaxis", ErrorLevel.HIGH, getName(), "The SuspensionAxis should be an invertible Quaternion");
            suspensionAxis = null;
        }
    }

    @Override
    public Object getDebugOption() {
        // TODO port:1.20.1 - return DynamXDebugOptions.WHEELS once the debug package is ported.
        return null;
    }

    @Override
    public boolean interact(Object entity, Player with) {
        return false;
    }

    public void setDefaultWheelInfo(PartWheelInfo partWheelInfo) {
        if (partWheelInfo == null) {
            throw new IllegalArgumentException("Attached wheel info " + getDefaultWheelName() + " was not found !");
        }
        defaultWheelInfo = partWheelInfo;
        setBox(new AABB(-partWheelInfo.getWheelWidth(), -partWheelInfo.getWheelRadius(), -partWheelInfo.getWheelRadius(),
                partWheelInfo.getWheelWidth(), partWheelInfo.getWheelRadius(), partWheelInfo.getWheelRadius()));
        if (getRimObjectName() == null && partWheelInfo.getModel() == null) {
            DynamXErrorManager.addPackError(getPackName(), "wheel_no_model", ErrorLevel.HIGH, owner.getFullName(), getName() + " using wheel info: " + partWheelInfo.getFullName());
        }
    }

    @Override
    public String getName() {
        return getPartName();
    }

    @Override
    public String[] getRenderedParts() {
        return java.util.stream.Stream.of(getRimObjectName(), getTireObjectName(), getMudGuardObjectName()).filter(java.util.Objects::nonNull).toArray(String[]::new);
    }

    @Override
    public String getObjectName() {
        return null;
    }

    @Override
    public void addToSceneGraph(ModularVehicleInfo packInfo, Object sceneBuilder) {
        ((SceneBuilder<?, ModularVehicleInfo>) sceneBuilder).addNode(packInfo, this);
    }

    @Override
    public Object createSceneGraph(Vector3f modelScale, List<Object> childGraph) {
        if (getMudGuardObjectName() != null) {
            PartAttachedWheelNode<ModularVehicleInfo> wheelNode = new PartAttachedWheelNode<>(this, modelScale, (List) childGraph);
            if (childGraph == null) childGraph = new ArrayList<>();
            childGraph.add(wheelNode);
            return new PartBaseWheelNode<>(this, modelScale, (List) childGraph, true);
        }
        return new PartBaseWheelNode<>(this, modelScale, (List) childGraph, false);
    }

    @Override
    public String getNodeName() {
        return getPartName();
    }

    @Override
    public void addModules(Object entity, Object modules) {
        // TODO port:1.20.1 - addModules signature (Object,Object) bridges Phase 6 ModuleListBuilder.
        // The actual wiring of WheelsModule lives in BaseVehicleEntity.createModules() in the 1.20.1
        // port; this hook is intentionally left no-op here.
    }

    /**
     * TODO port:1.20.1 - Original printed render insights using DxModelRegistry / ObjModelRenderer.
     */
    public void printFullRenderInsights(Object entity, boolean modelInsights) {
        System.out.println("=-=-=-=-=");
        System.out.println("Wheel " + getName() + ", id=" + getId() + " render infos:");
        System.out.println("Default info: " + getDefaultWheelInfo());
        System.out.println("rim object: " + getRimObjectName());
        System.out.println("tire object: " + getTireObjectName());
        System.out.println("mud guard object: " + getMudGuardObjectName());
        System.out.println("=-=-=-=-=");
    }

    /* -------------------------------------------------------------------- */
    /* Scene graph nodes (ported to PoseStack-native rendering for 1.20.1). */
    /* -------------------------------------------------------------------- */

    private static @Nullable WheelsModule getWheelsModule(BaseRenderContext.EntityRenderContext context) {
        ModularPhysicsEntity<?> entity = context.getEntity();
        if (entity instanceof BaseVehicleEntity<?>) {
            return ((BaseVehicleEntity<?>) entity).getModuleByType(WheelsModule.class);
        }
        return null;
    }

    /** Pushes a single visual-property interpolation onto the {@link PoseStack} / {@link Matrix4f}. */
    private static float interpProp(WheelsModule wm, int idx, float partialTicks) {
        return wm.prevVisualProperties[idx] + (wm.visualProperties[idx] - wm.prevVisualProperties[idx]) * partialTicks;
    }

    /**
     * If the wheel has a mudguard, this node renders the mudguard and an attached wheel child node
     * actually renders the wheel. Otherwise this node renders the wheel itself.
     *
     * <p>1.20.1 port note: transforms are now applied directly to the PoseStack so the normal matrix
     * tracks rotations/scales correctly (the legacy GlStateManager.multMatrix path is gone).
     */
    class PartBaseWheelNode<A extends ModularVehicleInfo> extends SimpleNode<BaseRenderContext.EntityRenderContext, A> {
        private final boolean isMudGuard;
        private static final java.util.Set<String> DUMPED_WHEELS = java.util.concurrent.ConcurrentHashMap.newKeySet();

        public PartBaseWheelNode(PartWheel wheel, Vector3f scale, List<SceneNode<BaseRenderContext.EntityRenderContext, A>> linkedChilds, boolean isMudGuard) {
            super(isMudGuard ? wheel.getRotationPoint() : wheel.getPosition(), wheel.getSuspensionAxis(), PartWheel.this.isAutomaticPosition, scale, linkedChilds);
            this.isMudGuard = isMudGuard;
        }

        @Override
        public void render(BaseRenderContext.EntityRenderContext context, A packInfo, Matrix4f parentTransform) {
            WheelsModule wheelsModule = getWheelsModule(context);
            boolean hasWheelsModule = wheelsModule != null;
            String diagKey = packInfo.getFullName() + "|" + getPartName() + "|mg=" + isMudGuard;
            boolean dump = DUMPED_WHEELS.add(diagKey);
            if (!isMudGuard && hasWheelsModule && wheelsModule.getWheelsStates()[getId()] == WheelsModule.WheelState.REMOVED) {
                if (dump) org.apache.logging.log4j.LogManager.getLogger("DynamX-WheelDump").info("wheel SKIP removed: {}", diagKey);
                return;
            }

            PartWheelInfo info = hasWheelsModule ? wheelsModule.getWheelInfo((byte) getId()) : getDefaultWheelInfo();
            if (info == null || (!info.isModelValid() && getRimObjectName() == null && !isMudGuard)) {
                if (dump) org.apache.logging.log4j.LogManager.getLogger("DynamX-WheelDump").info(
                        "wheel SKIP null/invalid: {} info={} modelValid={} rim={} mg={}",
                        diagKey, info, info != null && info.isModelValid(), getRimObjectName(), isMudGuard);
                return;
            }
            if (dump) org.apache.logging.log4j.LogManager.getLogger("DynamX-WheelDump").info(
                    "wheel RENDER: {} pos={} rot={} autoPos={} rim={} tire={} mg={} sepModel={} hasWM={} poseStack={}",
                    diagKey, translation, rotation, isAutomaticPosition, getRimObjectName(), getTireObjectName(), getMudGuardObjectName(),
                    getRimObjectName() == null, hasWheelsModule, context.getPoseStack() != null);

            // Maintain the Matrix4f transform so attached-wheel children can chain off the
            // rotation point + suspension axis. Children read it via transformToRotationPoint.
            transformToRotationPoint(parentTransform);

            PoseStack pose = context.getPoseStack();
            if (pose == null) return;
            pose.pushPose();

            // 1. Translate to the rotation point (or wheel position, if no mudguard).
            //    The entity scale is already on the PoseStack (applied in EntityNode), so the
            //    translation is in entity-local-scaled space - matching legacy semantics.
            if (translation != null) {
                pose.translate(translation.x, translation.y, translation.z);
            }
            // 2. Apply the suspension axis rotation (rotation field on SimpleNode).
            if (rotation != null) {
                pose.mulPose(rotation);
            }

            // 3. Dynamic transforms (steering, suspension travel, wheel spin).
            if (hasWheelsModule) {
                if (isWheelIsSteerable() && !isMudGuard) {
                    int idx = VehicleEntityProperties.getPropertyIndex(getId(), VehicleEntityProperties.EnumVisualProperties.STEER_ANGLE);
                    float angle = interpProp(wheelsModule, idx, context.getPartialTicks());
                    pose.mulPose(Axis.YP.rotation(angle * DynamXMath.TO_RADIAN));
                    transform.rotate(angle * DynamXMath.TO_RADIAN, 0, 1, 0);
                }
                int idx = VehicleEntityProperties.getPropertyIndex(getId(), VehicleEntityProperties.EnumVisualProperties.SUSPENSION_LENGTH);
                float suspensionLength = -interpProp(wheelsModule, idx, context.getPartialTicks());
                float dy = suspensionLength - info.getSuspensionRestLength();
                pose.translate(0, dy, 0);
                transform.translate(0, dy, 0);
                if (!isMudGuard) {
                    applyWheelRotationPose(pose, transform, context, wheelsModule);
                }
            } else {
                float dy = -info.getSuspensionRestLength();
                pose.translate(0, dy, 0);
                transform.translate(0, dy, 0);
            }

            // 4. If the wheel position came from the OBJ model, undo the rotation-point
            //    translation/rotation so the geometry renders at the position baked into
            //    the OBJ (legacy glTransformToPartPos behaviour).
            if (isAutomaticPosition) {
                if (rotation != null) {
                    Quaternionf inv = new Quaternionf();
                    rotation.invert(inv);
                    pose.mulPose(inv);
                }
                if (translation != null) {
                    pose.translate(-translation.x, -translation.y, -translation.z);
                }
            }

            // 5. Render.
            if (isMudGuard) {
                if (context.getModel() != null && getMudGuardObjectName() != null) {
                    context.getModel().renderGroup(getMudGuardObjectName(), context.getTextureId(), context.isUseVanillaRender());
                }
            } else {
                renderWheelGeometry(context, info, wheelsModule);
            }

            pose.popPose();

            renderChildren(context, packInfo, transform);
        }
    }

    /**
     * Wheel rendered as a child of {@link PartBaseWheelNode} (when there is a mudguard).
     */
    class PartAttachedWheelNode<A extends ModularVehicleInfo> extends SimpleNode<BaseRenderContext.EntityRenderContext, A> {
        public PartAttachedWheelNode(PartWheel wheel, Vector3f scale, List<SceneNode<BaseRenderContext.EntityRenderContext, A>> linkedChilds) {
            super(PartWheel.this.isAutomaticPosition
                            ? wheel.getPosition()
                            : new Vector3f(wheel.getPosition().subtract(wheel.getRotationPoint())),
                    (Quaternion) null, PartWheel.this.isAutomaticPosition, scale, linkedChilds);
            if (wheel.getSuspensionAxis() != null && !isAutomaticPosition && translation != null) {
                // Mudguard rotation has already been applied; "anticipate" it on the translation.
                Vector3f rotated = fr.dynamx.utils.maths.DynamXGeometry.rotateVectorByQuaternion(translation, wheel.getSuspensionAxis().inverse());
                translation.set(rotated);
            }
        }

        @Override
        public void render(BaseRenderContext.EntityRenderContext context, A packInfo, Matrix4f parentTransform) {
            WheelsModule wheelsModule = getWheelsModule(context);
            boolean hasWheelsModule = wheelsModule != null;
            if (hasWheelsModule && wheelsModule.getWheelsStates()[getId()] == WheelsModule.WheelState.REMOVED) {
                return;
            }
            PartWheelInfo info = hasWheelsModule ? wheelsModule.getWheelInfo((byte) getId()) : getDefaultWheelInfo();
            if (info == null || (!info.isModelValid() && getRimObjectName() == null)) {
                return;
            }

            transformToRotationPoint(parentTransform);

            PoseStack pose = context.getPoseStack();
            if (pose == null) return;
            pose.pushPose();

            if (translation != null) {
                pose.translate(translation.x, translation.y, translation.z);
            }

            if (hasWheelsModule) {
                applyWheelRotationPose(pose, transform, context, wheelsModule);
            }

            if (isAutomaticPosition && translation != null) {
                pose.translate(-translation.x, -translation.y, -translation.z);
            }

            renderWheelGeometry(context, info, wheelsModule);

            pose.popPose();

            renderChildren(context, packInfo, transform);
        }
    }

    /** Spin the wheel around its X axis. Mirrors the left/right logic of the 1.12 renderer. */
    private void applyWheelRotationPose(PoseStack pose, Matrix4f transform, BaseRenderContext.EntityRenderContext context, WheelsModule wheelsModule) {
        boolean isSeparateModel = getRimObjectName() == null;
        int index = VehicleEntityProperties.getPropertyIndex(getId(), VehicleEntityProperties.EnumVisualProperties.ROTATION_ANGLE);
        float prev = wheelsModule.prevVisualProperties[index];
        float curr = wheelsModule.visualProperties[index];
        // Avoid wrap-around when the angle crosses 360°.
        if (prev - curr > 180) prev -= 360;
        if (prev - curr < -180) prev += 360;
        float angle = (prev + (curr - prev) * context.getPartialTicks()) * DynamXMath.TO_RADIAN;
        if (isRight() && isSeparateModel) {
            pose.mulPose(Axis.YP.rotation(FastMath.PI));
            transform.rotate(FastMath.PI, 0, 1, 0);
            pose.mulPose(Axis.XN.rotation(angle));
            transform.rotate(angle, -1, 0, 0);
        } else {
            pose.mulPose(Axis.XN.rotation(-angle));
            transform.rotate(-angle, -1, 0, 0);
        }
    }

    /** Renders either the separate wheel model or the rim/tire groups baked into the chassis model. */
    private void renderWheelGeometry(BaseRenderContext.EntityRenderContext context, PartWheelInfo info, @Nullable WheelsModule wheelsModule) {
        boolean isSeparateModel = getRimObjectName() == null;
        DxModelRenderer model = isSeparateModel
                ? DynamXContext.getDxModelRegistry().getModel(info.getModel())
                : context.getModel();
        if (model == null) return;
        byte wheelTextureId = wheelsModule != null
                ? wheelsModule.getWheelsTextureId()[getId()]
                : info.getIdForVariant(context.getModel() != null ? "Default" : "Default");
        if (isSeparateModel) {
            boolean flat = wheelsModule != null && wheelsModule.getWheelsStates()[getId()] == WheelsModule.WheelState.ADDED_FLATTENED;
            if (!flat || !model.renderGroup("rim", wheelTextureId, context.isUseVanillaRender())) {
                model.renderModel(wheelTextureId, context.isUseVanillaRender());
            }
        } else {
            if (getRimObjectName() != null) {
                context.getModel().renderGroup(getRimObjectName(), wheelTextureId, context.isUseVanillaRender());
            }
            if (getTireObjectName() != null
                    && (wheelsModule == null || wheelsModule.getWheelsStates()[getId()] != WheelsModule.WheelState.ADDED_FLATTENED)) {
                context.getModel().renderGroup(getTireObjectName(), wheelTextureId, context.isUseVanillaRender());
            }
        }
    }
}
