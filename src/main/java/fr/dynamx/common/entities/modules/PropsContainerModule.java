package fr.dynamx.common.entities.modules;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.PartPropsContainer;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Sets the simulation holder of nearby physics entities (e.g. on a moving truck bed).
 */
// TODO port:1.20.1 - PartPropsContainer is forward-referenced (Phase 4b). DynamXContext/getNetwork stubbed.
public class PropsContainerModule implements IPhysicsModule<BaseVehiclePhysicsHandler<?>>, IPhysicsModule.IEntityUpdateListener, IPackInfoReloadListener {
    //TODO NEW SYNC IMPROVE THIS
    private final BaseVehicleEntity<?> entity;
    private final List<PartPropsContainer> containers;
    private final List<PhysicsEntity<?>> modifiedEntitiesCache = new ArrayList<>();

    public PropsContainerModule(BaseVehicleEntity<?> entity) {
        this.entity = entity;
        this.containers = entity.getPackInfo().getPartsByType(PartPropsContainer.class);
    }

    @Override
    public void onPackInfosReloaded() {
        modifiedEntitiesCache.forEach(e -> e.getSynchronizer().setSimulationHolder(e.getSynchronizer().getDefaultSimulationHolder(), null, SimulationHolder.UpdateContext.PROPS_CONTAINER_UPDATE));
        containers.clear();
        containers.addAll(entity.getPackInfo().getPartsByType(PartPropsContainer.class));
        onSetSimulationHolder(entity.getSynchronizer().getSimulationHolder(), null, SimulationHolder.UpdateContext.NORMAL);
    }

    @Override
    public void updateEntity() {
        if (entity.tickCount % 20 == 0 && !modifiedEntitiesCache.isEmpty()) {
            modifiedEntitiesCache.removeIf(e -> {
                if (e.distanceTo(entity) > 10) {
                    System.out.println("[DEV] Remove " + e + " : far from " + entity);
                    e.getSynchronizer().setSimulationHolder(e.getSynchronizer().getDefaultSimulationHolder(), null, SimulationHolder.UpdateContext.PROPS_CONTAINER_UPDATE);
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public void onSetSimulationHolder(SimulationHolder simulationHolder, Player simulationPlayerHolder, SimulationHolder.UpdateContext changeContext) {
        modifiedEntitiesCache.forEach(e -> e.getSynchronizer().setSimulationHolder(e.getSynchronizer().getDefaultSimulationHolder(), null, SimulationHolder.UpdateContext.PROPS_CONTAINER_UPDATE));
        modifiedEntitiesCache.clear();
        for (PartPropsContainer container : containers) {
            Vector3f pos = DynamXGeometry.rotateVectorByQuaternion(container.getPosition(), entity.physicsRotation);
            MutableBoundingBox rotatedSize = DynamXContext.getCollisionHandler().rotateBB(Vector3fPool.get(0, 0, 0), container.getBoundingBox(), entity.physicsRotation);
            rotatedSize = rotatedSize.offset(pos);
            rotatedSize = rotatedSize.offset(entity.physicsPosition);
            // TODO port:1.20.1 - getEntitiesWithinAABB -> getEntitiesOfClass; MutableBoundingBox#toBB still
            // returns the legacy AxisAlignedBB until the optimization helper is fully ported. The call below
            // matches the legacy semantic and will compile once MutableBoundingBox#toBB is migrated to AABB.
            List<PhysicsEntity> entityList = entity.level().getEntitiesOfClass(PhysicsEntity.class, rotatedSize.toBB(), ent -> ent != entity);
            if (!entityList.isEmpty()) {
                System.out.println("[DEV] Found " + entityList.size() + " to set sim holder " + simulationHolder + " from " + entity);
                for (PhysicsEntity<?> ent : entityList) {
                    System.out.println("[DEV] Set on " + ent);
                    ent.getSynchronizer().setSimulationHolder(simulationHolder, simulationPlayerHolder, SimulationHolder.UpdateContext.PROPS_CONTAINER_UPDATE);
                    modifiedEntitiesCache.add(ent);
                }
            } else {
                System.out.println("[DEV] Found no entity to set sim holder " + simulationHolder + " from " + entity + " box is " + rotatedSize);
            }
        }
    }
}
