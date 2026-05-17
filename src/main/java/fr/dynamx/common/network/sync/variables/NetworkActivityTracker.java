package fr.dynamx.common.network.sync.variables;

import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.common.entities.PhysicsEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side debug tracker that records which variables were sent / received per tick.
 */
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

    public static void drawNetworkActivity(GuiGraphics graphics, Font font, int size) {
        Minecraft mc = Minecraft.getInstance();
        Entity e;
        if (viewEntity != -1) {
            e = mc.level == null ? null : mc.level.getEntity(viewEntity);
        } else {
            e = (mc.hitResult instanceof EntityHitResult) ? ((EntityHitResult) mc.hitResult).getEntity() : null;
        }
        if (!(e instanceof PhysicsEntity))
            return;
        PhysicsEntity<?> entity = (PhysicsEntity<?>) e;
        int idx = viewIndex == -1 ? lastTime : viewIndex;
        int start = idx - size;
        int y = 2;
        for (int i = idx; i >= start; i--) {
            if (syncDebug.containsKey(i) && getDebugAt(i).containsKey(entity)) {
                graphics.drawString(font, "===============", 2, y, Color.GRAY.getRGB(), false);
                y += font.lineHeight;
                y = drawEntityDebug(y, i, entity, graphics, font);
            }
        }
    }

    public static int drawEntityDebug(int y, int time, PhysicsEntity<?> entity, GuiGraphics graphics, Font font) {
        EntitySyncData data = getDebugAt(time).get(entity);
        graphics.drawString(font, "-" + entity + " " + data.simulationHolder, 2, y, Color.GRAY.getRGB(), false);
        y += font.lineHeight;
        for (String s : data.activeVars) {
            boolean rcv = data.receivedVars.contains(s);
            boolean sent = data.sentVars.contains(s);
            Color color = sent ? (rcv ? Color.GREEN : Color.RED) : (rcv ? Color.CYAN : Color.ORANGE);
            graphics.drawString(font, s + " : " + (rcv ? "R" : " ") + (sent ? "S" : " "), 2, y, color.getRGB(), false);
            y += font.lineHeight;
        }
        return y;
    }

    public static void addSentVars(PhysicsEntity<?> entity, Collection<EntityVariable<?>> variables) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        lastTime = mc.player.tickCount;
        syncDebug.computeIfAbsent(lastTime, k -> new HashMap<>());
        getDebugAt(lastTime).computeIfAbsent(entity, e -> new EntitySyncData(
                e.getSynchronizer().getSimulationHolder(),
                e.getSynchronizer().getSynchronizedVariables().values().stream().map(EntityVariable::getName).collect(java.util.stream.Collectors.toList())));
        getDebugAt(lastTime).get(entity).sentVars.addAll(variables.stream().map(EntityVariable::getName).collect(java.util.stream.Collectors.toList()));
        if (viewIndex == -1) syncDebug.keySet().removeIf(i -> i < lastTime - 20 * 60);
    }

    public static void addReceivedVars(PhysicsEntity<?> entity, Collection<EntityVariable<?>> variables) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        lastTime = mc.player.tickCount;
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
