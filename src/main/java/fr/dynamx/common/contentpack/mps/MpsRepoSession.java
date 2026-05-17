package fr.dynamx.common.contentpack.mps;

import fr.dynamx.DynamX;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * One MPS-protected repository inside a content pack. Holds the per-resource AES keys
 * (decoded from the remote {@code .desc} file) and the cached remote payload bytes
 * (the JAR served by {@code router.php}).
 *
 * <p>Lifecycle:
 * <ol>
 *     <li>{@link #fetchAndDecrypt()} - HTTP-GET the remote URL, extract the {@code .desc},
 *     decrypt it with {@link MpsCrypto#deriveRepoKey(String)} and parse the per-file key map.</li>
 *     <li>{@link #getRemoteEntry(String)} - return the (still-encrypted) bytes from the
 *     remote JAR for a given resource path.</li>
 *     <li>{@link #getResourceKey(String)} - return the AES key for a given resource path.</li>
 * </ol>
 */
public class MpsRepoSession {

    private final String packName;
    private final String repoUrl;
    private final Map<String, byte[]> remoteEntries = new HashMap<>();
    private final Map<String, String> fileKeys = new HashMap<>();
    private String repoId;
    private String mainHash;
    private String signatureStore;
    private String resourcesDomains;
    private boolean ready;

    public MpsRepoSession(String packName, String repoUrl) {
        this.packName = packName;
        this.repoUrl = repoUrl;
    }

    public boolean isReady() {
        return ready;
    }

    public String getPackName() {
        return packName;
    }

    /**
     * Downloads the remote payload (a JAR), extracts every entry into memory and
     * decrypts the {@code <target>.desc} entry to populate the per-file key map.
     */
    public void fetchAndDecrypt() throws IOException {
        String descName = extractTargetName();
        byte[] payload = httpGet(repoUrl);
        DynamX.LOGGER.info("[MPS] Downloaded {} bytes for pack {} ({})", payload.length, packName, repoUrl);

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(payload))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.isDirectory()) {
                    continue;
                }
                remoteEntries.put(e.getName(), readAll(zis));
            }
        }
        byte[] descBytes = remoteEntries.get(descName + ".desc");
        if (descBytes == null) {
            // Fallback: search any *.desc entry
            for (Map.Entry<String, byte[]> entry : remoteEntries.entrySet()) {
                if (entry.getKey().endsWith(".desc")) {
                    descBytes = entry.getValue();
                    DynamX.LOGGER.info("[MPS] Using fallback desc entry {} for pack {}", entry.getKey(), packName);
                    break;
                }
            }
        }
        if (descBytes == null) {
            throw new IOException("No .desc entry in remote payload for pack " + packName + " (expected " + descName + ".desc)");
        }
        String repoKey = MpsCrypto.deriveRepoKey(repoUrl);
        DynamX.LOGGER.debug("[MPS] Derived repo key for {}: {}", packName, repoKey);
        byte[] decryptedDesc;
        try {
            decryptedDesc = MpsCrypto.decryptAes(repoKey, descBytes);
        } catch (Exception ex) {
            throw new IOException("Failed to decrypt .desc for pack " + packName, ex);
        }
        parseDesc(decryptedDesc);
        if (!verifyDescriptor()) {
            throw new IOException("MPS descriptor for pack " + packName + " failed integrity check (missing headers or no resource keys).");
        }
        ready = true;
        DynamX.LOGGER.info("[MPS] Pack {} ready (id={}), {} encrypted resources tracked", packName, repoId, fileKeys.size());
    }

    private String extractTargetName() {
        // URL of the form: .../router.php?mod_version=...&target=BASE64
        int idx = repoUrl.indexOf("target=");
        if (idx < 0) {
            return packName;
        }
        String tail = repoUrl.substring(idx + "target=".length());
        int amp = tail.indexOf('&');
        if (amp >= 0) {
            tail = tail.substring(0, amp);
        }
        int space = tail.indexOf(' ');
        if (space >= 0) {
            tail = tail.substring(0, space);
        }
        return tail;
    }

    private void parseDesc(byte[] decrypted) {
        try (Scanner sc = new Scanner(new ByteArrayInputStream(decrypted), StandardCharsets.UTF_8)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (!line.contains("=")) {
                    continue;
                }
                String[] sp = line.split("=", 2);
                String head = sp[0];
                String value = sp[1];
                switch (head) {
                    case "Main":
                        mainHash = value;
                        break;
                    case "SignatureStore":
                        signatureStore = value;
                        break;
                    case "Id":
                        repoId = value;
                        break;
                    case "ResourcesDomains":
                        resourcesDomains = value;
                        break;
                    default:
                        fileKeys.put(head, value);
                        break;
                }
            }
        }
    }

    /**
     * Best-effort integrity check on the decoded .desc payload. The original
     * ModProtectionLib used the {@code Main} entry as a SHA-256 fingerprint of the
     * pack metadata and {@code SignatureStore} as the RSA-signed key bundle.
     *
     * TODO port:1.20.1 - The ACsLib {@code RepositoryInformation} validator that
     *  performed the cryptographic signature check is not ported yet. For now we
     *  only verify that the mandatory descriptor headers were present after
     *  decryption, which catches truncated or wrong-key payloads.
     */
    public boolean verifyDescriptor() {
        return mainHash != null && repoId != null && !fileKeys.isEmpty();
    }

    public String getRepoId() {
        return repoId;
    }

    public String getMainHash() {
        return mainHash;
    }

    public String getSignatureStore() {
        return signatureStore;
    }

    public String getResourcesDomains() {
        return resourcesDomains;
    }

    /**
     * @return The (still-encrypted) remote bytes for a resource entry, or null if absent.
     */
    public byte[] getRemoteEntry(String entryName) {
        return remoteEntries.get(entryName);
    }

    /**
     * @return The 16-character AES key for a resource path, or null if the resource
     *     is not MPS-protected.
     */
    public String getResourceKey(String resourceName) {
        return fileKeys.get(resourceName);
    }

    public Map<String, String> getAllKeys() {
        return Collections.unmodifiableMap(fileKeys);
    }

    private static byte[] httpGet(String url) throws IOException {
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);
        conn.setRequestProperty("User-Agent", "DynamX-MPS/4.2.0");
        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("MPS HTTP " + code + " on " + url);
        }
        try (InputStream is = conn.getInputStream()) {
            return readAll(is);
        }
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
