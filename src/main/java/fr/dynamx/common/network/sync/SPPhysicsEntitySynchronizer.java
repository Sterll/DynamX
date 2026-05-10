package fr.dynamx.common.network.sync;

import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.network.sync.ClientEntityNetHandler;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SyncTarget;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.variables.SynchronizedEntityVariableSnapshot;
import fr.dynamx.utils.optimization.PooledHashMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.LogicalSide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Simplified network handler (no packets exchanged) for single-player games.
 */
public class SPPhysicsEntitySynchronizer<T extends PhysicsEntity<?>> extends PhysicsEntitySynchronizer<T> implements ClientEntityNetHandler {
    private final LogicalSide mySide;
    private final List<IVehicleController> controllers = new ArrayList<>();

    public SPPhysicsEntitySynchronizer(T entityIn, LogicalSide side) {
        super(entityIn);
        this.mySide = side;
    }

    /**
     * @return The matching entity on the other side, or {@code null} if not available.
     */
    public Entity getOtherSideEntity() {
        // TODO port:1.20.1 - Re-port via DynamXMain.proxy.getClientWorld()/getServerWorld() + getEntity(int).
        return null;
    }

    private void sendMyVars(SPPhysicsEntitySynchronizer<T> other, SyncTarget to) {
        PooledHashMap<Integer, EntityVariable<?>> varsToSync = getVarsToSync(mySide, to);
        ByteBuf buf = Unpooled.buffer();
        for (Map.Entry<Integer, EntityVariable<?>> entry : varsToSync.entrySet()) {
            Integer varId = entry.getKey();
            EntityVariable<?> sourceVar = entry.getValue();
            sourceVar.writeValue(buf, false);
            sourceVar.setChanged(false);
            SynchronizedEntityVariableSnapshot<?> targetVar = other.getReceivedVariables().get(varId);
            if (targetVar != null) {
                targetVar.read(buf);
            }
            buf.clear();
        }
        varsToSync.release();
    }

    @Override
    public void onPlayerStartControlling(Player player, boolean addControllers) {
        // TODO port:1.20.1 - physicsHandler.setForceActivation(true) + setSimulationHolder(DRIVER_SP).
        setSimulationHolder(SimulationHolder.DRIVER_SP, player);
        // TODO port:1.20.1 - Re-port controllers loop using BaseVehicleEntity#getModules#createNewController.
    }

    @Override
    public void onPlayerStopControlling(Player player, boolean removeControllers) {
        // TODO port:1.20.1 - physicsHandler.setForceActivation(false).
        setSimulationHolder(getDefaultSimulationHolder(), null);
        // TODO port:1.20.1 - clear controllers on client side.
    }

    @Override
    public void onPrePhysicsTick(Object profiler) {
        // TODO port:1.20.1 - update controllers + entity.prePhysicsUpdateWrapper once available.
    }

    @Override
    public void onPostPhysicsTick(Object profiler) {
        // TODO port:1.20.1 - entity.postUpdatePhysicsWrapper + cross-side sendMyVars.
    }

    public boolean isLocalPlayerDriving() {
        // TODO port:1.20.1 - entity.getControllingPassenger() == Minecraft.getInstance().player.
        return false;
    }

    @Override
    public boolean doesOtherSideUsesPhysics() {
        // TODO port:1.20.1 - !entity.level().isClientSide once entity.level is wired.
        return !mySide.equals(LogicalSide.CLIENT);
    }

    @Override
    public SimulationHolder getDefaultSimulationHolder() {
        return SimulationHolder.SERVER_SP;
    }

    @Override
    public List<IVehicleController> getControllers() {
        return controllers;
    }
}
