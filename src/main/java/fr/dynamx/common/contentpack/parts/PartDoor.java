package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
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
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.client.renders.scene.node.SimpleNode;
import fr.dynamx.common.contentpack.type.ObjectCollisionsHelper;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.handlers.TaskScheduler;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.common.physics.utils.SynchronizedRigidBodyTransform;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraftforge.common.MinecraftForge;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
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
 *   - fr.dynamx.client.renders.model.renderer.ObjObjectRenderer
 *   - fr.dynamx.common.entities.{BaseVehicleEntity, PackPhysicsEntity}
 *   - fr.dynamx.common.handlers.TaskScheduler
 *   - fr.dynamx.common.objloader.data.DxModelData
 *   - fr.dynamx.utils.physics.DynamXPhysicsHelper.EnumPhysicsAxis
 *   - fr.dynamx.utils.client.ClientDynamXUtils, DynamXUtils, IModelTextureVariants
 *   - net.minecraftforge.common.MinecraftForge (now net.minecraftforge.common.NeoForge)
 *   The interact() / mount() / readPosition() / addModules() are stubbed.
 *   axisToUse is typed as Object until DynamXPhysicsHelper lands.
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

    @PackFileProperty(configNames = "Axis", required = false, defaultValue = "Y_ROT")
    protected fr.dynamx.utils.physics.DynamXPhysicsHelper.EnumPhysicsAxis axisToUse;
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

    protected fr.dynamx.client.renders.scene.node.SceneNode<?, ?> sceneGraph;

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
        if (!(entity instanceof IModuleContainer.IDoorContainer) || !(entity instanceof BaseVehicleEntity)) {
            return false;
        }
        BaseVehicleEntity<?> vehicle = (BaseVehicleEntity<?>) entity;
        DoorsModule doors = (DoorsModule) ((IModuleContainer.IDoorContainer) entity).getDoors();
        if (doors == null) return false;
        if (isEnabled() && !doors.isDoorAttached(getId())) {
            if (!vehicle.level().isClientSide) {
                doors.spawnDoor(this);
            }
        } else if (!isPlayerMounting()) {
            PartEntitySeat seat = getLinkedSeat(entity);
            if (player.isShiftKeyDown() || seat == null) {
                doors.switchDoorState(getId());
            } else {
                if (isEnabled()) {
                    if (doors.isDoorOpened(getId())) {
                        mount(entity, seat, player);
                        doors.setDoorState(getId(), DoorsModule.DoorState.CLOSING);
                        return true;
                    }
                    isPlayerMounting = true;
                    doors.setDoorState(getId(), DoorsModule.DoorState.OPENING);
                    TaskScheduler.schedule(new TaskScheduler.ScheduledTask(getMountDelay()) {
                        @Override
                        public void run() {
                            isPlayerMounting = false;
                            mount(entity, seat, player);
                            doors.setDoorState(getId(), DoorsModule.DoorState.CLOSING);
                        }
                    });
                } else {
                    mount(entity, seat, player);
                }
            }
        }
        return true;
    }

    public void mount(Object vehicleEntity, PartEntitySeat seat, Player context) {
        Vector3fPool.openPool();
        try {
            if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.PlayerInteract(context, vehicleEntity, seat))) {
                seat.interact(vehicleEntity, context);
            }
        } finally {
            Vector3fPool.closePool();
        }
    }

    public PartEntitySeat getLinkedSeat(Object vehicleEntity) {
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
        if (modules instanceof ModuleListBuilder && entity instanceof BaseVehicleEntity) {
            ModuleListBuilder list = (ModuleListBuilder) modules;
            if (!list.hasModuleOfClass(DoorsModule.class)) {
                list.add(new DoorsModule((BaseVehicleEntity<?>) entity));
            }
        }
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
    public fr.dynamx.client.renders.scene.node.SceneNode<?, ?> getSceneGraph() {
        if (sceneGraph == null) {
            sceneGraph = (fr.dynamx.client.renders.scene.node.SceneNode<?, ?>) createSceneGraph(owner.getScaleModifier(), null);
        }
        return sceneGraph;
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
        return new PartDoorNode<>(this, modelScale, (List) childGraph);
    }

    @Override
    public ResourceLocation getModel() {
        return getOwner().getModel();
    }

    @Override
    public fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier.IModelTextureVariants getTextureVariantsFor(fr.dynamx.client.renders.model.renderer.ObjObjectRenderer objObjectRenderer) {
        return null;
    }

    public boolean hasTextureVariants() {
        return false;
    }

    public byte getMaxVariantId() {
        return 1;
    }

    class PartDoorNode<A extends ModularVehicleInfo> extends SimpleNode<BaseRenderContext.EntityRenderContext, A> {
        public PartDoorNode(PartDoor door, Vector3f scale, List<SceneNode<BaseRenderContext.EntityRenderContext, A>> linkedChilds) {
            super(door.getCarAttachPoint(), (Quaternion) null, PartDoor.this.isAutomaticPosition, scale, linkedChilds);
        }

        @Override
        public void render(BaseRenderContext.EntityRenderContext context, A packInfo, Matrix4f parentTransform) {
            if (context.getModel() == null) return;
            transform.set(parentTransform);

            ModularPhysicsEntity<?> entity = context.getEntity();
            DoorsModule module = entity != null ? entity.getModuleByType(DoorsModule.class) : null;
            byte doorId = (byte) getId();
            boolean physicsDriven = enabled && module != null
                    && module.getCurrentState(doorId) != null
                    && module.getCurrentState(doorId) != DoorsModule.DoorState.CLOSED
                    && module.getTransforms().containsKey(doorId);

            // Translation/rotation that will be pushed onto the PoseStack.
            float tx = 0, ty = 0, tz = 0;
            Quaternionf entityRotInv = null;
            Quaternionf physicsRot = null;

            if (!physicsDriven) {
                Vector3f pos = Vector3fPool.get().addLocal(translation != null ? translation : new Vector3f());
                pos.subtract(getDoorAttachPoint(), pos);
                tx = pos.x;
                ty = pos.y;
                tz = pos.z;
                transform.translate(tx, ty, tz);
            } else {
                float partialTicks = context.getPartialTicks();
                SynchronizedRigidBodyTransform sync = module.getTransforms().get(doorId);
                RigidBodyTransform rb = sync.getTransform();
                RigidBodyTransform prev = sync.getPrevTransform();
                Vector3f pos = Vector3fPool.get(prev.getPosition()).addLocal(
                        rb.getPosition().subtract(prev.getPosition(), Vector3fPool.get()).multLocal(partialTicks));

                entityRotInv = ClientDynamXUtils.computeInterpolatedJomlQuaternion(
                        entity.prevRenderRotation, entity.renderRotation, partialTicks, true);
                physicsRot = ClientDynamXUtils.computeInterpolatedJomlQuaternion(
                        prev.getRotation(), rb.getRotation(), partialTicks);

                float interpX = (float) (entity.xOld + (entity.getX() - entity.xOld) * partialTicks);
                float interpY = (float) (entity.yOld + (entity.getY() - entity.yOld) * partialTicks);
                float interpZ = (float) (entity.zOld + (entity.getZ() - entity.zOld) * partialTicks);
                tx = pos.x - interpX;
                ty = pos.y - interpY;
                tz = pos.z - interpZ;

                transform.rotate(entityRotInv);
                transform.translate(tx, ty, tz);
                transform.rotate(physicsRot);
            }
            transform.scale(scale.x, scale.y, scale.z);

            PoseStack pose = context.getPoseStack();
            if (pose != null) {
                pose.pushPose();
                if (physicsDriven) {
                    pose.mulPose(entityRotInv);
                    pose.translate(tx, ty, tz);
                    pose.mulPose(physicsRot);
                } else {
                    pose.translate(tx, ty, tz);
                }
                pose.scale(scale.x, scale.y, scale.z);
                if (isAutomaticPosition && translation != null) {
                    pose.translate(-translation.x / scale.x, -translation.y / scale.y, -translation.z / scale.z);
                }
                context.getModel().renderGroup(getObjectName(), context.getTextureId(), context.isUseVanillaRender());
                pose.popPose();
            }
            renderChildren(context, packInfo, transform);
        }
    }
}
