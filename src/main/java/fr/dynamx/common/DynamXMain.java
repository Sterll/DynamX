package fr.dynamx.common;

import fr.dynamx.utils.DynamXConstants;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

import static fr.dynamx.utils.DynamXConstants.ID;
import static fr.dynamx.utils.DynamXConstants.NAME;
import static fr.dynamx.utils.DynamXConstants.VERSION;
import static fr.dynamx.utils.DynamXConstants.VERSION_TYPE;

/**
 * Mod entry point.
 *
 * <p>TODO port:1.20.1 - Many things from the 1.12 version cannot be 1:1-ported because the
 * FML lifecycle changed substantially:
 * <ul>
 *   <li>{@code @Mod(modid=...)} -> {@link Mod @Mod}{@code (ID)} (no name/version/deps fields;
 *       those live in {@code META-INF/neoforge.mods.toml}).</li>
 *   <li>{@code @SidedProxy} is gone; sided code is loaded via {@code DistExecutor} or a
 *       {@code Dist}-specific subclass annotated with {@code @Mod.EventBusSubscriber}. The
 *       {@code CommonProxy} field is now set in {@link #DynamXMain(IEventBus)} by checking
 *       {@code FMLEnvironment.dist}.</li>
 *   <li>{@code @Instance} -> just keep a static reference; mod construction takes the mod
 *       event bus in its constructor.</li>
 *   <li>{@code FMLConstructionEvent} / {@code FMLPreInitializationEvent} /
 *       {@code FMLInitializationEvent} / {@code FMLPostInitializationEvent} /
 *       {@code FMLLoadCompleteEvent} -> the 1.20.1 lifecycle has only {@code FMLCommonSetupEvent},
 *       {@code FMLClientSetupEvent}, {@code FMLDedicatedServerSetupEvent}, and
 *       {@code FMLLoadCompleteEvent}. Construction work that used to happen in
 *       {@code FMLConstructionEvent} now happens in the mod constructor.</li>
 *   <li>{@code EntityRegistry.registerModEntity} -> {@code DeferredRegister<EntityType<?>>}.</li>
 *   <li>{@code NetworkRegistry.INSTANCE.registerGuiHandler} -> {@code MenuType} +
 *       {@code MenuScreens.register} (client-only).</li>
 *   <li>{@code CapabilityManager.INSTANCE.register} -> {@code DeferredRegister<AttachmentType<?>>}
 *       (see {@code DynamXChunkDataProvider}).</li>
 *   <li>{@code FMLServerStartingEvent#registerServerCommand} -> {@code RegisterCommandsEvent}.</li>
 *   <li>{@code @NetworkCheckHandler} -> {@code IExtensionPoint.DisplayTest} via
 *       {@code ModLoadingContext#registerExtensionPoint}.</li>
 *   <li>{@code ForgeVersion.CheckResult} -> {@code VersionChecker.CheckResult} (NeoForge).</li>
 *   <li>{@code ProgressManager} is gone; loading progress is handled by FML itself.</li>
 * </ul>
 *
 * <p>Body is stubbed to a minimum that compiles: register the mod, the lifecycle subscriptions,
 * and forward to the proxy methods. Full feature parity has to be reconstructed once the
 * registries / network / packs init are migrated.
 */
@Mod(ID)
public class DynamXMain {
    public static DynamXMain instance;

    /**
     * TODO port:1.20.1 - assigned via {@code FMLEnvironment.dist}-based instantiation. Until the
     * client/server proxies are ported, this field stays {@code null} and proxy calls have to
     * null-check.
     */
    public static CommonProxy proxy;

    public static final Logger log = LogManager.getLogger("DynamX");

    public static File resourcesDirectory;

    /**
     * TODO port:1.20.1 - {@code ModProtectionContainer} (ACsLib) is being moved; kept as
     * {@code Object} until the ACsLib port lands.
     */
    public static Object mpsContainer;

    /**
     * An error that occurred during construction, to be thrown at common-setup.
     */
    public static RuntimeException memoizedConstructionError;
    /**
     * An error that occurred during loading, to be shown to the user at load-complete.
     */
    public static RuntimeException memoizedLoadingError;

    public DynamXMain(IEventBus modBus) {
        instance = this;
        log.info(NAME + " version " + VERSION + "-" + VERSION_TYPE + " is running, by Yanis and Aym'");

        try {
            proxy = net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()
                    ? (CommonProxy) Class.forName("fr.dynamx.client.ClientProxy").getDeclaredConstructor().newInstance()
                    : (CommonProxy) Class.forName("fr.dynamx.server.ServerProxy").getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            log.error("Failed to instantiate DynamX proxy; running without one (many features will be no-ops)", t);
        }

        // TODO port:1.20.1 - port the FMLConstructionEvent body: bullet engine install, MPS init,
        //   ACsLib threaded loading service, addons init, schedulePacksInit().

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::loadComplete);

        // Attachment-type registration for chunk data.
        try {
            fr.dynamx.common.capability.DynamXChunkDataProvider.register(modBus);
        } catch (Throwable t) {
            log.error("Failed to register DynamX chunk-data attachment", t);
        }

        // Server lifecycle events live on the NeoForge bus.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        if (memoizedConstructionError != null) {
            log.warn("Construction error detected, throwing it now");
            throw memoizedConstructionError;
        }
        // TODO port:1.20.1 - DynamXConfig.load(...) using NeoForge's ModConfig system
        //   (ModLoadingContext.get().registerConfig(...)).
        DynamXContext.initNetwork();

        // TODO port:1.20.1 - register items (ItemShockWave / ItemSlopes / ItemRagdoll) via
        //   DeferredRegister<Item>.
        // TODO port:1.20.1 - register entities (CarEntity, TrailerEntity, ...) via
        //   DeferredRegister<EntityType<?>>.
        // TODO port:1.20.1 - MenuType registration replaces NetworkRegistry.registerGuiHandler.

        if (proxy != null) {
            try {
                proxy.preInit();
                proxy.init();
            } catch (Throwable t) {
                log.error("DynamX proxy init failed", t);
            }
        }
    }

    @SuppressWarnings("unused")
    private void clientSetup(FMLClientSetupEvent event) {
        // TODO port:1.20.1 - hook MenuScreens.register here once client proxy is ported.
    }

    private void loadComplete(FMLLoadCompleteEvent event) {
        if (proxy != null) {
            proxy.completeInit();
        }
        // TODO port:1.20.1 - NeoForge VersionChecker.CheckResult replaces ForgeVersion.CheckResult.
        // TODO port:1.20.1 - DxModelData cache cleanup on dedicated server.
    }

    public void onServerStarting(ServerStartingEvent event) {
        // TODO port:1.20.1 - RegisterCommandsEvent is the new home for server commands.
    }

    public void onServerStarted(ServerStartedEvent event) {
        if (DynamXContext.getNetwork() != null) {
            DynamXContext.getNetwork().startNetwork();
        }
    }

    public void stopServer(ServerStoppedEvent event) {
        if (DynamXContext.getNetwork() != null) {
            DynamXContext.getNetwork().stopNetwork();
        }
    }

    @SuppressWarnings("unused")
    private void dynamX$keepConstantsImport() {
        // ensure DynamXConstants import is used.
        String unused = DynamXConstants.ID;
    }

    @SuppressWarnings("unused")
    private void dynamX$keepModLoadingContextImport(FMLJavaModLoadingContext ctx) {
        // ensure import is referenced — full registration TBD.
    }
}
