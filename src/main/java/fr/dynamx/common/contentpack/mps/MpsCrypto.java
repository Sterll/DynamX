package fr.dynamx.common.contentpack.mps;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Crypto primitives for the DynamX Mod Protection System (MPS) re-implementation.
 *
 * <p>This is a 1.20.1 re-implementation of the algorithms originally located in
 * {@code fr.aym.mps.utils.MpsUtils} and {@code fr.aym.mps.impl.EncryptedMPSResourceLoader}
 * (ModProtectionLib 1.5.3, jar in {@code libs/}). The original loader has hard
 * dependencies on Forge 1.12 ({@code LaunchClassLoader}, {@code FMLCommonHandler},
 * etc.) so we can't reuse it directly - instead we reimplement only the resource
 * decryption path.
 */
public final class MpsCrypto {

    private static final int KEY_LENGTH = 16;
    private static final int CYCLE_C_LENGTH = 912;
    private static final int CYCLE_I_LENGTH = 16;

    private MpsCrypto() {
    }

    /**
     * Derives the master key from the configured MPS access key (a base64-encoded
     * blob of the form {@code key-suffix}).
     */
    public static String deriveMasterKey(String mpsAccessKey) {
        String decoded = new String(Base64.getDecoder().decode(mpsAccessKey.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        int dash = decoded.indexOf('-');
        return dash >= 0 ? decoded.substring(0, dash) : decoded;
    }

    /**
     * Reconstructs the encrypted payload from the local {@code .part} bytes and the remote
     * resource bytes. Mirrors {@code EncryptedMPSResourceLoader.reconcile(bc, bi, 912, 16)}.
     *
     * <p>In each 928-byte cycle (16 + 912), the first 16 bytes come from {@code remote} and
     * the next 912 bytes come from {@code part}. The caller is responsible for passing the
     * correct sources.
     */
    public static byte[] reconcile(byte[] part, byte[] remote) {
        byte[] out = new byte[part.length + remote.length];
        int iIndex = 0;
        int cIndex = 0;
        for (int i = 0; i < out.length; i++) {
            int row = i % (CYCLE_I_LENGTH + CYCLE_C_LENGTH);
            if (row < CYCLE_I_LENGTH && iIndex < remote.length) {
                out[i] = remote[iIndex++];
            } else if (cIndex < part.length) {
                out[i] = part[cIndex++];
            } else {
                out[i] = remote[iIndex++];
            }
        }
        return out;
    }

    /**
     * AES-decrypts {@code input} with the given 16-character key. RSA signature
     * verification is currently skipped (the original implementation made it optional
     * via {@code signatureStore != null}).
     */
    public static byte[] decryptAes(String key, byte[] input) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(input);
    }

    /**
     * Computes a hex-encoded SHA-256 digest of the given bytes. Used to fingerprint
     * downloaded MPS payloads and compare against the {@code Main} hash recorded in
     * the .desc.
     */
    public static String sha256Hex(byte[] data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable on this JVM", e);
        }
    }

    /**
     * Returns {@code true} if the SHA-256 of {@code data} matches the (hex-encoded)
     * {@code expectedHash}. A {@code null} or empty {@code expectedHash} is treated
     * as a non-validating descriptor and short-circuits to {@code true}.
     *
     * TODO port:1.20.1 - ModProtectionLib stored the Main hash as a base64 RSA-signed
     *  blob, not a raw hex digest. Until the signature pipeline is ported we only
     *  validate as a defense against truncated downloads.
     */
    public static boolean matchesExpectedHash(byte[] data, String expectedHash) {
        if (expectedHash == null || expectedHash.isEmpty()) {
            return true;
        }
        return sha256Hex(data).equalsIgnoreCase(expectedHash);
    }

    /**
     * Derives the 16-character per-repository key from the original repository URL.
     * Mirrors {@code EncryptedMPSResourceLoader.getK(url, null)} for the
     * "URL mode" (no launcher-encoding path component).
     */
    public static String deriveRepoKey(String originalUrl) {
        StringBuilder keys = new StringBuilder();
        String burl = Base64.getUrlEncoder().encodeToString(originalUrl.getBytes(StandardCharsets.UTF_8));
        int alpha = burl.length() / KEY_LENGTH;
        if (alpha < 1) {
            throw new IllegalArgumentException("URL too short to derive MPS key: " + originalUrl);
        }
        for (int i = 0; i < KEY_LENGTH; i++) {
            keys.append(burl, i * alpha, i * alpha + 1);
        }
        return keys.toString();
    }
}
