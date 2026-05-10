package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * TODO port:1.20.1 - Original referenced (Phase 6/7):
 *   - fr.dynamx.api.entities.VehicleEntityProperties
 *   - fr.dynamx.api.entities.modules.ModuleListBuilder
 *   - fr.dynamx.client.renders.scene.* (SceneNode/SimpleNode/BaseRenderContext/IRenderContext)
 *   - fr.dynamx.client.renders.model.renderer.{DxModelRenderer, ObjModelRenderer}
 *   - fr.dynamx.common.DynamXContext / DxModelData / DynamXUtils.readPart{Position,Rotation,Scale}
 *   - fr.dynamx.common.entities.{BaseVehicleEntity, ModularPhysicsEntity, PackPhysicsEntity}
 *   - fr.dynamx.common.entities.modules.WheelsModule
 *   - fr.dynamx.common.physics.entities.BaseWheeledVehiclePhysicsHandler
 *   - GlStateManager / RenderGlobal (removed in 1.20.1)
 *   The renderWheel()/applyWheelRotation()/printFullRenderInsights()/createSceneGraph()/inner
 *   PartBaseWheelNode/PartAttachedWheelNode are stubbed. The first generic A of InteractivePart
 *   was BaseVehicleEntity; relaxed to Object.
 *
 * TODO port:1.20.1 - AxisAlignedBB -> net.minecraft.world.phys.AABB.
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
    public Object createSceneGraph(Vector3f modelScale, List<Object> childGraph) {
        // TODO port:1.20.1 - Original returned new PartBaseWheelNode<>(this, modelScale, ...) and
        //   optionally wrapped it with a PartAttachedWheelNode child. Both nodes live in Phase 7.
        return null;
    }

    @Override
    public String getNodeName() {
        return getPartName();
    }

    @Override
    public void addModules(Object entity, Object modules) {
        // TODO port:1.20.1 - Original:
        //   if (!(entity instanceof BaseVehicleEntity)) throw ...
        //   if (!modules.hasModuleOfClass(WheelsModule.class))
        //       modules.add(new WheelsModule((BaseVehicleEntity<? extends BaseWheeledVehiclePhysicsHandler<?>>) entity));
        //   Both depend on Phase 6.
    }

    /**
     * TODO port:1.20.1 - Original printed render insights using DxModelRegistry / ObjModelRenderer
     *   (Phase 7). Body removed; method kept as a no-op to preserve the public API for diagnostic call sites.
     */
    public void printFullRenderInsights(Object entity, boolean modelInsights) {
        System.out.println("=-=-=-=-=");
        System.out.println("Wheel " + getName() + ", id=" + getId() + " render infos: (stubbed for 1.20.1 port)");
        System.out.println("Default info: " + getDefaultWheelInfo());
        System.out.println("rim object: " + getRimObjectName());
        System.out.println("tire object: " + getTireObjectName());
        System.out.println("mud guard object: " + getMudGuardObjectName());
        System.out.println("=-=-=-=-=");
    }
}
