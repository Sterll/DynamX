package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.contentpack.type.ObjectCollisionsHelper;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2f;

import java.util.Collections;
import java.util.List;

/**
 * Door part of a modular vehicle.
 *
 * TODO port:1.20.1 - Original referenced (Phase 6/7):
 *   - fr.dynamx.api.entities.IModuleContainer / ModuleListBuilder
 *   - fr.dynamx.api.events.VehicleEntityEvent
 *   - fr.dynamx.api.dxmodel.DxModelPath
 *   - fr.dynamx.client.renders.scene.* (SceneNode/SimpleNode/BaseRenderContext/IRenderContext)
 *   - fr.dynamx.client.renders.model.renderer.ObjObjectRenderer
 *   - fr.dynamx.common.entities.{BaseVehicleEntity, ModularPhysicsEntity, PackPhysicsEntity}
 *   - fr.dynamx.common.entities.modules.DoorsModule
 *   - fr.dynamx.common.handlers.TaskScheduler
 *   - fr.dynamx.common.objloader.data.DxModelData
 *   - fr.dynamx.common.physics.utils.{RigidBodyTransform, SynchronizedRigidBodyTransform}
 *   - fr.dynamx.utils.physics.DynamXPhysicsHelper.EnumPhysicsAxis
 *   - fr.dynamx.utils.client.ClientDynamXUtils, DynamXUtils, IModelTextureVariants
 *   - net.minecraftforge.common.MinecraftForge (now net.minecraftforge.common.NeoForge)
 *   - org.joml.Vector2f (gone in 1.20.1, replaced by org.joml.Vector2f)
 *   The interact() / mount() / readPosition() / addModules() / createSceneGraph() / inner PartDoorNode
 *   are stubbed. axisToUse is typed as Object until DynamXPhysicsHelper lands.
 *
 * TODO port:1.20.1 - The first generic A of InteractivePart was BaseVehicleEntity; relaxed to Object.
 */
