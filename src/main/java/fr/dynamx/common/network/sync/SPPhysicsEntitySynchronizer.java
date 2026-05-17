package fr.dynamx.common.network.sync;

import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.network.sync.ClientEntityNetHandler;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SyncTarget;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.sync.variables.SynchronizedEntityVariableSnapshot;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.PooledHashMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;

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
        if (DynamXMain.proxy == null) return null;
        if (mySide == LogicalSide.SERVER) {
            Level clientWorld = DynamXMain.proxy.getClientWorld();
            if (clientWorld == null) return null;
            return clientWorld.getEntity(entity.getId());
        }
        Level serverWorld = DynamXMain.proxy.getServerWorld();
        if (serverWorld == null) return null;
        return serverWorld.getEntity(entity.getId());
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
            } else if (other.getEntity().tickCount > 20) {
                DynamXMain.log.error("Synchronized variable not found " + varId + " " + sourceVar + " " + other.getReceivedVariables() + " on " + entity);
            }
            buf.clear();
        }
        varsToSync.release();
    }

    @Override
    public void onPlayerStartControlling(Player player, boolean addControllers) {
        if (entity.physicsHandler != null) {
            entity.physicsHandler.setForceActivation(true);
        }
        setSimulationHolder(SimulationHolder.DRIVER_SP, player);
        if (!player.level().isClientSide || !isLocalPlayer(player) || !(entity instanceof BaseVehicleEntity)) {
            return;
        }
        for (IPhysicsModule<?> module : ((BaseVehicleEntity<?>) entity).getModules()) {
            IVehicleController c = module.createNewController();
            if (c != null) {
                controllers.add(c);
            }
        }
    }

    @Override
    public void onPlayerStopControlling(Player player, boolean removeControllers) {
        if (entity.physicsHandler != null) {
            entity.physicsHandler.setForceActivation(false);
        }
        setSimulationHolder(getDefaultSimulationHolder(), null);
        if (player.level().isClientSide && isLocalPlayer(player)) {
            controllers.clear();
        }
    }

    @Override
    public void onPrePhysicsTick(Object profilerObj) {
        Profiler profiler = (Profiler) profilerObj;
        if (entity.level().isClientSide && entity.initialized == PhysicsEntity.EnumEntityInitState.ALL && isLocalPlayerDriving()) {
            controllers.forEach(IVehicleController::update);
        }
        Entity other = getOtherSideEntity();
        if (other instanceof PhysicsEntity) {
            getReceivedVariables().forEach((key, value) -> ((SynchronizedEntityVariableSnapshot<Object>) value).updateVariable(tryGetVariable(key)));
        }
        entity.prePhysicsUpdateWrapper(profiler, entity.usesPhysicsWorld());
    }

    @Override
    public void onPostPhysicsTick(Object profilerObj) {
        Profiler profiler = (Profiler) profilerObj;
        entity.postUpdatePhysicsWrapper(profiler, entity.usesPhysicsWorld());
        Entity other = getOtherSideEntity();
        if (other instanceof PhysicsEntity && ((PhysicsEntity<?>) other).initialized == PhysicsEntity.EnumEntityInitState.ALL) {
            if (mySide != LogicalSide.SERVER) {
                sendMyVars((SPPhysicsEntitySynchronizer<T>) ((T) other).getSynchronizer(), SyncTarget.SERVER);
            } else {
                profiler.start(Profiler.Profiles.PKTSEND2);
                sendMyVars((SPPhysicsEntitySynchronizer<T>) ((T) other).getSynchronizer(), SyncTarget.SPECTATORS);
                profiler.end(Profiler.Profiles.PKTSEND2);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isLocalPlayerDriving() {
        return entity.getControllingPassenger() == Minecraft.getInstance().player;
    }

    private static boolean isLocalPlayer(Player player) {
        if (!player.level().isClientSide) return false;
        return player == Minecraft.getInstance().player;
    }

    @Override
    public boolean doesOtherSideUsesPhysics() {
        return !entity.level().isClientSide;
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
