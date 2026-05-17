package fr.dynamx.api.events;

import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.CollisionsHandler;
import lombok.Getter;
import net.minecraftforge.eventbus.api.Event;

public class PhysicsEvent extends Event {

    @Getter
    private final IPhysicsWorld physicsWorld;

    public PhysicsEvent(IPhysicsWorld physicsWorld) {
        this.physicsWorld = physicsWorld;
    }

    /**
     * Fired when an entity is added to the physics world.
     */
    public static class PhysicsEntityAdded extends PhysicsEvent {

        @Getter
        private final PhysicsEntity<?> physicsEntity;

        public PhysicsEntityAdded(PhysicsEntity<?> physicsEntity, IPhysicsWorld physicsWorld) {
            super(physicsWorld);
            this.physicsEntity = physicsEntity;
        }
    }

    /**
     * Fired when an entity is removed from the physics world.
     */
    public static class PhysicsEntityRemoved extends PhysicsEvent {

        @Getter
        private final PhysicsEntity<?> physicsEntity;

        public PhysicsEntityRemoved(PhysicsEntity<?> physicsEntity, IPhysicsWorld physicsWorld) {
            super(physicsWorld);
            this.physicsEntity = physicsEntity;
        }
    }

    /**
     * Called on physics world update
     */
    public static class StepSimulation extends PhysicsEvent {
        @Getter
        private final float deltaTime;

        public StepSimulation(IPhysicsWorld physicsWorld, float deltaTime) {
            super(physicsWorld);
            this.deltaTime = deltaTime;
        }
    }

    /**
     * Called after bullet's world initialization
     */
    public static class PhysicsWorldLoad extends PhysicsEvent {
        public PhysicsWorldLoad(IPhysicsWorld physicsWorld) {
            super(physicsWorld);
        }
    }

    /**
     * Called when a collision occurs between two objects.
     */
    public static class PhysicsCollision extends PhysicsEvent {
        @Getter
        private final BulletShapeType<?> object1, object2;

        @Getter
        private final CollisionsHandler.CollisionInfo collisionInfo;

        public PhysicsCollision(IPhysicsWorld world, BulletShapeType<?> object1, BulletShapeType<?> object2, CollisionsHandler.CollisionInfo collisionInfo) {
            super(world);
            this.object1 = object1;
            this.object2 = object2;
            this.collisionInfo = collisionInfo;
        }

        /**
         * Called when a collision occurs in the physics engine.
         */
        public static class Pre extends PhysicsCollision {

            public Pre(IPhysicsWorld world, BulletShapeType<?> object1, BulletShapeType<?> object2, CollisionsHandler.CollisionInfo collisionInfo) {
                super(world, object1, object2, collisionInfo);
            }
        }

    }
}
