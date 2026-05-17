package fr.dynamx.common.network.sync;

import com.jme3.math.Vector3f;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SyncTarget;
import fr.dynamx.api.network.sync.SynchronizedEntityVariableRegistry;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.variables.SynchronizedEntityVariableSnapshot;
import fr.dynamx.utils.optimization.HashMapPool;
import fr.dynamx.utils.optimization.PooledHashMap;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Base net handler responsible for syncing a {@link PhysicsEntity} between client and server.
 */
public abstract class PhysicsEntitySynchronizer<T extends PhysicsEntity<?>> {
    private final ConcurrentHashMap<Integer, EntityVariable<?>> synchronizedVariables = new ConcurrentHashMap<>(3, 0.75f, 2);
    private final ConcurrentHashMap<Integer, SynchronizedEntityVariableSnapshot<?>> receivedVariables = new ConcurrentHashMap<>(3, 0.75f, 2);

    private SimulationHolder simulationHolder = getDefaultSimulationHolder();
    @Getter
    private Player simulationPlayerHolder;

    @Getter
    protected final T entity;

    public PhysicsEntitySynchronizer(T entity) {
        this.entity = entity;
    }

    public ConcurrentHashMap<Integer, EntityVariable<?>> getSynchronizedVariables() {
        return synchronizedVariables;
    }

    public EntityVariable<Object> tryGetVariable(int id) {
        EntityVariable<?> var = synchronizedVariables.get(id);
        if (var == null)
            throw new IllegalStateException("Variable " + id + " not registered on " + entity + ". Variable is " + SynchronizedEntityVariableRegistry.getSyncVarRegistry().inverse().get(id));
        return (EntityVariable<Object>) var;
    }

    public ConcurrentHashMap<Integer, SynchronizedEntityVariableSnapshot<?>> getReceivedVariables() {
        return receivedVariables;
    }

    public void registerVariable(Integer id, EntityVariable<?> variable) {
        if (synchronizedVariables.containsKey(id))
            throw new IllegalArgumentException("Duplicated synchronized entity variable " + id + " " + variable);
        synchronizedVariables.put(id, variable);
        receivedVariables.put(id, new SynchronizedEntityVariableSnapshot(variable.getSerializer(), variable.get()));
    }

    public void removeSynchronizedVariable(EntityVariable<?> variable) {
        if (!synchronizedVariables.containsValue(variable))
            throw new IllegalArgumentException("Variable isn't registered " + variable);
        synchronizedVariables.remove(SynchronizedEntityVariableRegistry.getSyncVarRegistry().get(variable.getName()));
    }

    public abstract void onPrePhysicsTick(Object profiler);

    public abstract void onPostPhysicsTick(Object profiler);

    public abstract void onPlayerStartControlling(Player player, boolean addControllers);

    public abstract void onPlayerStopControlling(Player player, boolean removeControllers);

    public void onWalkingPlayerChange(int playerId, Vector3f offset, byte face) {
    }

    public boolean doesOtherSideUsesPhysics() {
        return true;
    }

    public SimulationHolder getSimulationHolder() {
        return simulationHolder;
    }

    public SimulationHolder getDefaultSimulationHolder() {
        return SimulationHolder.SERVER;
    }

    public void setSimulationHolder(SimulationHolder simulationHolder, Player simulationPlayerHolder) {
        setSimulationHolder(simulationHolder, simulationPlayerHolder, SimulationHolder.UpdateContext.NORMAL);
    }

    public void setSimulationHolder(SimulationHolder simulationHolder, Player simulationPlayerHolder, SimulationHolder.UpdateContext changeContext) {
        this.simulationHolder = simulationHolder;
        this.simulationPlayerHolder = simulationPlayerHolder;
        if (changeContext != SimulationHolder.UpdateContext.ATTACHED_ENTITIES && entity.getJointsHandler() != null) {
            entity.getJointsHandler().setSimulationHolderOnJointedEntities(simulationHolder, simulationPlayerHolder);
        }
        if (entity instanceof ModularPhysicsEntity) {
            ((ModularPhysicsEntity<?>) entity).getModules().forEach(m -> m.onSetSimulationHolder(simulationHolder, simulationPlayerHolder, changeContext));
        }
    }

    public void resyncEntity(ServerPlayer target) {
        int simulationTimeClient = fr.dynamx.server.network.ServerPhysicsSyncManager.getTime(target);
        MessagePhysicsEntitySync<PhysicsEntity<?>> msg = new MessagePhysicsEntitySync<>(
                (PhysicsEntity<?>) entity, simulationTimeClient, synchronizedVariables, false);
        fr.dynamx.common.network.DynamXNetwork.sendTo(msg, target);
        if (entity instanceof fr.dynamx.api.entities.IModuleContainer.ISeatsContainer
                && ((fr.dynamx.api.entities.IModuleContainer.ISeatsContainer) entity).hasSeats()) {
            fr.dynamx.common.network.DynamXNetwork.sendTo(
                    new MessageSeatsSync((fr.dynamx.api.entities.IModuleContainer.ISeatsContainer) entity), target);
        }
        if (entity.getJointsHandler() != null) {
            entity.getJointsHandler().sync(target);
        }
    }

    public PooledHashMap<Integer, EntityVariable<?>> getVarsToSync(LogicalSide fromSide, SyncTarget target) {
        PooledHashMap<Integer, EntityVariable<?>> ret = HashMapPool.get();
        getSynchronizedVariables().forEach((i, s) -> {
            SyncTarget varTarget = s.getSyncTarget(simulationHolder, fromSide);
            if (target.isIncluded(varTarget)) {
                ret.put(i, s);
            }
        });
        return ret;
    }
}
