package fr.dynamx.common.physics.entities;

import com.jme3.bullet.collision.Activation;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.common.DynamX;
import fr.dynamx.common.entities.PhysicsEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Physics handler of {@link PhysicsEntity} using rigid bodies <br>
 * The physics handler is the bridge between the minecraft entity and the physics engine
 *
 * @param <T> The entity type
 */
public abstract class EntityPhysicsHandler<T extends PhysicsEntity<?>> extends AbstractEntityPhysicsHandler<T, PhysicsRigidBody> {
    private final Vector3f linearVel = new Vector3f();
    private final Vector3f rotationalVel = new Vector3f();

    public EntityPhysicsHandler(T entity) {
        super(entity);
    }

    @Override
    public void update() {
        super.update();
        if(collisionObject.getActivationState() == Activation.error){
            // TODO port:1.20.1 - DynamXMain.log replaced by DynamX.LOGGER; getName() now returns Component
            DynamX.LOGGER.error("Fatal error on " + handledEntity.getName().getString());
            return;
        }
        getCollisionObject().getLinearVelocity(linearVel);
        getCollisionObject().getAngularVelocity(rotationalVel);

    }

    @Override
    public Vector3f getLinearVelocity() {
        return linearVel;
    }

    @Override
    public Vector3f getAngularVelocity() {
        return rotationalVel;
    }

    @Override
    public void setPhysicsPosition(Vector3f position) {
        handledEntity.physicsPosition.set(position);
        collisionObject.setPhysicsLocation(position);
    }

    @Override
    public void setPhysicsRotation(Quaternion rotation) {
        handledEntity.physicsRotation.set(rotation);
        collisionObject.setPhysicsRotation(rotation);
    }

    @Override
    public void setLinearVelocity(Vector3f velocity) {
        this.linearVel.set(velocity);
        collisionObject.setLinearVelocity(velocity);
    }

    @Override
    public void setAngularVelocity(Vector3f velocity) {
        this.rotationalVel.set(velocity);
        collisionObject.setAngularVelocity(velocity);
    }

    @Override
    public void applyForce(Vector3f force, Vector3f at) {
        //if(getLinearVelocity().length() > 0.005f)
            getCollisionObject().applyForce(force, at);
    }

    @Override
    public void applyTorque(Vector3f force) {
        //if(getLinearVelocity().length() > 0.005f)
            getCollisionObject().applyTorque(force);
    }

    @Override
    public void applyImpulse(Vector3f at, Vector3f force) {
        getCollisionObject().applyImpulse(force, at);
    }

    @Override
    public void applyTorqueImpulse(Vector3f force) {
        getCollisionObject().applyTorqueImpulse(force);
    }

    @Override
    public void setFreezePhysics(boolean freeze) {
        getCollisionObject().setKinematic(freeze);
    }

    /**
     * @return The water level at the entity's position, or {@link Float#MIN_VALUE} if the entity is not in water
     */
    public float getWaterLevel() {
        PhysicsEntity<?> entity = getHandledEntity();
        // search water downwards, two blocks from the entity
        for (int offset = 2; offset > -2; offset--) {
            // TODO port:1.20.1 - BlockPos(double,double,double) ctor removed; use containing()
            BlockPos blockPos = BlockPos.containing(entity.physicsPosition.x, entity.physicsPosition.y + offset, entity.physicsPosition.z);
            // TODO port:1.20.1 - Material removed; check fluid state instead
            if (entity.level().getBlockState(blockPos).getFluidState().is(FluidTags.WATER)) {
                // TODO port:1.20.1 - getBoundingBox(world,pos) removed; use getShape().bounds()
                VoxelShape shape = entity.level().getBlockState(blockPos).getShape(entity.level(), blockPos);
                AABB boundingBox = shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds();
                return (float) boundingBox.move(blockPos.getX(), blockPos.getY(), blockPos.getZ()).maxY - 0.125F + 0.5f;
            }
        }
        return Float.MIN_VALUE;
    }
}
