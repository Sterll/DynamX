package fr.dynamx.common.contentpack.parts;

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
 * TODO port:1.20.1 - The original PartHandleNode inner class referenced fr.dynamx.client.renders.scene.*
 *   (SceneNode, SimpleNode, BaseRenderContext, IRenderContext), fr.dynamx.common.entities.modules.engines.HelicopterEngineModule
 *   and net.minecraft.client.renderer.GlStateManager (removed in 1.20.1). All live in Phase 6/7.
 *   createSceneGraph now returns null until those packages are ported.
 */
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
    public Object createSceneGraph(Vector3f modelScale, List<Object> childGraph) {
        // TODO port:1.20.1 - Original returned new PartHandleNode<>(this, modelScale, (List) childGraph).
        //   Phase 7 (SceneNode/SimpleNode/BaseRenderContext) is not yet ported.
        return null;
    }
}