@Getter
@RegisteredSubInfoType(name = "door", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartDoor extends InteractivePart<Object, ModularVehicleInfo> implements IPhysicsPackInfo, IDrawablePart<ModularVehicleInfo>, IPartContainer<PartDoor> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER})
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("CarAttachPoint".equals(key))
            return new IPackFilePropertyFixer.FixResult("LocalCarAttachPoint", true);
        if ("DoorAttachPoint".equals(key))
            return new IPackFilePropertyFixer.FixResult("LocalDoorAttachPoint", true);
        if ("OpenLimit".equals(key))
            return new IPackFilePropertyFixer.FixResult("OpenedDoorAngleLimit", true);
        if ("CloseLimit".equals(key))
            return new IPackFilePropertyFixer.FixResult("ClosedDoorAngleLimit", true);
        if ("OpenMotor".equals(key))
            return new IPackFilePropertyFixer.FixResult("DoorOpenForce", true);
        if ("CloseMotor".equals(key))
            return new IPackFilePropertyFixer.FixResult("DoorCloseForce", true);
        if ("PartName".equals(key))
            return new IPackFilePropertyFixer.FixResult("ObjectName", false);
        return null;
    };
    @Getter
    @PackFileProperty(configNames = "ObjectName", required = false, defaultValue = "Suffix after 'Door_' in part name")
    protected String objectName;

    @PackFileProperty(configNames = "LocalCarAttachPoint", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, required = false)
    protected Vector3f carAttachPoint;
    @PackFileProperty(configNames = "LocalDoorAttachPoint", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, required = false)
    protected Vector3f doorAttachPoint = new Vector3f();
    @PackFileProperty(configNames = "AttachStrength", required = false, defaultValue = "400")
    protected int attachStrength = 400;

    /**
     * TODO port:1.20.1 - axisToUse was DynamXPhysicsHelper.EnumPhysicsAxis (fr.dynamx.utils.physics,
     *   not yet ported). Relaxed to Object until that enum is ported.
     */
    @PackFileProperty(configNames = "Axis", required = false, defaultValue = "Y_ROT")
    protected Object axisToUse;
    @PackFileProperty(configNames = "OpenedDoorAngleLimit", required = false, defaultValue = "0 0")
    protected Vector2f openLimit = new Vector2f();
    @PackFileProperty(configNames = "ClosedDoorAngleLimit", required = false, defaultValue = "0 0")
    protected Vector2f closeLimit = new Vector2f();
    @PackFileProperty(configNames = "DoorOpenForce", required = false, defaultValue = "1 200")
    protected Vector2f openMotor = new Vector2f(1, 200);
    @PackFileProperty(configNames = "DoorCloseForce", required = false, defaultValue = "-1.5 300")
    protected Vector2f closeMotor = new Vector2f(-1.5f, 300);

    @PackFileProperty(configNames = "AutoMountDelay", required = false, defaultValue = "40")
    protected byte mountDelay = (byte) 40;
    @PackFileProperty(configNames = "DoorCloseTime", required = false, defaultValue = "25")
    protected byte doorCloseTime = (byte) 25;

    @PackFileProperty(configNames = "Enabled", required = false, defaultValue = "true")
    protected boolean enabled = true;

    @PackFileProperty(configNames = "DoorOpenSound", required = false)
    protected String doorOpenSound;

    @PackFileProperty(configNames = "DoorCloseSound", required = false)
    protected String doorCloseSound;

    /**
     * True if the mounting animation is playing, use to prevent other interactions in the same time
     */
    @Setter
    protected boolean isPlayerMounting;

    protected ObjectCollisionsHelper collisionsHelper = new ObjectCollisionsHelper();

    /**
     * TODO port:1.20.1 - sceneGraph was SceneNode (Phase 7); relaxed to Object.
     */
    protected Object sceneGraph;

    public PartDoor(ModularVehicleInfo owner, String partName) {
        super(owner, partName, 0, 0);
        this.objectName = partName.replaceFirst("Door_", "");
    }

    @Override
    public Object getDebugOption() {
        // TODO port:1.20.1 - return DynamXDebugOptions.DOOR_ATTACH_POINTS once the debug package is ported.
        return null;
    }

    @Override
    public boolean interact(Object entity, Player player) {
        // TODO port:1.20.1 - Original implementation switched on DoorsModule state, called
        //   doors.spawnDoor / doors.switchDoorState / doors.setDoorState and scheduled a TaskScheduler
        //   task for mounting. All of those depend on Phase 6 modules.
        return false;
    }

    /**
     * TODO port:1.20.1 - mount() originally posted a VehicleEntityEvent.PlayerInteract to MinecraftForge.EVENT_BUS
     *   and delegated to PartEntitySeat.interact(). Both depend on Phase 6.
     */
    public void mount(Object vehicleEntity, PartEntitySeat seat, Player context) {
        // TODO port:1.20.1 - implement once Phase 6 (BaseVehicleEntity, VehicleEntityEvent) is ported.
    }

    /**
     * TODO port:1.20.1 - Original returned a PartEntitySeat matching a partName lookup on the vehicle's
     *   pack info. Signature kept; body still works since it only touches the pack info.
     */
    public PartEntitySeat getLinkedSeat(Object vehicleEntity) {
        // TODO port:1.20.1 - When BaseVehicleEntity is ported, replace with:
        //   return vehicleEntity.getPackInfo().getPartsByType(PartEntitySeat.class).stream()...
        return getOwner().getPartsByType(PartEntitySeat.class).stream()
                .filter(seat -> seat.getLinkedDoor() != null && seat.getLinkedDoor().equalsIgnoreCase(getPartName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * TODO port:1.20.1 - Original read position/scale/attach point from the obj model via DxModelData.
     *   The obj loader pipeline isn't ported yet; this method falls back to the configured values
     *   and logs an error when they are missing.
     */
    protected void readPosition(ResourceLocation model) {
        if (getPosition() == null) {
            position = new Vector3f();
        }
        if (getCarAttachPoint() == null) {
            carAttachPoint = new Vector3f();
        }
        if (getScale() == null || getScale().lengthSquared() == 0) {
            scale = new Vector3f(0.5f, 0.7f, 0.5f);
        }
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        readPosition(owner.getModel());
        super.appendTo(owner);
        getCarAttachPoint().multLocal(getScaleModifier(this.owner));
        getDoorAttachPoint().multLocal(getScaleModifier(this.owner));
        MutableBoundingBox box = new MutableBoundingBox(getScale()).offset(getPosition());
        collisionsHelper.addCollisionShape(new IShapeInfo() {
            @Override
            public Vector3f getPosition() {
                return PartDoor.this.getPosition();
            }

            @Override
            public Vector3f getSize() {
                return getScale();
            }

            @Override
            public MutableBoundingBox getBoundingBox() {
                return box;
            }
        });
        // TODO port:1.20.1 - collisionsHelper.loadCollisions originally took a DxModelPath; now relaxed to Object.
        collisionsHelper.loadCollisions(this, null, getObjectName(), new Vector3f(), 0, owner.isUseComplexCollisions(), owner.getScaleModifier(), ObjectCollisionsHelper.CollisionType.PROP);
    }

    @Override
    public void addModules(Object entity, Object modules) {
        // TODO port:1.20.1 - Original:
        //   if (!modules.hasModuleOfClass(DoorsModule.class))
        //       modules.add(new DoorsModule((BaseVehicleEntity<?>) entity));
        //   DoorsModule lives in Phase 6.
    }

    @Override
    public Vector3f getCenterOfMass() {
        return new Vector3f();
    }

    /**
     * TODO port:1.20.1 - Original returned ItemStack.EMPTY; kept.
     */
    public ItemStack getPickedResult(int metadata) {
        return ItemStack.EMPTY;
    }

    @Override
    public float getAngularDamping() {
        return 0;
    }

    @Override
    public float getLinearDamping() {
        return 0;
    }

    @Override
    public float getRenderDistanceSquared() {
        return owner.getRenderDistanceSquared();
    }

    @Override
    public ResourceLocation getHudCursorTexture() {
        return new ResourceLocation(DynamXConstants.ID, "textures/door.png");
    }

    @Override
    public String getName() {
        return getPartName();
    }

    @Override
    public String getFullName() {
        return super.getFullName();
    }

    @Override
    public void getBox(MutableBoundingBox out) {
        out.setTo(new MutableBoundingBox(getScale()));
    }

    @Override
    public Vector3f getScaleModifier() {
        return getScaleModifier(owner);
    }

    @Override
    public List<BasePart<PartDoor>> getAllParts() {
        return Collections.emptyList();
    }

    @Override
    public void addPart(BasePart<PartDoor> partDoorBasePart) {
        throw new IllegalStateException("Cannot add part to a door");
    }

    @Override
    public void addSubProperty(ISubInfoType<PartDoor> property) {
        throw new IllegalStateException("Cannot add sub property to a door");
    }

    @Override
    public List<ISubInfoType<PartDoor>> getSubProperties() {
        return Collections.emptyList();
    }

    @Override
    public Object getSceneGraph() {
        if (sceneGraph == null) {
            sceneGraph = createSceneGraph(owner.getScaleModifier(), null);
        }
        return sceneGraph;
    }

    @Override
    public String getNodeName() {
        return getPartName();
    }

    @Override
    public Object createSceneGraph(Vector3f modelScale, List<Object> childGraph) {
        // TODO port:1.20.1 - Original returned new PartDoorNode<>(this, modelScale, (List) childGraph).
        return null;
    }

    @Override
    public ResourceLocation getModel() {
        return getOwner().getModel();
    }

    /**
     * TODO port:1.20.1 - Original returned IModelTextureVariantsSupplier.IModelTextureVariants
     *   for an ObjObjectRenderer (Phase 7); relaxed to Object.
     */
    public Object getTextureVariantsFor(Object objObjectRenderer) {
        return null;
    }

    public boolean hasTextureVariants() {
        return false;
    }

    public byte getMaxVariantId() {
        return 1;
    }
}
