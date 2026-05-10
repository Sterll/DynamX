package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * TODO port:1.20.1 - The original PartRotorNode inner class referenced fr.dynamx.client.renders.scene.*
 *   (SceneNode, SimpleNode, BaseRenderContext, IRenderContext), fr.dynamx.client.renders.model.renderer.DxModelRenderer,
 *   fr.dynamx.common.entities.{ModularPhysicsEntity, BaseVehicleEntity, PackPhysicsEntity},
 *   fr.dynamx.common.entities.modules.{HelicopterRotorModule, engines.BoatPropellerModule, engines.CarEngineModule},
 *   fr.dynamx.common.entities.vehicles.HelicopterEntity and the removed GlStateManager / RenderGlobal classes.
 *   All live in Phase 6/7. createSceneGraph and addModules are stubbed.
 */
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

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        Quaternion rot = readPositionFromModel(owner.getModel(), getObjectName(), true, rotation == null);
        if (rot != null)
            rotation = rot;
        super.appendTo(owner);
    }

    @Override
    public void addModules(Object entity, Object modules) {
        // TODO port:1.20.1 - Original:
        //   if (!modules.hasModuleOfClass(HelicopterRotorModule.class) && entity instanceof HelicopterEntity)
        //       modules.add(new HelicopterRotorModule((BaseVehicleEntity<?>) entity));
        //   HelicopterRotorModule / HelicopterEntity live in Phase 6.
    }

    public PartRotor(ModularVehicleInfo owner, String partName) {
        super(owner, partName);
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
    public Object createSceneGraph(Vector3f modelScale, List<Object> childGraph) {
        // TODO port:1.20.1 - Original returned new PartRotorNode<>(this, modelScale, childGraph).
        return null;
    }

    public enum RotorType {
        PROPELLER,
        ALWAYS_ROTATING,
        ROTATING_WHEN_STARTED
    }
}
