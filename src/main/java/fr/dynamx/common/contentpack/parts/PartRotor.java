package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.client.renders.scene.node.SimpleNode;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.HelicopterRotorModule;
import fr.dynamx.common.entities.modules.engines.BoatPropellerModule;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.utils.maths.DynamXMath;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

@Getter
@Setter
@RegisteredSubInfoType(name = "rotor", registries = {SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BOATS}, strictName = false)
public class PartRotor extends BasePart<ModularVehicleInfo> implements IDrawablePart<ModularVehicleInfo> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.BOATS, SubInfoTypeRegistries.HELICOPTER})
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("PartName".equals(key))
            return new IPackFilePropertyFixer.FixResult("ObjectName", false);
        return null;
    };

    @PackFileProperty(configNames = "Rotation", required = false, defaultValue = "none")
    protected Quaternion rotation;
    @PackFileProperty(configNames = "RotationAxis", required = false, defaultValue = "0, 1, 0")
    protected Vector3f rotationAxis = new Vector3f(0, 1, 0);
    @PackFileProperty(configNames = "RotationSpeed", required = false, defaultValue = "0.0")
    protected float rotationSpeed = 15.0f;
    @PackFileProperty(configNames = "ObjectName")
    protected String objectName = "Rotor";

    public PartRotor(ModularVehicleInfo owner, String partName) {
        super(owner, partName);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        Quaternion rot = readPositionFromModel(owner.getModel(), getObjectName(), true, rotation == null);
        if (rot != null)
            rotation = rot;
        super.appendTo(owner);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void addModules(Object entity, Object modules) {
        if (entity instanceof HelicopterEntity && modules instanceof ModuleListBuilder) {
            ModuleListBuilder list = (ModuleListBuilder) modules;
            if (!list.hasModuleOfClass(HelicopterRotorModule.class)) {
                list.add(new HelicopterRotorModule((BaseVehicleEntity<?>) entity));
            }
        }
    }

    @Override
    public Object getDebugOption() {
        // TODO port:1.20.1 - return DynamXDebugOptions.ROTORS once the debug package is ported.
        return null;
    }

    @Override
    public String getName() {
        return "PartRotor named " + getPartName();
    }

    @Override
    public String getNodeName() {
        return getPartName();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addToSceneGraph(ModularVehicleInfo packInfo, Object sceneBuilder) {
        ((SceneBuilder<?, ModularVehicleInfo>) sceneBuilder).addNode(packInfo, this);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object createSceneGraph(Vector3f modelScale, List<Object> childGraph) {
        return new PartRotorNode<>(this, modelScale, (List) childGraph);
    }

    class PartRotorNode<A extends ModularVehicleInfo> extends SimpleNode<BaseRenderContext.EntityRenderContext, A> {
        public PartRotorNode(PartRotor part, Vector3f scale, List<SceneNode<BaseRenderContext.EntityRenderContext, A>> linkedChilds) {
            super(part.getPosition(), part.getRotation(), PartRotor.this.isAutomaticPosition, scale, linkedChilds);
        }

        @Override
        public void render(BaseRenderContext.EntityRenderContext context, A packInfo, Matrix4f parentTransform) {
            DxModelRenderer vehicleModel = context.getModel();
            if (vehicleModel == null || !vehicleModel.containsObjectOrNode(getObjectName()))
                return;
            transformToRotationPoint(parentTransform);

            ModularPhysicsEntity<?> entity = context.getEntity();
            float angle = 0;
            if (entity != null) {
                if (entity.hasModuleOfType(HelicopterRotorModule.class)) {
                    HelicopterRotorModule m = entity.getModuleByType(HelicopterRotorModule.class);
                    angle = (m.getCurAngle() + context.getPartialTicks() * m.getCurPower()) * getRotationSpeed();
                    transform.rotate(angle * DynamXMath.TO_RADIAN, getRotationAxis().x, getRotationAxis().y, getRotationAxis().z);
                } else if (entity.hasModuleOfType(BoatPropellerModule.class)) {
                    BoatPropellerModule m = entity.getModuleByType(BoatPropellerModule.class);
                    angle = (m.getBladeAngle() + context.getPartialTicks() * m.getRevs()) * getRotationSpeed();
                    transform.rotate(angle, getRotationAxis().x, getRotationAxis().y, getRotationAxis().z);
                }
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
                    Vector3f axis = getRotationAxis();
                    pose.mulPose(new Quaternionf().fromAxisAngleRad(axis.x, axis.y, axis.z, angle * DynamXMath.TO_RADIAN));
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

    public enum RotorType {
        PROPELLER,
        ALWAYS_ROTATING,
        ROTATING_WHEN_STARTED
    }
}
