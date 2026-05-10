package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Info of the steering wheel of a {@link ModularVehicleInfo}
 *
 * TODO port:1.20.1 - The original SteeringWheelNode inner class referenced
 *   fr.dynamx.client.renders.scene.* (SceneNode/SimpleNode/BaseRenderContext/IRenderContext),
 *   fr.dynamx.client.renders.model.renderer.DxModelRenderer, fr.dynamx.common.entities.modules.*,
 *   fr.dynamx.api.entities.VehicleEntityProperties, fr.dynamx.utils.debug.DynamXDebugOptions and
 *   net.minecraft.client.renderer.{GlStateManager, RenderGlobal}. Everything has been removed
 *   for the port; createSceneGraph now returns null until Phase 7 / Phase 6 are ported.
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
    public Object createSceneGraph(Vector3f modelScale, List<Object> childGraph) {
        // TODO port:1.20.1 - Re-enable once Phase 7 SceneNode/SimpleNode is ported. Original
        //   created a SteeringWheelNode<>(this, modelScale, (List) childGraph).
        return null;
    }

    @Override
    public String getNodeName() {
        return getPartName();
    }

    @Override
    public String getName() {
        return "Steering wheel named " + getPartName() + " in " + getOwner().getName();
    }
}
