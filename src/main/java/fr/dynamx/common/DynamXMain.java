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

        // Bullet native engine install. Legacy did this in FMLConstructionEvent; here we run it
        // synchronously at mod-construction time so that PhysicsSpace can be created later.
        // We must call NativeLibraryLoader.loadLibbulletjme (from the libbulletjme jar) so that
        // System.load is invoked from the same class loader as the native-method classes,
        // otherwise JNI can't bind the natives.
        try {
            fr.dynamx.utils.LibraryInstaller.configureSsslContext();
            java.io.File nativesDir = new java.io.File(
                    net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().toFile(),
                    "dynamx_natives");
            if (!nativesDir.exists() && !nativesDir.mkdirs()) {
                log.warn("Could not create dynamx_natives directory at {}", nativesDir);
            }
            // Download the native (legacy installer naming) then copy it under the jme3 loader's
            // expected name so NativeLibraryLoader can pick it up.
            fr.dynamx.utils.physics.NativeEngineInstaller.loadLibbulletjme(
                    nativesDir, DynamXConstants.LIBBULLET_VERSION, "Release", "Sp", false);
            com.jme3.system.Platform platform = com.jme3.system.JmeSystem.getPlatform();
            String libFile;
            switch (platform) {
                case Windows32: case Windows64: libFile = "bulletjme.dll"; break;
                case MacOSX32: case MacOSX64: case MacOSX_ARM64: libFile = "libbulletjme.dylib"; break;
                default: libFile = "libbulletjme.so"; break;
            }
            java.io.File legacy = new java.io.File(nativesDir,
                    platform + "Release" + "Sp_" + DynamXConstants.LIBBULLET_VERSION + "_" + libFile);
            java.io.File jmeExpected = new java.io.File(nativesDir,
                    platform + "ReleaseSp_" + libFile);
            if (legacy.exists() && !jmeExpected.exists()) {
                java.nio.file.Files.copy(legacy.toPath(), jmeExpected.toPath());
            }
            boolean loaded = com.jme3.system.NativeLibraryLoader.loadLibbulletjme(
                    true, nativesDir, "Release", "Sp");
            if (!loaded) {
                log.warn("NativeLibraryLoader reported failure for {}", jmeExpected);
            } else {
                log.info("Libbulletjme native engine loaded via jme3 loader from {}", nativesDir);
            }
        } catch (Throwable t) {
            log.error("Failed to load libbulletjme native engine — physics will be disabled", t);
            memoizedConstructionError = (t instanceof RuntimeException) ? (RuntimeException) t
                    : new RuntimeException("libbulletjme load failed", t);
        }

        // TODO port:1.20.1 - port the FMLConstructionEvent body: MPS init,
        //   ACsLib threaded loading service, addons init, schedulePacksInit().

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::loadComplete);

        // DeferredRegister wiring for all DynamX content (items, blocks, block entities, entities, creative tabs).
        fr.dynamx.common.core.DynamXItems.register(modBus);
        fr.dynamx.common.core.DynamXBlocks.register(modBus);
        fr.dynamx.common.core.DynamXBlockEntities.register(modBus);
        fr.dynamx.common.core.DynamXEntities.register(modBus);
        fr.dynamx.common.core.DynamXCreativeTabs.register(modBus);

        // Attachment-type registration for chunk data (stubbed until Capabilities migration).
        try {
            fr.dynamx.common.capability.DynamXChunkDataProvider.register(modBus);
        } catch (Throwable t) {
            log.error("Failed to register DynamX chunk-data attachment", t);
        }

        // Client-side mod-bus event wiring (entity renderers, etc.). Loaded reflectively so the
        // dedicated server jar never sees the client classes.
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            try {
                Class<?> clientReg = Class.forName("fr.dynamx.client.DynamXClientRegistration");
                clientReg.getMethod("register", IEventBus.class).invoke(null, modBus);
            } catch (Throwable t) {
                log.error("Failed to wire DynamX client registration", t);
            }
        }

        // Server lifecycle + level events live on the NeoForge bus.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new fr.dynamx.server.command.DynamXServerCommands());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new fr.dynamx.common.handlers.LevelLifecycleHandler());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        if (memoizedConstructionError != null) {
            log.warn("Construction error detected, throwing it now");
            throw memoizedConstructionError;
        }
        // TODO port:1.20.1 - DynamXConfig.load(...) using NeoForge's ModConfig system
        //   (ModLoadingContext.get().registerConfig(...)).
        DynamXContext.initNetwork();

        // Content pack discovery: scan the "DynamXResourcePacks" folder under the game directory
        // and load every folder/zip/.dnxpack inside it. Replaces the legacy schedulePacksInit()
        // path that ran through ACsLib's ThreadedLoadingService.
        try {
            java.io.File gameDir = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().toFile();
            resourcesDirectory = gameDir;
            java.io.File packsDir = fr.dynamx.common.contentpack.ContentPackLoader.init(gameDir, "DynamXResourcePacks");
            fr.dynamx.utils.optimization.Vector3fPool.openPool(
                    fr.dynamx.utils.optimization.SubClassPool.PACK_MODEL_LOAD);
            try {
                fr.dynamx.common.contentpack.ContentPackLoader.reload(packsDir, true);
            } finally {
                fr.dynamx.utils.optimization.Vector3fPool.closePool();
            }
        } catch (Throwable t) {
            log.error("DynamX content pack loading failed", t);
        }

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
