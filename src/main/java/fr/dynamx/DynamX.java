package fr.dynamx;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(DynamX.MOD_ID)
public class DynamX {
    public static final String MOD_ID = "dynamx";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DynamX(IEventBus modBus) {
        LOGGER.info("DynamX 5.0 (NeoForge 1.20.1) - port en cours");
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("DynamX common setup");
    }
}
