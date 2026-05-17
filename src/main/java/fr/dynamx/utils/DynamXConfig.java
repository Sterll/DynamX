package fr.dynamx.utils;

import lombok.Getter;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DynamX configuration values.
 *
 * <p>Migrated to Forge {@link ForgeConfigSpec} for 1.20.1. Legacy public static fields are kept
 * so the rest of the codebase keeps reading them without change. They are populated from the
 * spec by {@link #bake()} on config load/reload events.</p>
 */
public class DynamXConfig {

    public static boolean syncPacks = false;

    public static boolean useUdp = true;
    public static boolean doUdpTimeOut = true;
    public static int udpPort = 25575;
    public static boolean usingProxy = false;
    public static boolean udpDebug = false;

    public static int vehiclesSyncTickRate = 1;
    public static int mountedVehiclesSyncTickRate = 1;
    public static int propsSyncTickRate = 2;

    public static int maxZoomOut = 20;
    public static int gearChangeDelay = 5;
    public static int blockCollisionRadius = 3;
    public static int maxComplexBlockBoxes = 8;

    public static int networkChunkComputeWarnTime = 40;

    public static boolean allowPlayersToMoveObjects = true;
    public static int[] allowedWrenchModes = {0, 2, 5};

    public static boolean disableItemTooltips = false;

    public static List<VerticalChunkPos> chunkDebugPoses = Collections.emptyList();
    public static boolean enableDebugTerrainManager = false;
    public static boolean ignoreDangerousTerrainErrors = false;

    public static int ragdollSpawnMinForce = -1;

    public static boolean disableSSLCertification = false;
    public static Set<String> ignoreCollisionEntities = new HashSet<>(Collections.singletonList("example.entity.*"));

    @Getter
    private static float masterSoundVolume = 0.8f;
    @Getter
    private static int maxSounds = 8;

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue CFG_SYNC_PACKS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> CFG_ALLOWED_WRENCH_MODES;

    private static final ForgeConfigSpec.BooleanValue CFG_USE_UDP;
    private static final ForgeConfigSpec.BooleanValue CFG_DO_UDP_TIMEOUT;
    private static final ForgeConfigSpec.IntValue CFG_UDP_PORT;
    private static final ForgeConfigSpec.BooleanValue CFG_USING_PROXY;
    private static final ForgeConfigSpec.BooleanValue CFG_UDP_DEBUG;

    private static final ForgeConfigSpec.IntValue CFG_MAX_ZOOM_OUT;
    private static final ForgeConfigSpec.BooleanValue CFG_DISABLE_ITEM_TOOLTIPS;

    private static final ForgeConfigSpec.BooleanValue CFG_ALLOW_MOVE_OBJECTS;
    private static final ForgeConfigSpec.IntValue CFG_RAGDOLL_FORCE;
    private static final ForgeConfigSpec.IntValue CFG_BLOCK_COLLISION_RADIUS;
    private static final ForgeConfigSpec.IntValue CFG_MAX_COMPLEX_BOXES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CFG_IGNORE_COLLISION_ENTITIES;

    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> CFG_CHUNK_DEBUG_X;
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> CFG_CHUNK_DEBUG_Y;
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> CFG_CHUNK_DEBUG_Z;
    private static final ForgeConfigSpec.BooleanValue CFG_DEBUG_TERRAIN;
    private static final ForgeConfigSpec.BooleanValue CFG_IGNORE_TERRAIN_ERRORS;
    private static final ForgeConfigSpec.BooleanValue CFG_DISABLE_SSL;

    private static final ForgeConfigSpec.DoubleValue CFG_VOLUME;
    private static final ForgeConfigSpec.IntValue CFG_MAX_SOUNDS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("Multiplayer");
        CFG_SYNC_PACKS = b.comment("If enabled, the server will send all content pack objects to the clients (only where there are differences)")
                .define("SyncContentPacks", false);
        CFG_ALLOWED_WRENCH_MODES = b.comment("Wrench modes allowed for players")
                .defineList("AllowedWrenchModes", Arrays.asList(0, 2, 5), o -> o instanceof Integer);
        b.pop();

        b.push("UDP");
        CFG_USE_UDP = b.comment("True to use (faster) UDP networking, false to use vanilla networking (TCP)")
                .define("UseUdpServer", true);
        CFG_DO_UDP_TIMEOUT = b.comment("True to automatically disconnect players when the udp connection cannot be established")
                .define("DoUdpTimeOut", true);
        CFG_UDP_PORT = b.comment("A port for the udp server, if enabled")
                .defineInRange("UdpPort", 25575, 2000, 65535);
        CFG_USING_PROXY = b.comment("If you have a proxy in front of your server")
                .define("HasProxy", false);
        CFG_UDP_DEBUG = b.comment("True to print debug for UDP connections")
                .define("PrintUdpDebug", false);
        b.pop();

        b.push("Visuals");
        CFG_MAX_ZOOM_OUT = b.comment("Max de-zoom in F5 view")
                .defineInRange("MaxZoomOut", 20, 0, 200);
        CFG_DISABLE_ITEM_TOOLTIPS = b.comment("Disables item tooltips showing pack information.")
                .define("DisableItemTooltips", false);
        b.pop();

        b.push("Physics");
        CFG_ALLOW_MOVE_OBJECTS = b.comment("Allow players in survival to move objects")
                .define("AllowPlayersToMoveObjects", true);
        CFG_RAGDOLL_FORCE = b.comment("The minimum force of collision to spawn player ragdolls. Set to -1 to disable it.")
                .defineInRange("RagdollSpawnMinForce", -1, -1, Integer.MAX_VALUE);
        CFG_BLOCK_COLLISION_RADIUS = b.comment("The radius of collision checking with DynamX blocks around players. Has an impact on game performance.")
                .defineInRange("BlockCollisionRadius2", 3, 0, 16);
        CFG_MAX_COMPLEX_BOXES = b.comment("The amount of detailed collisions per each complex block. If the block has more collisions, it will be a cube containing all collisions.")
                .defineInRange("MaxComplexBoxes", 8, 0, 100);
        CFG_IGNORE_COLLISION_ENTITIES = b.comment("A list of entity classes that should ignore collisions. Wildcard supported (*)")
                .defineList("IgnoreCollisionEntities", Collections.singletonList("example.entity.*"), o -> o instanceof String);
        b.pop();

        b.push("Debug");
        CFG_CHUNK_DEBUG_X = b.comment("X poses of chunks to debug")
                .defineList("ChunkDebugX", new ArrayList<>(), o -> o instanceof Integer);
        CFG_CHUNK_DEBUG_Y = b.comment("Y poses of chunks to debug")
                .defineList("ChunkDebugY", new ArrayList<>(), o -> o instanceof Integer);
        CFG_CHUNK_DEBUG_Z = b.comment("Z poses of chunks to debug")
                .defineList("ChunkDebugZ", new ArrayList<>(), o -> o instanceof Integer);
        CFG_DEBUG_TERRAIN = b.comment("Permits to debug terrain loading issues but may produce lag and instabilities")
                .define("UseDebugTerrainManager", false);
        CFG_IGNORE_TERRAIN_ERRORS = b.comment("Will try to prevent the game from crashing when there is a weird error in the terrain. Only enable this if you want server stability.")
                .define("IgnoreDangerousTerrainErrors", false);
        CFG_DISABLE_SSL = b.comment("Disables ssl certificates for dynamx.fr, may be a security breach for your computer, DO NOT disable it if you don't know what you are doing")
                .define("DisableSSLVerification", false);
        b.pop();

        b.push("Sounds");
        CFG_VOLUME = b.comment("The volume of DynamX sounds (engines...)")
                .defineInRange("Volume", 0.8d, 0.0d, 1.0d);
        CFG_MAX_SOUNDS = b.comment("The maximum amount of sounds DynamX can play at the same time")
                .defineInRange("MaxSounds", 8, 0, 64);
        b.pop();

        SPEC = b.build();
    }

    /**
     * Registers the common config spec and an event listener that mirrors the spec values
     * into the legacy static fields whenever the file is loaded or reloaded.
     */
    public static void register(IEventBus modEventBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
        modEventBus.addListener(DynamXConfig::onConfigChanged);
    }

    private static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            bake();
        }
    }

    public static void bake() {
        syncPacks = CFG_SYNC_PACKS.get();
        allowedWrenchModes = toIntArray(CFG_ALLOWED_WRENCH_MODES.get());

        useUdp = CFG_USE_UDP.get();
        doUdpTimeOut = CFG_DO_UDP_TIMEOUT.get();
        udpPort = CFG_UDP_PORT.get();
        usingProxy = CFG_USING_PROXY.get();
        udpDebug = CFG_UDP_DEBUG.get();

        maxZoomOut = CFG_MAX_ZOOM_OUT.get();
        disableItemTooltips = CFG_DISABLE_ITEM_TOOLTIPS.get();

        allowPlayersToMoveObjects = CFG_ALLOW_MOVE_OBJECTS.get();
        ragdollSpawnMinForce = CFG_RAGDOLL_FORCE.get();
        blockCollisionRadius = CFG_BLOCK_COLLISION_RADIUS.get();
        maxComplexBlockBoxes = CFG_MAX_COMPLEX_BOXES.get();
        ignoreCollisionEntities = new HashSet<>(CFG_IGNORE_COLLISION_ENTITIES.get());

        chunkDebugPoses = bakeChunkDebug();
        enableDebugTerrainManager = CFG_DEBUG_TERRAIN.get();
        ignoreDangerousTerrainErrors = CFG_IGNORE_TERRAIN_ERRORS.get();
        disableSSLCertification = CFG_DISABLE_SSL.get();

        masterSoundVolume = CFG_VOLUME.get().floatValue();
        maxSounds = CFG_MAX_SOUNDS.get();
    }

    private static List<VerticalChunkPos> bakeChunkDebug() {
        List<? extends Integer> xs = CFG_CHUNK_DEBUG_X.get();
        List<? extends Integer> ys = CFG_CHUNK_DEBUG_Y.get();
        List<? extends Integer> zs = CFG_CHUNK_DEBUG_Z.get();
        int n = Math.min(xs.size(), Math.min(ys.size(), zs.size()));
        if (n == 0) return Collections.emptyList();
        List<VerticalChunkPos> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(new VerticalChunkPos(xs.get(i), ys.get(i), zs.get(i)));
        }
        return out;
    }

    private static int[] toIntArray(List<? extends Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    public static void setMasterSoundVolume(float volume) {
        masterSoundVolume = volume;
        try {
            CFG_VOLUME.set((double) volume);
            CFG_VOLUME.save();
        } catch (IllegalStateException ignored) {
            // spec not loaded yet, volume will be applied at next bake()
        }
    }

    /**
     * Legacy entry point kept so callers that still pass a file path keep compiling. The
     * Forge config lifecycle now owns the file location and is registered via
     * {@link #register(IEventBus)}; this method is a no-op so defaults remain in place
     * until the {@link ModConfigEvent} fires {@link #bake()}.
     */
    public static void load(File file) {
        // no-op: defaults declared above are used until the spec is loaded
    }
}
