package fr.dynamx.common.network.sync.variables;

import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.common.entities.PhysicsEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side debug tracker that records which variables were sent / received per tick.
 */
// TODO port:1.20.1 - drawNetworkActivity used FontRenderer + Color from java.awt and the client
// objectMouseOver (now Minecraft.getInstance().hitResult). The whole drawing path needs Phase 8
// (client handlers/UI). The recording helpers (addSentVars/addReceivedVars) reference
// ClientEventHandler.MC, which isn't ported yet — calls are gated and stubbed.
public class NetworkActivityTracker {
    public static final Map<Integer, Map<PhysicsEntity<?>, EntitySyncData>> syncDebug = new HashMap<>();

    public static int lastTime;
    public static int viewIndex = -1;
    public static int viewEntity = -1;

    public static Map<PhysicsEntity<?>, EntitySyncData> getDebugAt(int time) {
        return syncDebug.get(time);
    }

    public static void pause() {
        viewIndex = lastTime;
    }

    public static void resume() {
        viewIndex = -1;
    }

    public static void drawNetworkActivity(Object fontRenderer, int size) {
        // TODO port:1.20.1 - Re-port using GuiGraphics + Font once Phase 8 client GUI is in.
    }

    public static int drawEntityDebug(int y, int time, PhysicsEntity<?> entity, Object fontRenderer) {
        // TODO port:1.20.1 - Re-port drawing once Phase 8 client GUI lands.
        return y;
    }

    public static void addSentVars(PhysicsEntity<?> entity, Collection<EntityVariable<?>> variables) {
        // TODO port:1.20.1 - lastTime = Minecraft.getInstance().player.tickCount once player is reachable.
        try {
            lastTime = net.minecraft.client.Minecraft.getInstance().player.tickCount;
        } catch (Throwable t) {
            return; // server side
        }
        syncDebug.computeIfAbsent(lastTime, k -> new HashMap<>());
        getDebugAt(lastTime).computeIfAbsent(entity, e -> new EntitySyncData(
                e.getSynchronizer().getSimulationHolder(),
                e.getSynchronizer().getSynchronizedVariables().values().stream().map(EntityVariable::getName).collect(java.util.stream.Collectors.toList())));
        getDebugAt(lastTime).get(entity).sentVars.addAll(variables.stream().map(EntityVariable::getName).collect(java.util.stream.Collectors.toList()));
        if (viewIndex == -1) syncDebug.keySet().removeIf(i -> i < lastTime - 20 * 60);
    }

    public static void addReceivedVars(PhysicsEntity<?> entity, Collection<EntityVariable<?>> variables) {
        try {
            lastTime = net.minecraft.client.Minecraft.getInstance().player.tickCount;
        } catch (Throwable t) {
            return;
        }
        syncDebug.computeIfAbsent(lastTime, k -> new HashMap<>());
        getDebugAt(lastTime).computeIfAbsent(entity, e -> new EntitySyncData(
                e.getSynchronizer().getSimulationHolder(),
                e.getSynchronizer().getSynchronizedVariables().values().stream().map(EntityVariable::getName).collect(java.util.stream.Collectors.toList())));
        getDebugAt(lastTime).get(entity).receivedVars.addAll(variables.stream().map(EntityVariable::getName).collect(java.util.stream.Collectors.toList()));
        if (viewIndex == -1) syncDebug.keySet().removeIf(i -> i < lastTime - 20 * 60);
    }

    public static class EntitySyncData {
        public SimulationHolder simulationHolder;
        public List<String> activeVars;
        public List<String> sentVars = new ArrayList<>();
        public List<String> receivedVars = new ArrayList<>();

        public EntitySyncData(SimulationHolder simulationHolder, List<String> activeVars) {
            this.simulationHolder = simulationHolder;
            this.activeVars = activeVars;
        }
    }
}
