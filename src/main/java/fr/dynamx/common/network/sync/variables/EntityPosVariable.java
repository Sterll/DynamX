package fr.dynamx.common.network.sync.variables;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.common.entities.PhysicsEntity;
import lombok.Getter;

import java.util.concurrent.Callable;

/**
 * Synchronized variable holding the physics position/rotation/velocity of a {@link PhysicsEntity}.
 */
// TODO port:1.20.1 - The legacy body referenced ClientPhysicsEntitySynchronizer, MessageForcePlayerPos,
// AbstractEntityPhysicsHandler, DynamXContext.getNetwork.sendToClient, ServerPlayer disconnect with a
// Component, SyncHelper.different, and entity#motion fields. Most of those are pending later phases —
// the receive callback is stubbed to a no-op, and the value-updater computes a fresh EntityPositionData
// every tick (no change detection). Phase 5b/8 must restore the real behaviour.
public class EntityPosVariable extends ListeningEntityVariable<EntityPosVariable.EntityPositionData> {
    public static int CRITIC1 = 3, CRITIC1warn = 100, CRITIC2 = 400, CRITIC3 = 50;

    public EntityPosVariable(PhysicsEntity<?> entity) {
        super((var, data) -> {
            // TODO port:1.20.1 - Re-port receive callback: solo-mode motion writeback, MP delta checks,
            // resync via MessageForcePlayerPos, ServerPlayer disconnect on excessive delta, etc.
        }, SynchronizationRules.PHYSICS_TO_SPECTATORS, new Callable<EntityPositionData>() {
            @Override
            public EntityPositionData call() {
                // TODO port:1.20.1 - Re-port change detection against entity.physicsPosition / physicsRotation
                // once those fields are exposed on PhysicsEntity.
                return new EntityPositionData(false, new Vector3f(), new Quaternion());
            }
        });
        this.set(new EntityPositionData(false, new Vector3f(), new Quaternion()));
    }

    public static class EntityPositionData {
        @Getter
        private final boolean bodyActive;
        @Getter
        private final Vector3f position = new Vector3f();
        @Getter
        private final Quaternion rotation = new Quaternion();
        @Getter
        private final Vector3f linearVel = new Vector3f();
        @Getter
        private final Vector3f rotationalVel = new Vector3f();

        public EntityPositionData(boolean bodyActive, Vector3f position, Quaternion rotation) {
            this.bodyActive = bodyActive;
            this.position.set(position);
            this.rotation.set(rotation);
        }
    }

    public void onTeleported(PhysicsEntity<?> entity, Vector3f newPos) {
        // TODO port:1.20.1 - Re-port: DynamXContext.getNetwork().sendToClient(new MessageForcePlayerPos(...)).
    }
}
