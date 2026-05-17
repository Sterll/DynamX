package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
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
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.entities.modules.engines.BoatPropellerModule;
import fr.dynamx.utils.maths.DynamXMath;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

/**
 * Info of the steering wheel of a {@link ModularVehicleInfo}
 */
@Getter
@Setter
@RegisteredSubInfoType(name = "steeringwheel", registries = SubInfoTypeRegistries.WHEELED_VEHICLES, strictName = false)
public class SteeringWheelInfo extends BasePart<ModularVehicleInfo> implements IDrawablePart<ModularVehicleInfo> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("PartName".equals(key))
            return new IPackFilePropertyFixer.FixResult("ObjectName", false);
        return null;
    };

    @PackFileProperty(configNames = "ObjectName", required = false, defaultValue = "SteeringWheel")
    protected String objectName = "SteeringWheel";
    @PackFileProperty(configNames = {"Rotation", "BaseRotation", "BaseRotationQuat"}, required = false, defaultValue = "From model", description = "SteeringWheelInfo.Rotation")
    protected Quaternion steeringWheelBaseRotation = null;

    public SteeringWheelInfo(ModularVehicleInfo owner, String partName) {
        super(owner, partName);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        Quaternion rot = readPositionFromModel(owner.getModel(), getObjectName(), true, steeringWheelBaseRotation == null);
        if (rot != null)
            steeringWheelBaseRotation = rot;
        super.appendTo(owner);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addToSceneGraph(ModularVehicleInfo packInfo, Object sceneBuilder) {
        ((SceneBuilder<?, ModularVehicleInfo>) sceneBuilder).addNode(packInfo, this);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object createSceneGraph(Vector3f modelScale, List<Object> childGraph) {
        return new SteeringWheelNode<>(this, modelScale, (List) childGraph);
    }

    @Override
    public String getNodeName() {
        return getPartName();
    }

    @Override
    public String getName() {
        return "Steering wheel named " + getPartName() + " in " + getOwner().getName();
    }

    class SteeringWheelNode<A extends ModularVehicleInfo> extends SimpleNode<BaseRenderContext.EntityRenderContext, A> {
        public SteeringWheelNode(SteeringWheelInfo part, Vector3f scale, List<SceneNode<BaseRenderContext.EntityRenderContext, A>> linkedChilds) {
            super(part.getPosition(), part.getSteeringWheelBaseRotation(), SteeringWheelInfo.this.isAutomaticPosition, scale, linkedChilds);
        }

        @Override
        public void render(BaseRenderContext.EntityRenderContext context, A packInfo, Matrix4f parentTransform) {
            DxModelRenderer vehicleModel = context.getModel();
            if (vehicleModel == null) return;
            transformToRotationPoint(parentTransform);

            float angle = 0;
            if (context.getEntity() != null && context.getEntity().hasModuleOfType(WheelsModule.class)) {
                int directingWheel = VehicleEntityProperties.getPropertyIndex(packInfo.getDirectingWheel(), VehicleEntityProperties.EnumVisualProperties.STEER_ANGLE);
                WheelsModule m = context.getEntity().getModuleByType(WheelsModule.class);
                if (m.visualProperties.length > directingWheel) {
                    angle = -(m.prevVisualProperties[directingWheel] + (m.visualProperties[directingWheel] - m.prevVisualProperties[directingWheel]) * context.getPartialTicks()) * DynamXMath.TO_RADIAN;
                    transform.rotate(angle, 0F, 0F, 1F);
                }
            } else if (context.getEntity() != null && context.getEntity().hasModuleOfType(BoatPropellerModule.class)) {
                BoatPropellerModule module = context.getEntity().getModuleByType(BoatPropellerModule.class);
                angle = module.getPrevPhysicsSteeringForce() + (module.getPhysicsSteeringForce() - module.getPrevPhysicsSteeringForce()) * context.getPartialTicks();
                angle = angle * 6;
                transform.rotate(angle, 0F, 0F, 1F);
            }

            PoseStack pose = context.getPoseStack();
            if (pose != null) {
                pose.pushPose();
                if (translation != null) {
                    pose.translate(translation.x, translation.y, translation.z);
                }
                if (rotation != null) {
                    pose.mulPose(rotation);
                }
                if (angle != 0) {
                    pose.mulPose(Axis.ZP.rotation(angle));
                }
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
                vehicleModel.renderGroup(getObjectName(), context.getTextureId(), context.isUseVanillaRender());
                pose.popPose();
            }
            renderChildren(context, packInfo, transform);
        }
    }
}
