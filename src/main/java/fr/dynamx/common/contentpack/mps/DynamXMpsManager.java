package fr.dynamx.common.contentpack.mps;

import fr.dynamx.DynamX;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Orchestrates MPS-protected pack initialization and resource decryption for the
 * 1.20.1 port of DynamX. Replaces the 1.12 {@code fr.aym.mps.ModProtectionSystem}
 * pipeline.
 *
 * <p>The only mode currently supported is {@code PACK_URL_V1} (the format used by
 * the Dartcher pack and the live {@code dynamx.fr/mps/} server). Local-only
 * encryption variants ({@code PACK_DIR_V*}, launcher-encoding) are out of scope
 * for now.
 *
 * <p>Public API:
 * <ul>
 *     <li>{@link #registerPack(String, File)} - call once per discovered pack to
 *     parse {@code MpsRepositories.dnx} and initialise the remote session.</li>
 *     <li>{@link #tryReadResource(String, ResourceLocation)} - returns decrypted
 *     bytes if the resource is MPS-protected; returns {@code null} otherwise so
 *     callers can fall back to the plain ZIP lookup.</li>
 * </ul>
 */
public final class DynamXMpsManager {

    private static final String REPOSITORIES_FILE = "MpsRepositories.dnx";

    private static final DynamXMpsManager INSTANCE = new DynamXMpsManager();

    public static DynamXMpsManager get() {
        return INSTANCE;
    }

    private final Map<String, MpsRepoSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, File> packFiles = new ConcurrentHashMap<>();
    private final Map<String, byte[]> decryptedCache = new ConcurrentHashMap<>();

    private DynamXMpsManager() {
    }

    /**
     * Inspects the given pack file. If it contains a {@code MpsRepositories.dnx} entry,
     * a remote session is fetched and prepared for later resource lookups.
     */
    public void registerPack(String packName, File packFile) {
        if (packFile == null || !packFile.isFile()) {
            return;
        }
        String packPath = packFile.getAbsolutePath();
        try (ZipFile zip = new ZipFile(packFile)) {
            ZipEntry entry = zip.getEntry(REPOSITORIES_FILE);
            if (entry == null) {
                return;
            }
            String line;
            try (InputStream is = zip.getInputStream(entry)) {
                line = new String(is.readAllBytes()).trim();
            }
            // Format: "<url> <type>" e.g. "https://.../router.php?... url"
            int sep = line.lastIndexOf(' ');
            String url = sep > 0 ? line.substring(0, sep) : line;
            String type = sep > 0 ? line.substring(sep + 1).trim() : "url";
            if (!"url".equalsIgnoreCase(type)) {
                DynamX.LOGGER.warn("[MPS] Pack {} uses unsupported repo type '{}' - skipping", packName, type);
                return;
            }
            MpsRepoSession session = new MpsRepoSession(packName, url);
            session.fetchAndDecrypt();
            sessions.put(packName, session);
            packFiles.put(packName, packFile);
            DynamX.LOGGER.info("[MPS] Registered MPS pack '{}' from {}", packName, packPath);
        } catch (Exception e) {
            DynamX.LOGGER.error("[MPS] Failed to register pack '{}' for MPS: {}", packName, e.getMessage(), e);
        }
    }

    /**
     * Attempts to read {@code resource} as an MPS-protected file inside {@code packName}.
     *
     * @return Decrypted bytes wrapped in an InputStream, or {@code null} if the resource
     *     is not protected or this pack has no MPS session.
     */
    @Nullable
    public InputStream tryReadResource(String packName, ResourceLocation resource) {
        MpsRepoSession session = sessions.get(packName);
        if (session == null || !session.isReady()) {
            return null;
        }
        String assetPath = "assets/" + resource.getNamespace() + "/" + resource.getPath();
        String cacheKey = packName + "::" + assetPath;
        byte[] cached = decryptedCache.get(cacheKey);
        if (cached != null) {
            return new ByteArrayInputStream(cached);
        }
        String key = session.getResourceKey(assetPath);
        if (key == null) {
            return null;
        }
        try {
            byte[] decrypted = decryptResource(packName, session, assetPath, key);
            if (decrypted != null) {
                decryptedCache.put(cacheKey, decrypted);
                dumpForDebug(packName, assetPath, decrypted);
                return new ByteArrayInputStream(decrypted);
            }
        } catch (Exception ex) {
            DynamX.LOGGER.error("[MPS] Failed to decrypt {} from pack {}: {}", assetPath, packName, ex.getMessage(), ex);
        }
        return null;
    }

    private byte[] decryptResource(String packName, MpsRepoSession session, String assetPath, String key) throws Exception {
        byte[] remote = session.getRemoteEntry(assetPath);
        if (remote == null) {
            DynamX.LOGGER.warn("[MPS] No remote entry for {} in pack {}", assetPath, packName);
            return null;
        }
        byte[] part = readLocalPart(packName, assetPath);
        byte[] combined;
        if (part != null) {
            combined = MpsCrypto.reconcile(part, remote);
        } else {
            // Some resources have no .part (only remote bytes need decrypting)
            combined = remote;
        }
        return MpsCrypto.decryptAes(key, combined);
    }

    @Nullable
    private byte[] readLocalPart(String packName, String assetPath) throws IOException {
        File packFile = resolvePackFile(packName);
        if (packFile == null) {
            return null;
        }
        try (ZipFile zip = new ZipFile(packFile)) {
            ZipEntry entry = zip.getEntry(assetPath + ".part");
            if (entry == null) {
                return null;
            }
            try (InputStream is = zip.getInputStream(entry)) {
                return is.readAllBytes();
            }
        }
    }

    @Nullable
    private File resolvePackFile(String packName) {
        return packFiles.get(packName);
    }

    /**
     * Returns true if the pack has been registered as MPS-protected (regardless of
     * whether its session is ready).
     */
    public boolean isMpsPack(String packName) {
        return sessions.containsKey(packName);
    }

    public Map<String, MpsRepoSession> getSessionsSnapshot() {
        return new HashMap<>(sessions);
    }

    private void dumpForDebug(String packName, String assetPath, byte[] data) {
        try {
            File dir = new File("mps_debug_dumps");
            if (!dir.exists() && !dir.mkdirs()) {
                return;
            }
            String safe = (packName + "__" + assetPath).replace('/', '_').replace('\\', '_').replace(':', '_');
            File out = new File(dir, safe);
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
                fos.write(data);
            }
            DynamX.LOGGER.info("[MPS-DEBUG] Dumped {} bytes -> {}", data.length, out.getAbsolutePath());
        } catch (Exception ignored) {
        }
    }
}
