package fr.dynamx.client.network;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.network.sync.ClientEntityNetHandler;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SyncTarget;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.network.packets.MessageWalkingPlayer;
import fr.dynamx.common.network.sync.MPPhysicsEntitySynchronizer;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.common.network.sync.variables.NetworkActivityTracker;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.PooledHashMap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>TODO port:1.20.1 - direct port:</p>
 * <ul>
 *   <li>{@code EntityPlayer} -> {@code Player}.</li>
 *   <li>{@code Side.CLIENT} -> {@code LogicalSide.CLIENT}.</li>
 *   <li>{@code player.isUser()} -> {@code player instanceof LocalPlayer} - kept the simpler check via {@code Minecraft.getInstance().player == player}.</li>
 * </ul>
 */
public class ClientPhysicsEntitySynchronizer<T extends PhysicsEntity<?>> extends MPPhysicsEntitySynchronizer<T> implements ClientEntityNetHandler {
    private int ticksBeforeNextSync, skippedPacketsCount;

    private final List<IVehicleController> controllers = new ArrayList<>();

    private boolean usePhysicsThisTick;

    @Getter
    @Setter
    private Vector3f serverPos;
    @Getter
    @Setter
    private Quaternion serverRotation;

    public ClientPhysicsEntitySynchronizer(T entity) {
        super(entity);
    }

    @Override
    public void setSimulationTimeClient(int simulationTimeClient) {

    }

    @Override
    protected void onDataReceived(MessagePhysicsEntitySync<T> msg) {
        NetworkActivityTracker.addReceivedVars(entity, msg.getVarsToRead().keySet().stream().map(v -> getSynchronizedVariables().get(v)).collect(Collectors.toList()));
        super.onDataReceived(msg);
    }

    // TODO port:1.20.1 - parent PhysicsEntitySynchronizer typed profiler as Object pending Phase 5 port.
    @Override
    public void onPrePhysicsTick(Object profilerObj) {
        Profiler profiler = (Profiler) profilerObj;
        controllers.forEach(IVehicleController::update);
        if (getSimulationHolder().ownsPhysics(LogicalSide.CLIENT) || true) {
            readReceivedPackets();
            usePhysicsThisTick = true;
        } else if (!getSimulationHolder().ownsPhysics(LogicalSide.CLIENT)) {
            ticksBeforeNextSync--;
            if (skippedPacketsCount > 0)
                skippedPacketsCount--;
            if (ticksBeforeNextSync <= 0) {
                if (!getReceivedPackets().isEmpty()) {
                    readReceivedPackets();
                    ticksBeforeNextSync = entity.getSyncTickRate();
                    usePhysicsThisTick = DynamXMain.proxy.ownsSimulation(entity);
                } else {
                    usePhysicsThisTick = true;
                }
            } else if (ticksBeforeNextSync > 0) {
                usePhysicsThisTick = DynamXMain.proxy.ownsSimulation(entity);
            }
        }
        entity.prePhysicsUpdateWrapper(profiler, usePhysicsThisTick);

        if (getSimulationHolder().ownsPhysics(LogicalSide.CLIENT))
            sendVariables();
    }

    protected void sendVariables() {
        PooledHashMap<Integer, EntityVariable<?>> syncData = getVarsToSync(LogicalSide.CLIENT, SyncTarget.SERVER);
        NetworkActivityTracker.addSentVars(entity, syncData.values());
        if (!syncData.isEmpty()) {
            DynamXContext.getNetwork().sendToServer(new MessagePhysicsEntitySync(entity, ClientPhysicsSyncManager.simulationTime, syncData, false));
        } else {
            syncData.release();
        }
    }

    @Override
    public void onPostPhysicsTick(Object profilerObj) {
        Profiler profiler = (Profiler) profilerObj;
        entity.postUpdatePhysicsWrapper(profiler, usePhysicsThisTick);
    }

    @Override
    public void onPlayerStartControlling(Player player, boolean addControllers) {
        if (entity.physicsHandler != null) {
            entity.physicsHandler.setForceActivation(true);
        }
        if (isLocalPlayer(player)) {
            if (addControllers && entity instanceof BaseVehicleEntity) {
                for (Object module : ((BaseVehicleEntity) entity).getModules()) {
                    IVehicleController c = ((IPhysicsModule) module).createNewController();
                    if (c != null)
                        controllers.add(c);
                }
            }
            setSimulationHolder(SimulationHolder.DRIVER, player);
            ClientPhysicsSyncManager.simulationTime = 0;
        } else {
            setSimulationHolder(SimulationHolder.OTHER_CLIENT, player);
        }
    }

    @Override
    public void onPlayerStopControlling(Player player, boolean removeControllers) {
        if (entity.physicsHandler != null) {
            entity.physicsHandler.setForceActivation(false);
        }
        setSimulationHolder(getDefaultSimulationHolder(), null);
        if (removeControllers && isLocalPlayer(player)) {
            controllers.clear();
        }
    }

    @Override
    public void onWalkingPlayerChange(int playerId, Vector3f offset, byte face) {
        super.onWalkingPlayerChange(playerId, offset, face);
        DynamXContext.getNetwork().sendToServer(new MessageWalkingPlayer(entity, playerId, offset, face));
    }

    @Override
    public List<IVehicleController> getControllers() {
        return controllers;
    }

    /**
     * <p>TODO port:1.20.1 - replacement for the 1.12 {@code EntityPlayer.isUser()} check.
     * Uses Minecraft.getInstance().player == player when called on the client.</p>
     */
    private static boolean isLocalPlayer(Player player) {
        return net.minecraft.client.Minecraft.getInstance().player == player;
    }
}
