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

        // Content pack discovery: only the resource-pack scan and the gamedir handle are safe to
        // grab here. Item construction (`new ItemCar(...)` etc.) goes through `Item.<init>` which
        // calls `BuiltInRegistries.ITEM.createIntrusiveHolder(...)` and the items registry is frozen
        // outside the RegisterEvent window. The full pack reload therefore runs from the items
        // RegisterEvent listener below so all pack items are built while the registry is open.
        try {
            java.io.File gameDir = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().toFile();
            resourcesDirectory = gameDir;
            fr.dynamx.common.contentpack.ContentPackLoader.init(gameDir, fr.dynamx.utils.DynamXConstants.RES_DIR_NAME);
        } catch (Throwable t) {
            log.error("DynamX content pack discovery failed", t);
        }

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::loadComplete);

        // Single RegisterEvent listener handles both BLOCKS and ITEMS registries.
        //   - BLOCKS fires first: reload content packs (so DynamXBlock instances exist before
        //     freezeData() validates intrusive holders) and register every collected block.
        //   - ITEMS fires next: register every pack item collected during pack loading.
        modBus.addListener((net.minecraftforge.registries.RegisterEvent event) -> {
            if (event.getRegistryKey().equals(net.minecraftforge.registries.ForgeRegistries.Keys.BLOCKS)) {
                try {
                    java.io.File packsDir = new java.io.File(resourcesDirectory, fr.dynamx.utils.DynamXConstants.RES_DIR_NAME);
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
                try {
                    java.util.Map<net.minecraft.resources.ResourceLocation,
                            fr.aym.acslib.api.services.error.LocatedErrorList> allErrors =
                                    fr.dynamx.utils.errors.DynamXErrorManager.getErrorManager().getAllErrors();
                    int total = allErrors.values().stream().mapToInt(l -> l.getErrors().size()).sum();
                    if (total > 0) {
                        log.warn("DynamX content packs reported {} loading error(s)", total);
                        allErrors.forEach((loc, list) -> list.getErrors().stream().limit(3).forEach(err ->
                                log.warn("[pack {}] {} {} {} {}", loc, err.getLevel(),
                                        err.getGenericType(), err.getObject(), err.getMessage(),
                                        err.getException())));
                    }
                } catch (Throwable t) {
                    log.error("Failed to inspect DynamX pack errors", t);
                }
                fr.dynamx.common.items.DynamXItemRegistry.injectBlocks(event);
                return;
            }
            if (event.getRegistryKey().equals(net.minecraftforge.registries.ForgeRegistries.Keys.ITEMS)) {
                fr.dynamx.common.items.DynamXItemRegistry.injectItems(event);
            }
        });

        // Expose every pack found in <gamedir>/DynamX/ to Minecraft's resource manager so item
        // textures, models and lang files inside the pack's assets/ folder become resolvable.
        // Without this listener the pack files are loaded by ContentPackLoader for vehicle data
        // but their assets stay invisible to vanilla code paths (ItemRenderer, language loader).
        modBus.addListener((net.minecraftforge.event.AddPackFindersEvent event) -> {
            try {
                java.io.File packsDir = new java.io.File(resourcesDirectory, fr.dynamx.utils.DynamXConstants.RES_DIR_NAME);
                java.io.File[] files = packsDir.listFiles();
                if (files == null) return;
                net.minecraft.server.packs.PackType packType = event.getPackType();
                for (java.io.File file : files) {
                    final String name = file.getName();
                    final boolean isDir = file.isDirectory();
                    final boolean isZip = !isDir && (name.endsWith(".zip")
                            || name.endsWith(fr.dynamx.common.contentpack.ContentPackLoader.PACK_FILE_EXTENSION));
                    if (!isDir && !isZip) continue;
                    final String packId = "dynamx_pack/" + name;
                    event.addRepositorySource(consumer -> {
                        net.minecraft.server.packs.repository.Pack.ResourcesSupplier supplier = id -> isDir
                                ? new net.minecraftforge.resource.PathPackResources(id, true, file.toPath())
                                : new net.minecraft.server.packs.FilePackResources(id, file, false);
                        net.minecraft.server.packs.repository.Pack pack = net.minecraft.server.packs.repository.Pack.readMetaAndCreate(
                                packId,
                                net.minecraft.network.chat.Component.literal("DynamX: " + name),
                                true,
                                supplier,
                                packType,
                                net.minecraft.server.packs.repository.Pack.Position.TOP,
                                net.minecraft.server.packs.repository.PackSource.BUILT_IN);
                        if (pack != null) consumer.accept(pack);
                        else log.warn("DynamX pack {} has no valid pack.mcmeta, assets won't be exposed to MC", name);
                    });
                }

                // Virtual pack that generates the blockstate/model JSONs DynamX blocks need but that the
                // content packs don't ship (the legacy state-mapper / on-disk generation is gone in 1.20.1).
                // Client resources only; it answers exclusively for DynamX block paths and returns null
                // otherwise, so it never shadows the packs' own item models.
                if (packType == net.minecraft.server.packs.PackType.CLIENT_RESOURCES) {
                    event.addRepositorySource(consumer -> {
                        net.minecraft.server.packs.repository.Pack.ResourcesSupplier supplier =
                                id -> new fr.dynamx.client.DynamXGeneratedResourcePack();
                        net.minecraft.server.packs.repository.Pack pack = net.minecraft.server.packs.repository.Pack.readMetaAndCreate(
                                fr.dynamx.client.DynamXGeneratedResourcePack.PACK_ID,
                                net.minecraft.network.chat.Component.literal("DynamX generated models"),
                                true,
                                supplier,
                                net.minecraft.server.packs.PackType.CLIENT_RESOURCES,
                                net.minecraft.server.packs.repository.Pack.Position.TOP,
                                net.minecraft.server.packs.repository.PackSource.BUILT_IN);
                        if (pack != null) consumer.accept(pack);
                    });
                }
            } catch (Throwable t) {
                log.error("DynamX AddPackFindersEvent listener failed", t);
            }
        });

        // DeferredRegister wiring for all DynamX content (items, blocks, block entities, entities, creative tabs).
        fr.dynamx.common.core.DynamXItems.register(modBus);
        fr.dynamx.common.core.DynamXBlocks.register(modBus);
        fr.dynamx.common.core.DynamXBlockEntities.register(modBus);
        fr.dynamx.common.core.DynamXEntities.register(modBus);
        fr.dynamx.common.core.DynamXCreativeTabs.register(modBus);

        // Capability registration for the per-chunk DynamX collision data. Wires
        // RegisterCapabilitiesEvent (mod bus) + AttachCapabilitiesEvent<LevelChunk> (forge bus).
        try {
            fr.dynamx.common.capability.DynamXChunkDataProvider.register(modBus);
        } catch (Throwable t) {
            log.error("Failed to register DynamX chunk-data capability", t);
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

        // Discover and register all @SynchronizedPhysicsModule classes (position/controls/etc.).
        // Without this no EntityVariable is registered and physics entities never sync between sides
        // (in single player the server-side vehicle stays frozen at its spawn position).
        fr.dynamx.api.network.sync.SynchronizedEntityVariableRegistry.discoverSyncVars();
        fr.dynamx.api.network.sync.SynchronizedEntityVariableRegistry.sortRegistry(modid -> true);

        // Content pack discovery has moved to the mod constructor so RegisterEvent can see the
        // loaded items. See DynamXMain#DynamXMain(IEventBus).

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
        // Force client-only class init of the DynamX item BEWLR singleton so it is ready before
        // Forge queries IClientItemExtensions#getCustomRenderer on registered items.
        event.enqueueWork(() -> {
            //noinspection ResultOfMethodCallIgnored
            fr.dynamx.client.renders.model.renderer.DxItemModelLoader.INSTANCE.toString();
        });
        // TODO port:1.20.1 - hook MenuScreens.register here once client proxy is ported.
    }

    private void loadComplete(FMLLoadCompleteEvent event) {
        if (proxy != null) {
            proxy.completeInit();
        }
        // TODO port:1.20.1 - NeoForge VersionChecker.CheckResult replaces ForgeVersion.CheckResult.
        // TODO port:1.20.1 - DxModelData cache cleanup on dedicated server.
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // TODO port:1.20.1 - RegisterCommandsEvent is the new home for server commands.
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        if (DynamXContext.getNetwork() != null) {
            DynamXContext.getNetwork().startNetwork();
        }
        String testSpawn = System.getProperty("dynamx.testSpawn");
        if ((testSpawn == null || testSpawn.isEmpty()) && resourcesDirectory != null) {
            java.io.File marker = new java.io.File(resourcesDirectory, "dynamx_test_spawn.txt");
            if (marker.isFile()) {
                try {
                    testSpawn = new String(java.nio.file.Files.readAllBytes(marker.toPath())).trim();
                } catch (Throwable t) {
                    log.error("[testSpawn] failed reading marker", t);
                }
            }
        }
        if (testSpawn != null && !testSpawn.isEmpty()) {
            try {
                net.minecraft.server.level.ServerLevel overworld = event.getServer().overworld();
                net.minecraft.core.BlockPos sp = overworld.getSharedSpawnPos();
                com.jme3.math.Vector3f pos = new com.jme3.math.Vector3f(sp.getX() + 0.5f, sp.getY() + 4f, sp.getZ() + 0.5f);
                fr.dynamx.common.items.DynamXItemSpawner<?> item =
                        fr.dynamx.server.command.CmdSpawnObjects.getSpawnItem(testSpawn);
                if (item == null) {
                    log.error("[testSpawn] no item registered for '{}'", testSpawn);
                } else {
                    Object entity = item.getSpawnEntity(overworld, null, pos, 0f, 0);
                    if (entity instanceof net.minecraft.world.entity.Entity) {
                        overworld.addFreshEntity((net.minecraft.world.entity.Entity) entity);
                        log.info("[testSpawn] Spawned {} at {} {} {}", testSpawn, sp.getX(), sp.getY() + 4, sp.getZ());
                    } else {
                        log.error("[testSpawn] factory returned {}", entity == null ? "null" : entity.getClass().getName());
                    }
                }
            } catch (Throwable t) {
                log.error("[testSpawn] spawn failed", t);
            }
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
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
