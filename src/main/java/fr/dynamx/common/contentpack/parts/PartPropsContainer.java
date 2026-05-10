package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.utils.optimization.MutableBoundingBox;

/**
 * TODO port:1.20.1 - Original referenced:
 *   - fr.dynamx.api.entities.modules.ModuleListBuilder (Phase 6)
 *   - fr.dynamx.common.entities.BaseVehicleEntity / PackPhysicsEntity (Phase 6)
 *   - fr.dynamx.common.entities.modules.PropsContainerModule (Phase 6)
 *   - fr.dynamx.utils.debug.DynamXDebugOptions
 *   All relaxed; addModules signature uses Object (inherited from ISubInfoType) and is stubbed.
 */
@RegisteredSubInfoType(name = "propscontainer", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER})
public class PartPropsContainer extends BasePart<ModularVehicleInfo> implements IShapeInfo {
    protected MutableBoundingBox box;

    public PartPropsContainer(ModularVehicleInfo owner, String partName) {
        super(owner, partName);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        super.appendTo(owner);
        Vector3f min = getPosition().subtract(getScale());
        Vector3f max = getPosition().add(getScale());
        this.box = new MutableBoundingBox(
                min.x, min.y, min.z,
                max.x, max.y, max.z);
    }

    @Override
    public void addModules(Object entity, Object modules) {
        // TODO port:1.20.1 - Original:
        //   if (!modules.hasModuleOfClass(PropsContainerModule.class))
        //       modules.add(new PropsContainerModule((BaseVehicleEntity<?>) entity));
        //   PropsContainerModule lives in Phase 6.
    }

    @Override
    public String getName() {
        return "PartPropsContainer named " + getPartName();
    }

    @Override
    public Vector3f getSize() {
        return getScale();
    }

    @Override
    public MutableBoundingBox getBoundingBox() {
        return box;
    }

    @Override
    public Object getDebugOption() {
        // TODO port:1.20.1 - return DynamXDebugOptions.PROPS_CONTAINERS once the debug package is ported.
        return null;
    }
}
