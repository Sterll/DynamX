package fr.dynamx.common.handlers;

import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class LevelLifecycleHandler {
    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            if (DynamXMain.proxy != null) {
                try {
                    DynamXMain.proxy.initPhysicsWorld(serverLevel);
                } catch (Throwable t) {
                    DynamXMain.log.error("Failed to init physics world for level " + serverLevel.dimension().location(), t);
                }
            }
        }
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        Level level = (Level) event.getLevel();
        if (level instanceof ServerLevel) {
            DynamXContext.getPhysicsWorldPerDimensionMap().remove(level.dimension());
        }
    }
}
