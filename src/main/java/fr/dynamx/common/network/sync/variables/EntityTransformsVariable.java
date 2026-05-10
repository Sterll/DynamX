package fr.dynamx.common.network.sync.variables;

import fr.dynamx.api.network.sync.AttachedBodySynchronizer;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.common.entities.PhysicsEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Synchronizes the transforms of bodies attached to a physics entity (joints, props, etc.).
 */
// TODO port:1.20.1 - The legacy body referenced RigidBodyTransform, SynchronizedRigidBodyTransform,
// AttachedBodySynchronizer (already ported as marker), entity.level().isClientSide, SyncHelper.different.
// RigidBodyTransform is part of Phase 2 (physics utils) which may or may not be fully wired here.
// Receive callback + value-updater are stubbed; restore in Phase 5b/7.
public class EntityTransformsVariable extends ListeningEntityVariable<Map<Byte, Object>> {
    public EntityTransformsVariable(PhysicsEntity<?> entity, AttachedBodySynchronizer synchronizer) {
        super((var, transforms) -> {
            // TODO port:1.20.1 - Re-port both solo-mode write-back and MP physics-receiver branches.
        }, SynchronizationRules.PHYSICS_TO_SPECTATORS, new Callable<Map<Byte, Object>>() {
            @Override
            public Map<Byte, Object> call() {
                return new HashMap<>();
            }
        });
        this.set(new HashMap<>());
    }
}
