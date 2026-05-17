package fr.dynamx.common.network.sync.variables;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.client.network.ClientPhysicsEntitySynchronizer;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.DynamXNetwork;
import fr.dynamx.common.network.packets.MessageForcePlayerPos;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.utils.debug.SyncHelper;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.Callable;

/**
 * Synchronized variable holding the physics position/rotation/velocity of a {@link PhysicsEntity}.
 */
public class EntityPosVariable extends ListeningEntityVariable<EntityPosVariable.EntityPositionData> {
    public static int CRITIC1 = 3, CRITIC1warn = 100, CRITIC2 = 400, CRITIC3 = 50;

    public EntityPosVariable(PhysicsEntity<?> entity) {
        super((var, entityPositionData) -> {
            if (entity.getSynchronizer().getSimulationHolder().isSinglePlayer()) {
                if (!entity.level().isClientSide) {
                    double dx = entityPositionData.position.x - entity.physicsPosition.x;
                    double dy = entityPositionData.position.y - entity.physicsPosition.y;
                    double dz = entityPositionData.position.z - entity.physicsPosition.z;
                    entity.setDeltaMovement(new Vec3(dx, dy, dz));
                    double x = entity.physicsPosition.x + dx;
                    double y = entity.physicsPosition.y + dy;
                    double z = entity.physicsPosition.z + dz;
                    entity.physicsPosition.set((float) x, (float) y, (float) z);
                    entity.physicsRotation.set(entityPositionData.rotation);
                } else {
                    DynamXMain.log.error("Incorrect simulation holder in client set pos value : " + entity.getSynchronizer().getSimulationHolder());
                }
            } else {
                int ignoreFor = 0;
                if (ignoreFor <= 0) {
                    Vector3f pos = entityPositionData.position;
                    float delta = entity.physicsPosition.subtract(pos).length();
                    if (delta > CRITIC1) {
                        Player controllingPlayer = entity.getSynchronizer().getSimulationPlayerHolder();
                        boolean isControllingPlayerRidingThisEntity = controllingPlayer == entity.getControllingPassenger();
                        if (delta > CRITIC1warn)
                            DynamXMain.log.warn("Physics entity " + entity + " is moving too quickly (ridden by " + entity.getControllingPassenger() + ", simulated by " + controllingPlayer + ") !");
                        if (delta > CRITIC2 && controllingPlayer instanceof ServerPlayer && isControllingPlayerRidingThisEntity) {
                            ((ServerPlayer) controllingPlayer).connection.disconnect(Component.literal("Invalid physics entity move packet"));
                        } else if (controllingPlayer instanceof ServerPlayer || entity.level().isClientSide) {
                            if (delta > CRITIC3 && !entity.level().isClientSide && isControllingPlayerRidingThisEntity) {
                                DynamXMain.log.error(entity + " doing resync !!!");
                                DynamXNetwork.sendTo(new MessageForcePlayerPos(entity, entity.physicsPosition, entity.physicsRotation,
                                                entity.physicsHandler.getLinearVelocity(), entity.physicsHandler.getAngularVelocity()),
                                        (ServerPlayer) controllingPlayer);
                            } else
                                entity.physicsHandler.updatePhysicsState(pos, entityPositionData.rotation, entityPositionData.linearVel, entityPositionData.rotationalVel);
                        } else
                            DynamXMain.log.error(entity + " lost his player for sync. ");
                    } else if (entityPositionData.isBodyActive()) {
                        entity.physicsHandler.updatePhysicsStateFromNet(pos, entityPositionData.rotation, entityPositionData.linearVel, entityPositionData.rotationalVel);
                    }
                }
                if (entity.getSynchronizer() instanceof ClientPhysicsEntitySynchronizer) {
                    ((ClientPhysicsEntitySynchronizer<?>) entity.getSynchronizer()).setServerPos(entityPositionData.position);
                    ((ClientPhysicsEntitySynchronizer<?>) entity.getSynchronizer()).setServerRotation(entityPositionData.rotation);
                }
            }
        }, SynchronizationRules.PHYSICS_TO_SPECTATORS, new Callable<EntityPositionData>() {
            private EntityPositionData positionData;

            @Override
            public EntityPositionData call() {
                AbstractEntityPhysicsHandler<?, ?> physicsHandler = entity.physicsHandler;
                if (physicsHandler == null) return positionData;
                boolean changed = entity.tickCount % (physicsHandler.isBodyActive() ? 13 : 20) == 0;
                Vector3f pos = entity.physicsPosition;
                if (positionData == null || positionData.bodyActive != physicsHandler.isBodyActive()) {
                    changed = true;
                } else if (SyncHelper.different(pos.x, positionData.position.x) || SyncHelper.different(pos.y, positionData.position.y) || SyncHelper.different(pos.z, positionData.position.z)) {
                    changed = true;
                } else if (SyncHelper.different(entity.physicsRotation.getX(), positionData.rotation.getX()) || SyncHelper.different(entity.physicsRotation.getY(), positionData.rotation.getY()) ||
                        SyncHelper.different(entity.physicsRotation.getZ(), positionData.rotation.getZ()) || SyncHelper.different(entity.physicsRotation.getW(), positionData.rotation.getW())) {
                    changed = true;
                }
                if (changed) {
                    positionData = new EntityPositionData(physicsHandler);
                    entity.synchronizedPosition.setChanged(true);
                }
                return positionData;
            }
        });
        this.set(new EntityPositionData(false, entity.physicsPosition, entity.physicsRotation));
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

        private EntityPositionData(AbstractEntityPhysicsHandler<?, ?> physicsHandler) {
            bodyActive = physicsHandler.isBodyActive();
            position.set(physicsHandler.getHandledEntity().physicsPosition);
            rotation.set(physicsHandler.getHandledEntity().physicsRotation);
            linearVel.set(physicsHandler.getLinearVelocity());
            rotationalVel.set(physicsHandler.getAngularVelocity());
        }

        public EntityPositionData(boolean bodyActive, Vector3f position, Quaternion rotation) {
            this.bodyActive = bodyActive;
            this.position.set(position);
            this.rotation.set(rotation);
        }
    }

    public void onTeleported(PhysicsEntity<?> entity, Vector3f newPos) {
        if (entity.getControllingPassenger() instanceof ServerPlayer) {
            DynamXNetwork.sendTo(new MessageForcePlayerPos(entity, newPos, entity.physicsRotation,
                            entity.physicsHandler.getLinearVelocity(), entity.physicsHandler.getAngularVelocity()),
                    (ServerPlayer) entity.getControllingPassenger());
        }
    }
}
