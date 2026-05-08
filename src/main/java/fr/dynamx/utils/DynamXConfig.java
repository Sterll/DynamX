package fr.dynamx.utils;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DynamX configuration values.
 *
 * TODO port:1.20.1 - Migrate to NeoForge ModConfigSpec.
 * The Forge 1.12 Configuration API and ACsLib config are gone. This class currently
 * exposes the same public fields as before so the rest of the codebase keeps compiling
 * while we port; the load() method only assigns defaults until the new ModConfigSpec
 * binding is wired up.
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
    public static Set<String> ignoreCollisionEntities = new HashSet<>(Arrays.asList("example.entity.*"));

    @Getter
    @Setter
    private static float masterSoundVolume = 0.8f;
    @Getter
    private static int maxSounds = 8;

    /**
     * TODO port:1.20.1 - rewrite using NeoForge ModConfigSpec.
     * For now this is a no-op that keeps the defaults declared above.
     */
    public static void load(File file) {
        // intentionally empty: defaults are used until the ModConfigSpec is wired
    }
}
