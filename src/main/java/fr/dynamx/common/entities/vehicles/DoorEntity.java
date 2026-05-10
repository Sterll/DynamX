package fr.dynamx.common.entities.vehicles;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.parts.PartDoor;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.physics.entities.PackEntityPhysicsHandler;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.EntityJointsHandler;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.RemovalReason;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

/**
 * Detachable vehicle door entity with its own physics body.
 */
// TODO port:1.20.1 - DynamXObjectLoaders/PartDoor/PackEntityPhysicsHandler/EntityJointsHandler forward (Phase 4b/7).
public class DoorEntity<T extends PackEntityPhysicsHandler<PartDoor, ?>> extends PackPhysicsEntity<T, PartDoor> {
    private static final EntityDataAccessor<Integer> VEHICLE_ID = SynchedEntityData.defineId(DoorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DOOR_ID = SynchedEntityData.defineId(DoorEntity.class, EntityDataSerializers.BYTE);
    int timer = -1;
    private BaseVehicleEntity<?> vehicleEntity;
    @Getter
    private DoorsModule doorAttachModule;

    public DoorEntity(EntityType<? extends DoorEntity<?>> type, Level level) {
        super(type, level);
    }

    // TODO port:1.20.1 - Legacy (Level) constructor.
    public DoorEntity(Level level) {
        super(level);
    }

    public DoorEntity(EntityType<? extends DoorEntity<?>> type, BaseVehicleEntity<?> vehicleEntity, Vector3f pos, float spawnAngle, byte doorID) {
        super(type, vehicleEntity.getInfoName(), vehicleEntity.level(), pos, spawnAngle, 0);
        setDoorID(doorID);
        setVehicleEntity(vehicleEntity);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.getEntityData().define(VEHICLE_ID, -1);
        this.getEntityData().define(DOOR_ID, (byte) -1);
    }

    @Override
    public PartDoor createInfo(String infoName) {
        return getVehicleEntity(level()) instanceof TrailerEntity
                ? DynamXObjectLoaders.TRAILERS.findInfo(infoName).getPartByTypeAndId(PartDoor.class, getDoorID())
                : DynamXObjectLoaders.WHEELED_VEHICLES.findInfo(infoName).getPartByTypeAndId(PartDoor.class, getDoorID());

    }

    @Override
    public T createPhysicsHandler() {
        return (T) new DoorPhysicsHandler(this);
    }

    @Override
    public void createModules(ModuleListBuilder modules) {
        if (vehicleEntity != null) {
            doorAttachModule = new DoorsModule(vehicleEntity);
            modules.add(doorAttachModule);
        }
        modules.add(new MovableModule(this));
        modules.add(jointsHandler = new EntityJointsHandler(this) {
            @Override
            public void onRemoveJoint(EntityJoint<?> joint) {
                super.onRemoveJoint(joint);
                if (vehicleEntity != null) {
                    //vehicleEntity.detachDoor(((DoorEntity<?>) getEntity()).getDoorID());
                    //getEntity().physicEntity.getRigidBody().removeFromIgnoreList(vehicleEntity.physicEntity.getRigidBody());
                }
            }
        });
    }

    @Override
    protected final void fireCreateModulesEvent(Dist side) {
        //Don't simplify the generic type, for fml
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.CreateModules<>(DoorEntity.class, this, (java.util.List) moduleList, side));
    }

    @Override
    public void initPhysicsEntity(boolean usePhysics) {
        super.initPhysicsEntity(usePhysics);
        BaseVehicleEntity<?> car = getVehicleEntity(level());
        if (car != null) {
            /*DynamXMain.physicsWorld.schedule(() -> doorJoint =  DoorsModule.createP2PJoint(car, this, -1));
            if (car.isDoorAttached(this)) {
                physicEntity.getRigidBody().addToIgnoreList(car.physicEntity.getRigidBody());
            }*/
        }
    }

    @Override
    public int getSyncTickRate() {
        return 2;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tagCompound) {
        super.addAdditionalSaveData(tagCompound);
        tagCompound.putInt("carID", getVehicleID());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tagCompound) {
        super.readAdditionalSaveData(tagCompound);
        setVehicleID(tagCompound.getInt("carID"));
    }

    @Override
    public List<MutableBoundingBox> getCollisionBoxes() {
        if (getPackInfo() == null || physicsPosition == null)
            return new ArrayList<>(0);
        List<MutableBoundingBox> list = new ArrayList<>();
        for (IShapeInfo partShape : getPackInfo().getCollisionsHelper().getShapes()) {
            list.add(new MutableBoundingBox(partShape.getBoundingBox()).offset(physicsPosition));
        }
        return list;
    }

    public BaseVehicleEntity<?> getVehicleEntity(Level world) {
        if (vehicleEntity == null) {
            if (getVehicleID() != -1) {
                Entity entity = world.getEntity(getVehicleID());
                if (entity instanceof BaseVehicleEntity) {
                    this.vehicleEntity = (BaseVehicleEntity<?>) entity;
                }
            }
        }
        return vehicleEntity;
    }

    @Override
    public void preUpdatePhysics(boolean simulatingPhysics) {
        super.preUpdatePhysics(simulatingPhysics);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (vehicleEntity != null) {
            /*if (vehicleEntity.getAttachedDoors() != null) {
                vehicleEntity.detachDoor(getDoorID());
            }*/
        }
    }


    public void setVehicleEntity(BaseVehicleEntity<?> vehicleEntity) {
        this.vehicleEntity = vehicleEntity;
        setVehicleID(vehicleEntity.getId());
    }

    public int getVehicleID() {
        return this.getEntityData().get(VEHICLE_ID);
    }

    private void setVehicleID(int name) {
        this.getEntityData().set(VEHICLE_ID, name);
    }

    public byte getDoorID() {
        return this.getEntityData().get(DOOR_ID);
    }

    private void setDoorID(byte id) {
        this.getEntityData().set(DOOR_ID, id);
    }

    public static class DoorPhysicsHandler<A extends DoorEntity<?>> extends PackEntityPhysicsHandler<PartDoor, A> {
        public DoorPhysicsHandler(A entity) {
            super(entity);
        }

        @Override
        public PhysicsRigidBody createShape(Vector3f position, Quaternion rotation, float spawnRotation) {
            return DynamXPhysicsHelper.fastCreateRigidBody(handledEntity, 50, new BoxCollisionShape(getHandledEntity().getPackInfo().getScale()), position, spawnRotation);
        }
    }
}
