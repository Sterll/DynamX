package fr.dynamx.common.physics;

import fr.dynamx.DynamX;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.handlers.TaskScheduler;
import fr.dynamx.common.network.packets.MessageCollisionDebugDraw;
import fr.dynamx.server.command.CmdNetworkConfig;
import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.SubClassPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.event.TickEvent;

import java.util.HashMap;
import java.util.Map;

public class PhysicsTickHandler {
    private static long lastTickTimeMs;
    public static final Map<Player, Integer> requestedDebugInfo = new HashMap<>();

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void tickClient(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            try {
                Profiler.get().start(Profiler.Profiles.TICK);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (canTickClient(Minecraft.getInstance())) {
            tickWorldPhysics(event.phase, Minecraft.getInstance().level);
        }

        if (event.phase == TickEvent.Phase.START) {
            QuaternionPool.openPool(SubClassPool.TICK_CLIENT);
            Vector3fPool.openPool(SubClassPool.TICK_CLIENT);
            DynamXLoadingTasks.tick();
        } else {
            Profiler.get().end(Profiler.Profiles.TICK);
            if (Minecraft.getInstance().level != null) {
                boolean profiling = DynamXDebugOptions.PROFILING.isActive();
                if (profiling) {
                    if (DynamXMain.proxy.getTickTime() % 20 == 0) {
                        Profiler.get().printData("Client");
                    }
                }
                Profiler.setIsProfilingOn(profiling);
                Profiler.get().update();
            }

            if (!Minecraft.getInstance().hasSingleplayerServer()) {//If not in solo
                TaskScheduler.tick();
            }
            Vector3fPool.closePool();
            QuaternionPool.closePool();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private boolean canTickClient(Minecraft mc) {
        return mc.level != null && !mc.isPaused() && DynamXMain.proxy.shouldUseBulletSimulation(mc.level) && DynamXContext.getPhysicsWorld(mc.level) != null;
    }

    @SubscribeEvent
    public void tickServer(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            QuaternionPool.openPool(SubClassPool.TICK_SERVER);
            Vector3fPool.openPool(SubClassPool.TICK_SERVER);
            try {
                Profiler.get().start(Profiler.Profiles.TICK);
            } catch (Exception e) {
                DynamX.LOGGER.error("Profiler error", e);
            }
        }
        // TODO port:1.20.1 - FMLCommonHandler removed; use ServerLifecycleHooks.getCurrentServer().getAllLevels()
        for (ServerLevel world : net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
            if (canTickServer(world)) {
                tickWorldPhysics(event.phase, world);
            }
        }

        if (event.phase == TickEvent.Phase.START) {
            if (FMLLoader.getDist().isDedicatedServer()) {
                DynamXLoadingTasks.tick();
            }
        } else {
            Profiler.get().end(Profiler.Profiles.TICK);
            sendClientsDebug();
            Profiler.get().update();
            TaskScheduler.tick();
            Vector3fPool.closePool();
            QuaternionPool.closePool();
        }
    }

    private boolean canTickServer(Level world) {
        return world != null && DynamXMain.proxy.shouldUseBulletSimulation(world) && DynamXContext.getPhysicsWorld(world) != null;
    }

    private void tickWorldPhysics(TickEvent.Phase phase, Level world) {
        IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(world);
        if (phase == TickEvent.Phase.END) {
            physicsWorld.tickEnd();
            return;
        }
        // START phase
        QuaternionPool.openPool(SubClassPool.TICK_PHYSICS_WORLD);
        Vector3fPool.openPool(SubClassPool.TICK_PHYSICS_WORLD);

        physicsWorld.tickStart();

        float deltaTimeSecond = getDeltaTimeMilliseconds() * 1.0E-3F;
        if (deltaTimeSecond > 0.5f) // game was paused ?
            deltaTimeSecond = 0.05f;

        Profiler.get().start(Profiler.Profiles.STEP_SIMULATION);
        physicsWorld.stepSimulation(deltaTimeSecond);
        Profiler.get().end(Profiler.Profiles.STEP_SIMULATION);

        if (physicsWorld.getDynamicsWorld() != null) {
            physicsWorld.getDynamicsWorld().getJointList().forEach(joint -> {
                if ((joint.getBodyA() != null && !physicsWorld.getDynamicsWorld().contains(joint.getBodyA()))
                        || (joint.getBodyB() != null && !physicsWorld.getDynamicsWorld().contains(joint.getBodyB()))) {
                    physicsWorld.removeJoint(joint);
                }
            });
        }

        Vector3fPool.closePool();
        QuaternionPool.closePool();
    }

    private void sendClientsDebug() {
        boolean profiling;
        if (DynamXMain.proxy.getServerWorld().getServer().isDedicatedServer()) { //If integrated server, the vars are already shared
            profiling = false;
            boolean networkDebug = false, wheelData = false;
            for (Map.Entry<Player, Integer> e : requestedDebugInfo.entrySet()) {
                //Don't spam of debug packets
                if (DynamXMain.proxy.getServerWorld().getServer().getTickCount() % 10 == 0 && (DynamXDebugOptions.BLOCK_BOXES.matchesNetMask(e.getValue()) || DynamXDebugOptions.SLOPE_BOXES.matchesNetMask(e.getValue()))) {
                    DynamXContext.getNetwork().sendToClient(new MessageCollisionDebugDraw(DynamXDebugOptions.BLOCK_BOXES.getDataIn(), DynamXDebugOptions.SLOPE_BOXES.getDataIn()), EnumPacketTarget.PLAYER, (ServerPlayer) e.getKey());
                }
                if (DynamXDebugOptions.PROFILING.matchesNetMask(e.getValue())) {
                    profiling = true;
                } else if (DynamXDebugOptions.FULL_NETWORK_DEBUG.matchesNetMask(e.getValue())) {
                    networkDebug = true;
                } else if (DynamXDebugOptions.WHEEL_ADVANCED_DATA.matchesNetMask(e.getValue())) {
                    wheelData = true;
                }
            }
            if (DynamXMain.proxy.getServerWorld().getServer().getTickCount() % 5 == 0) //requestedDebugInfo is sent all 5 ticks
                requestedDebugInfo.clear();
            if (networkDebug != DynamXDebugOptions.FULL_NETWORK_DEBUG.isActive()) {
                //System.out.println("Setting FULL_NETWORK_DEBUG active : " + networkDebug);
                if (networkDebug)
                    DynamXDebugOptions.FULL_NETWORK_DEBUG.enable();
                else
                    DynamXDebugOptions.FULL_NETWORK_DEBUG.disable();
            }
            if (wheelData != DynamXDebugOptions.WHEEL_ADVANCED_DATA.isActive()) {
                //System.out.println("Setting WHEEL_ADVANCED_DATA active : " + wheelData);
                if (wheelData)
                    DynamXDebugOptions.WHEEL_ADVANCED_DATA.enable();
                else
                    DynamXDebugOptions.WHEEL_ADVANCED_DATA.disable();
            }
        } else {
            profiling = DynamXDebugOptions.PROFILING.isActive();
        }
        if (profiling) {
            if (DynamXMain.proxy.getTickTime() % 20 == 0) {
                Profiler.get().printData("Server");
            }
        }
        Profiler.setIsProfilingOn(profiling);
    }

    private float getDeltaTimeMilliseconds() {
        long cur = System.currentTimeMillis();
        long dt = cur - lastTickTimeMs;
        lastTickTimeMs = cur;
        return dt;
    }
}
