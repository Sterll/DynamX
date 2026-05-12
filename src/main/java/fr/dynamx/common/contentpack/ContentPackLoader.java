package fr.dynamx.common.contentpack;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.DynamX;
import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.common.contentpack.loader.InfoList;
import fr.dynamx.common.contentpack.loader.InfoLoader;
import fr.dynamx.common.contentpack.sync.PackSyncHandler;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Root of the DynamX packs system.
 *
 * TODO port:1.20.1 - Original referenced (not yet ported):
 *   - fr.aym.acslib.api.services.mps.ModProtectionContainer + ContentPackSystemEvent +
 *     SynchronizedEntityVariableRegistry.discoverSyncVars - MPS / events are Phase 0/5.
 *     {@link #init} no longer takes a {@code ModProtectionContainer} (relaxed to Object)
 *     and the protected-resources map stores Object too. The
 *     {@link #getProtectedResources} accessor returns null entries until MPS is back.
 *   - fr.dynamx.api.events.{ContentPackSystemEvent, EventPhase} - Phase 5.
 *   - fr.dynamx.common.{DynamXContext, DynamXMain} - replaced by DynamX.LOGGER and a
 *     local {@code resourcesDirectory} static set during {@link #init}.
 *   - fr.dynamx.utils.DynamXLoadingTasks - not yet ported; the end-task notification is
 *     replaced by a log line.
 *   - net.minecraft.block.Block -&gt; net.minecraft.world.level.block.Block (1.20.1).
 *     {@code Block.getBlockFromName} no longer exists; replaced by
 *     {@code BuiltInRegistries.BLOCK.get(new ResourceLocation(...))}.
 *   - net.minecraft.launchwrapper.LaunchClassLoader - removed; the PackFiles.jar hot-add
 *     is dropped (NeoForge does not allow runtime classpath augmentation without a
 *     transformer service, and MPS-protected jars should now be supplied through the
 *     mods folder).
 *   - net.minecraft.client.Minecraft.addScheduledTask / getLanguageManager.onResourceManagerReload
 *     -&gt; replaced by Minecraft.getInstance().tell + reloadable resource manager events
 *     in Phase 4 (Client side). The hook is stubbed (debug log).
 *   - FMLCommonHandler.instance().getSide().isClient() -&gt; FMLEnvironment.dist.isClient().
 *   - net.minecraftforge.fml.common.{FMLModContainer, MetadataCollection, ProgressManager,
 *     discovery.{ContainerType, ModCandidate}} - removed/replaced; the legacy
 *     loadPackResources() inlined a FMLModContainer to register pack assets as a resource
 *     pack. NeoForge handles this via {@code Minecraft.getInstance().getResourcePackRepository()}
 *     or via PackRepository at server start; both paths are Phase 4 client/server resource
 *     loaders. The method is stubbed and always returns true.
 */
public class ContentPackLoader {
    public static final String PACK_FILE_EXTENSION = ".dnxpack";

    /**
     * For hot reload detection
     */
    private static boolean initialized;
    public static boolean isHotReloading;
    /**
     * Loaded BlockInfo
     */
    private static final Map<Block, float[]> BLOCKS_GRIP = new HashMap<>();
    private static final float[] DEFAULT_GRIP = new float[]{1, 0.9f};

    /**
     * Blocks where slopes can be placed
     */
    public static final List<Block> slopes = new ArrayList<>();

    public static boolean PLACE_SLOPES = false;
    public static int SLOPES_LENGTH = 20;

    /**
     * Resources directory of the game (the parent of "DynamXResourcePacks"). Populated by
     * {@link #init}. Mirrors what {@code DynamXMain.resourcesDirectory} held in the legacy.
     */
    public static File resourcesDirectory;

    /**
     * Protected resources of protected packs.
     *
     * TODO port:1.20.1 - Map values were {@link fr.aym.acslib.api.services.mps.ModProtectionContainer}.
     *   Relaxed to Object pending MPS rewire (Phase 0).
     */
    @Getter
    private static final Map<String, Object> protectedResources = new HashMap<>();

    /**
     * Inits the packs by finding the packs folder.
     *
     * TODO port:1.20.1 - Original signature was
     *   {@code init(FMLConstructionEvent, ModProtectionContainer, String, Side)}. The
     *   FMLConstructionEvent is replaced by calls to {@link AddonLoader#discoverAddons()}
     *   and {@link fr.dynamx.common.contentpack.loader.SubInfoTypesRegistry#discoverSubInfoTypes()}.
     *   SynchronizedEntityVariableRegistry.discoverSyncVars(event) call removed (Phase 5).
     *   The MPS container parameter is dropped (Phase 0); side detection uses FMLEnvironment.
     *
     * @param resDir     The folder that *contains* the packs folder; typically the Minecraft
     *                   working directory. Stored in {@link #resourcesDirectory} for later use.
     * @param folderName The packs folder name (relative to resDir)
     * @return The chosen folder file
     */
    public static File init(File resDir, String folderName) {
        resourcesDirectory = resDir;
        PackInfo.resourcesDirectory = resDir;
        //Production-environment
        File myDir = new File(folderName);
        if (!myDir.exists()) {
            if (myDir.getParentFile() != null) {
                //Dev-environment
                myDir = new File(myDir.getParentFile().getParentFile(), folderName);
                if (!myDir.exists()) {
                    //first-run of the mod, in production environment
                    myDir = new File(folderName);
                    myDir.mkdirs();
                }
            } else //First run, in production environment
                myDir.mkdirs();
        }
        //Discover addons / sub info types - the legacy code wired these up here using
        //FMLConstructionEvent. The NeoForge equivalents are no-arg.
        AddonLoader.discoverAddons();
        fr.dynamx.common.contentpack.loader.SubInfoTypesRegistry.discoverSubInfoTypes();
        // TODO port:1.20.1 - SynchronizedEntityVariableRegistry.discoverSyncVars(event) - Phase 5.
        int packCount = 0;
        File[] files = myDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() || file.getName().endsWith(".zip") || file.getName().endsWith(PACK_FILE_EXTENSION)) {
                    DynamX.LOGGER.debug("Loading resource pack: {}", file.getName());
                    if (loadPackResources(file, file.isDirectory()))
                        packCount++;
                    // TODO port:1.20.1 - ModProtectionContainer wiring removed (Phase 0). The
                    //   legacy code populated a per-pack secure repository here.
                    protectedResources.put(file.getName(), null);
                }
                if (file.isDirectory()) {
                    //Legacy: load protected .jar files via LaunchClassLoader.addURL(...).
                    //LaunchClassLoader is gone in NeoForge; the secure-pack mechanism must be
                    //rebuilt as a Phase 0 SecureModuleLayer service. Dropped for now.
                    File[] inner = file.listFiles();
                    if (inner != null) {
                        for (File f : inner) {
                            if (f.isFile() && f.getName().endsWith(".jar")) {
                                DynamX.LOGGER.warn("Skipped legacy MPS pack jar {} - secure jar loader not yet ported", f.getName());
                            }
                        }
                    }
                }
            }
        }
        DynamX.LOGGER.info("Loaded {} DynamX resource packs", packCount);
        return myDir;
    }

    /**
     * TODO port:1.20.1 - Original built a FMLModContainer and called
     *   {@code FMLClientHandler.instance().addModAsResource(container)}. Both APIs are gone in
     *   NeoForge; resource pack registration should now go through PackRepository (server) or
     *   the new pack source API (client). Stubbed for Phase 3b; returns true.
     */
    private static boolean loadPackResources(File file, boolean isDirectory) {
        // TODO port:1.20.1 - Register file as a NeoForge resource pack (Phase 4).
        DynamX.LOGGER.debug("Resource pack registration stubbed for {} (dir={})", file.getName(), isDirectory);
        return true;
    }

    @Nonnull
    public static Object getProtectedResources(String packName) {
        return protectedResources.getOrDefault(packName, null);
    }

    /**
     * All builtin addon objects ({@link fr.dynamx.api.contentpack.object.subinfo.ISubInfoType}s, blocks, items...) should have been registered before packs loading <br> <br>
     * Use the addons init callback to avoid problems
     *
     * @return True if the loading of packs has started
     */
    public static boolean isPackLoadingStarted() {
        return initialized;
    }

    /**
     * Reloads all packs.
     *
     * TODO port:1.20.1 - {@code loadBlocksConfigs} parameter is preserved verbatim. The
     *   "MinecraftForge.EVENT_BUS.post(ContentPackSystemEvent.Load.PRE)" / .POST calls are
     *   stubbed (Phase 5) - log messages take their place.
     *   DynamXContext.getDxModelDataCache().clear() is also stubbed (Phase 7).
     */
    public static void reload(File resDir, boolean loadBlocksConfigs) {
        isHotReloading = initialized;
        if (!isHotReloading)
            initialized = true;
        for (InfoList<?> loader : DynamXObjectLoaders.getInfoLists())
            loader.clear(isHotReloading);
        DynamXErrorManager.getErrorManager().clear(DynamXErrorManager.PACKS_ERRORS);
        fr.dynamx.common.DynamXContext.getDxModelDataCache().clear();
        try {
            DynamX.LOGGER.info("Loading content pack system...");
            // TODO port:1.20.1 - ContentPackSystemEvent.Load(EventPhase.PRE) - Phase 5.
            int packCount = 0;
            int errorCount = 0;
            String suffix = ".dynx";
            File[] files = resDir.listFiles();
            if (files != null) {
                for (File contentPack : files) {
                    if (contentPack.getName().equals("slopes.dynx")) {
                        if (loadBlocksConfigs)
                            registerSlopes(new BufferedReader(new InputStreamReader(new FileInputStream(contentPack))));
                    } else if (contentPack.getName().equals("blocks.dynx")) {
                        if (loadBlocksConfigs)
                            registerBlockGrip(new BufferedReader(new InputStreamReader(new FileInputStream(contentPack))));
                    } else if (contentPack.isDirectory()) {
                        String loadingPack = contentPack.getName();
                        try {
                            AtomicReference<PackFile> packInfo = new AtomicReference<>();
                            List<PackFile> packFiles = new ArrayList<>();
                            try (Stream<Path> configs = Files.walk(Paths.get(contentPack.getPath()))) {
                                configs.forEach(path -> {
                                    if (path.toString().endsWith(suffix)) {
                                        try {
                                            PackFile packFile = new PackFile(path.getFileName().toString(), new FileInputStream(path.toFile()));
                                            if (packFile.getName().endsWith("pack_info.dynx"))
                                                packInfo.set(packFile);
                                            else
                                                packFiles.add(packFile);
                                        } catch (FileNotFoundException e) {
                                            throw new RuntimeException("Failed to find file " + path, e);
                                        }
                                    }
                                });
                            }
                            loadPack(loadingPack, contentPack, ContentPackType.FOLDER, suffix, packInfo.get(), packFiles);
                            packCount++;
                        } catch (Throwable e) {
                            if (!(e instanceof Exception))
                                e = new RuntimeException("encapsulated error", e);
                            DynamXErrorManager.addError(loadingPack, DynamXErrorManager.PACKS_ERRORS, "pack_load_fail", ErrorLevel.FATAL, "loading folder pack", loadingPack, (Exception) e, 800);
                            errorCount++;
                        }
                    } else if (contentPack.isFile() && (contentPack.getName().endsWith(".zip") || contentPack.getName().endsWith(PACK_FILE_EXTENSION))) {
                        String loadingPack = contentPack.getName().replace(".zip", "").replace(PACK_FILE_EXTENSION, "");
                        try {
                            ZipFile zip = new ZipFile(contentPack);
                            PackFile packInfo = null;
                            List<PackFile> packFiles = new ArrayList<>();
                            Enumeration<? extends ZipEntry> configs = zip.entries();
                            while (configs.hasMoreElements()) {
                                ZipEntry config = configs.nextElement();
                                if (config.getName().endsWith(suffix)) {
                                    PackFile packFile = new PackFile(config.getName().substring(config.getName().lastIndexOf("/") + 1), zip.getInputStream(config));
                                    if (config.getName().endsWith("pack_info.dynx"))
                                        packInfo = packFile;
                                    else
                                        packFiles.add(packFile);
                                }
                            }
                            loadPack(loadingPack, contentPack, contentPack.getName().endsWith(".zip") ? ContentPackType.ZIP : ContentPackType.DNXPACK, suffix, packInfo, packFiles);
                            packCount++;
                        } catch (Throwable e) {
                            if (!(e instanceof Exception))
                                e = new RuntimeException("encapsulated error", e);
                            DynamXErrorManager.addError(loadingPack, DynamXErrorManager.PACKS_ERRORS, "pack_load_fail", ErrorLevel.FATAL, "loading compressed pack", loadingPack, (Exception) e, 800);
                            errorCount++;
                        }
                    } else if (!contentPack.getName().endsWith(".dll") && !contentPack.getName().endsWith(".so") && !contentPack.getName().endsWith(".dylib")) {
                        DynamX.LOGGER.warn("File {} isn't a valid DynamX content pack file", contentPack.getName());
                    }
                }
            }
            //Load shapes
            for (InfoList<?> loader : DynamXObjectLoaders.getInfoLists()) {
                DynamX.LOGGER.debug("Post load : {}", loader.getName());
                loader.postLoad(isHotReloading);
            }
            // TODO port:1.20.1 - ContentPackSystemEvent.Load(EventPhase.POST) - Phase 5.
            DynamX.LOGGER.info("Loaded {} content packs", packCount);
            if (errorCount > 0)
                DynamX.LOGGER.warn("Ignored {} errored packs", errorCount);
        } catch (Throwable e) {
            DynamX.LOGGER.error("Fatal error while loading DynamX packs, we can't continue !", e);
            throw new RuntimeException(e);
        }
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            //Reload languages added by packs
            scheduleLanguageRefresh();
        }
        PackSyncHandler.computeAll();
        fr.dynamx.utils.DynamXLoadingTasks.endTask(fr.dynamx.utils.DynamXLoadingTasks.PACK);
    }

    private static void loadPack(String loadingPack, File contentPack, ContentPackType packType, String suffix, PackFile packInfo, List<PackFile> packFiles) {
        //Search for real pack name in the pack info
        String packVersion = "<missing pack info>";
        PackInfo loadedInfo = packInfo != null ? loadPackInfoFile(loadingPack, suffix, packInfo, contentPack.getName(), packType) : null;
        if (loadedInfo != null) { // Pack info exists
            loadingPack = loadedInfo.getFixedPackName();
            packVersion = loadedInfo.getPackVersion();
        } else { // Pack info doesn't exist: create a dummy one
            loadedInfo = new PackInfo(loadingPack, contentPack.getName(), packType).setPackVersion("dummy_info");
            DynamXErrorManager.addError(loadingPack, DynamXErrorManager.PACKS_ERRORS, "missing_pack_info", ErrorLevel.HIGH, loadedInfo.getName(), "Add a pack_info.dynx file in the pack !", null, 600);
            DynamXObjectLoaders.PACKS.loadItems(loadedInfo, isHotReloading);
        }
        DynamX.LOGGER.info("Loading {} version {} (in {})", loadingPack, packVersion, contentPack.getName());
        for (PackFile packFile : packFiles) {
            loadFile(loadingPack, suffix, packFile);
        }
    }

    private static PackInfo loadPackInfoFile(String loadingPack, String suffix, PackFile file, String pathName, ContentPackType packType) {
        try {
            return DynamXObjectLoaders.PACKS.load(loadingPack, file, isHotReloading, pathName, packType);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Throwable e) {
            if (!(e instanceof Exception))
                e = new RuntimeException("encapsulated error", e);
            DynamXErrorManager.addError(loadingPack, DynamXErrorManager.PACKS_ERRORS, "pack_file_load_error", ErrorLevel.FATAL, file.getName().replace(suffix, ""), null, (Exception) e, 100);
            return null;
        }
    }

    private static void loadFile(String loadingPack, String suffix, PackFile file) {
        try {
            String configName = file.getName().substring(0, file.getName().length() - suffix.length()).toLowerCase();
            boolean loaded = false;
            for (InfoLoader<?> loader : DynamXObjectLoaders.getInfoLoaders()) {
                if (loader.load(loadingPack, configName, file, isHotReloading)) {
                    loaded = true;
                    break;
                }
            }
            if (!loaded)
                throw new IllegalArgumentException("Invalid " + suffix + " file name : " + file.getName());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Throwable e) {
            if (!(e instanceof Exception))
                e = new RuntimeException("encapsulated error", e);
            DynamXErrorManager.addError(loadingPack, DynamXErrorManager.PACKS_ERRORS, "pack_file_load_error", ErrorLevel.FATAL, file.getName().replace(suffix, ""), null, (Exception) e, 100);
        }
    }

    /**
     * TODO port:1.20.1 - Original used {@code Minecraft.getMinecraft().addScheduledTask}
     *   to call {@code getLanguageManager().onResourceManagerReload(...)}. 1.20.1 uses
     *   {@code Minecraft.getInstance().tell(...)} and a different reload pipeline; the
     *   triggering will be re-implemented in Phase 4 (client side).
     */
    private static void scheduleLanguageRefresh() {
        DynamX.LOGGER.debug("Language refresh scheduling stubbed - pending Phase 4 client wire-up");
    }

    /**
     * TODO port:1.20.1 - {@code Block.getBlockFromName} was removed in 1.20.1; use the
     *   block registry by ResourceLocation. We parse "namespace:path" or fall back to
     *   "minecraft:path".
     */
    private static void registerSlopes(BufferedReader reader) {
        String[] array = reader.lines().toArray(String[]::new);
        for (int i = 0; i < array.length; i++) {
            //Configuring length of slopes
            Pattern p = Pattern.compile("length\\s*:\\s*(\\d+)");
            Matcher m = p.matcher(array[i]);
            if (m.find()) {
                SLOPES_LENGTH = Integer.parseInt(m.group(1));
                continue;
            }
            //Configuring auto-placing of slopes
            p = Pattern.compile("auto place\\s*:\\s*(\\w+)");
            m = p.matcher(array[i]);
            if (m.find()) {
                PLACE_SLOPES = Boolean.parseBoolean(m.group(1));
                continue;
            }
            Block block = resolveBlock(array[i].trim());
            if (block != null) {
                slopes.add(block);
            } else {
                DynamX.LOGGER.error("Block {} doesn't exist", array[i]);
            }
        }
    }

    /**
     * TODO port:1.20.1 - Same notes as {@link #registerSlopes}.
     */
    private static void registerBlockGrip(BufferedReader reader) {
        reader.lines().forEach(s -> {
            if (!s.trim().startsWith("//") && s.contains(":")) {
                String[] blockString = s.split(": ");
                Block block = resolveBlock(blockString[0].trim());
                if (block != null) {
                    String[] values = blockString[1].split(" ");
                    if (values.length > 1) {
                        BLOCKS_GRIP.put(block, new float[]{
                                Float.parseFloat(values[0]), Float.parseFloat(values[1])});
                    } else {
                        BLOCKS_GRIP.put(block, new float[]{
                                Float.parseFloat(values[0]), Float.parseFloat(values[0])});
                    }
                } else {
                    DynamX.LOGGER.error("Bad block grip config: block {} doesn't exist", blockString[0]);
                }
            }
        });
    }

    private static Block resolveBlock(String name) {
        try {
            net.minecraft.resources.ResourceLocation rl = name.contains(":")
                    ? new net.minecraft.resources.ResourceLocation(name)
                    : new net.minecraft.resources.ResourceLocation("minecraft", name);
            Block b = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(rl);
            //BLOCK.get returns minecraft:air on miss; treat that as miss only when the user did not ask for it
            if (b == null) return null;
            if (b == net.minecraft.world.level.block.Blocks.AIR && !"minecraft:air".equals(rl.toString()))
                return null;
            return b;
        } catch (Throwable t) {
            DynamX.LOGGER.warn("Failed to resolve block '{}'", name, t);
            return null;
        }
    }

    public static float[] getBlockFriction(Block of) {
        return BLOCKS_GRIP.getOrDefault(of, DEFAULT_GRIP);
    }

    public static Map<Block, float[]> getBlocksGrip() {
        return BLOCKS_GRIP;
    }

    /**
     * A DynamX file found in a pack
     */
    @Getter
    public static class PackFile {
        private final String name;
        private final InputStream inputStream;

        private PackFile(String name, InputStream inputStream) {
            this.name = name;
            this.inputStream = inputStream;
        }

        @Override
        public String toString() {
            return "PackFile{" + name + '}';
        }
    }
}
