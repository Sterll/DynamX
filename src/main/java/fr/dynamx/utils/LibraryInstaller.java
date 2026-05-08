package fr.dynamx.utils;

import fr.dynamx.DynamX;
import lombok.Setter;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

/**
 * Library installer (downloads optional companion jars at runtime).
 *
 * TODO port:1.20.1 - The 1.12 ACsLib SSLHelper integration is gone.
 * For 1.20.1 we either drop runtime library installation entirely (preferred,
 * since gradle/jij handles dependencies) or we re-implement the certificate
 * pinning ourselves. Currently uses the JVM default SSL context.
 */
public class LibraryInstaller {
    @Setter
    private static SSLContext dynamXSSLContext;

    public static boolean loadACsGuis(File directory, String defaultACsGuisVersion) {
        // TODO port:1.20.1 - ACsGuis library does not yet exist for 1.20.1
        DynamX.LOGGER.warn("ACsGuis loader not implemented for 1.20.1 yet (called for version {})", defaultACsGuisVersion);
        return false;
    }

    public static void download(URL from, File to) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) from.openConnection();
        if (connection instanceof HttpsURLConnection && dynamXSSLContext != null) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(dynamXSSLContext.getSocketFactory());
        }
        try (InputStream in = new BufferedInputStream(connection.getInputStream());
             FileOutputStream out = new FileOutputStream(to)) {
            byte[] buf = new byte[1024];
            int n;
            while (-1 != (n = in.read(buf))) {
                out.write(buf, 0, n);
            }
        }
    }

    public static SSLContext getDynamXSSLContext() {
        if (dynamXSSLContext == null) {
            throw new IllegalStateException("SSLContext not initialized");
        }
        return dynamXSSLContext;
    }

    public static void configureSsslContext() {
        // TODO port:1.20.1 - reimplement custom CA loading (DYNAMX_CERT/DYNAMX_AUX_CERT)
        // using the JDK SSLContext APIs once we drop the ACsLib dependency.
        try {
            dynamXSSLContext = SSLContext.getDefault();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SSLContext error", e);
        }
    }
}
