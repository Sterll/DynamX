package fr.dynamx.common.contentpack.parts;

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
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.IRenderContext;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.client.renders.scene.node.SimpleNode;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.modules.engines.HelicopterEngineModule;
import fr.dynamx.utils.maths.DynamXMath;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

@Getter
@Setter
@RegisteredSubInfoType(name = "handle", registries = {SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartHandle extends BasePart<ModularVehicleInfo> implements IDrawablePart<ModularVehicleInfo> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.HELICOPTER})
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("PartName".equals(key))
            return new IPackFilePropertyFixer.FixResult("ObjectName", false);
        return null;
    };

    @PackFileProperty(configNames = "ObjectName")
    protected String objectName = "handle";

    public PartHandle(ModularVehicleInfo owner, String partName) {
        super(owner, partName);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        readPositionFromModel(owner.getModel(), getObjectName(), false, false);
        super.appendTo(owner);
    }

    @Override
    public Object getDebugOption() {
        // TODO port:1.20.1 - return DynamXDebugOptions.HANDLES once the debug package is ported.
        return null;
    }

    @Override
    public String getName() {
        return "PartHandle named " + getPartName();
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
        return new PartHandleNode<>(this, modelScale, (List) childGraph);
    }

    class PartHandleNode<A extends ModularVehicleInfo> extends SimpleNode<BaseRenderContext.EntityRenderContext, A> {
        public PartHandleNode(PartHandle part, Vector3f scale, List<SceneNode<BaseRenderContext.EntityRenderContext, A>> linkedChilds) {
            super(part.getPosition(), (Quaternion) null, PartHandle.this.isAutomaticPosition, scale, linkedChilds);
        }

        @Override
        public void render(BaseRenderContext.EntityRenderContext context, A packInfo, Matrix4f parentTransform) {
            if (context.getModel() == null || !context.getModel().containsObjectOrNode(getObjectName()))
                return;
            transformToRotationPoint(parentTransform);

            float dx = 0, dy = 0;
            if (context.getEntity() != null && context.getEntity().hasModuleOfType(HelicopterEngineModule.class)) {
                HelicopterEngineModule engine = context.getEntity().getModuleByType(HelicopterEngineModule.class);
                dx = engine.getRollControls().get(0);
                dy = engine.getRollControls().get(1);
                transform.rotate(dx * DynamXMath.TO_RADIAN, 0, dx > 0 ? 0.5f : -0.5f, 0);
                transform.rotate(dy * DynamXMath.TO_RADIAN, dy > 0 ? 0.5f : -0.5f, 0, 0);
            }

            PoseStack pose = context.getPoseStack();
            if (pose != null) {
                pose.pushPose();
                if (translation != null) {
                    pose.translate(translation.x, translation.y, translation.z);
                }
                if (dx != 0) {
                    pose.mulPose(new Quaternionf().fromAxisAngleRad(0, dx > 0 ? 0.5f : -0.5f, 0, dx * DynamXMath.TO_RADIAN));
                }
                if (dy != 0) {
                    pose.mulPose(new Quaternionf().fromAxisAngleRad(dy > 0 ? 0.5f : -0.5f, 0, 0, dy * DynamXMath.TO_RADIAN));
                }
                if (isAutomaticPosition && translation != null) {
                    pose.translate(-translation.x, -translation.y, -translation.z);
                }
                context.getModel().renderGroup(getObjectName(), context.getTextureId(), context.isUseVanillaRender());
                pose.popPose();
            }
            renderChildren(context, packInfo, transform);
        }
    }
}
