package fr.dynamx.utils.doc;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import fr.dynamx.common.DynamXMain;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>TODO port:1.20.1 -
 * <ul>
 *   <li>{@code @SideOnly(Side.CLIENT)} -> {@link OnlyIn @OnlyIn}{@code (Dist.CLIENT)}.</li>
 *   <li>{@code FMLCommonHandler.instance().loadLanguage(properties, stream)} is gone. NeoForge's
 *       language client uses {@code net.minecraft.client.resources.language.ClientLanguage}.
 *       For this doc generator we now read the .lang file straight (no FML pre-processing),
 *       which is sufficient for "$key=$value" entries.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class DocLocale {
    private static final Splitter SPLITTER = Splitter.on('=').limit(2);
    private static final Pattern PATTERN = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");
    Map<String, String> properties = Maps.newHashMap();
    private boolean unicode;

    public synchronized void loadLocaleDataFiles(File f) {
        this.properties.clear();
        System.out.println("Loading " + f);
        try {
            loadLocaleData(new FileInputStream(f));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.checkUnicode();
    }

    public boolean isUnicode() {
        return this.unicode;
    }

    private void checkUnicode() {
        this.unicode = false;
        int i = 0;
        int j = 0;

        for (String s : this.properties.values()) {
            int k = s.length();
            j += k;

            for (int l = 0; l < k; ++l) {
                if (s.charAt(l) >= 256) {
                    ++i;
                }
            }
        }

        float f = (float) i / (float) j;
        this.unicode = (double) f > 0.1D;
    }

    private void loadLocaleData(InputStream inputStreamIn) throws IOException {
        // TODO port:1.20.1 - FMLCommonHandler.instance().loadLanguage is gone. Read the file directly.
        for (String s : IOUtils.readLines(inputStreamIn, StandardCharsets.UTF_8)) {
            if (!s.isEmpty() && s.charAt(0) != '#') {
                String[] astring = Iterables.toArray(SPLITTER.split(s), String.class);

                if (astring != null && astring.length == 2) {
                    String s1 = astring[0];
                    String s2 = PATTERN.matcher(astring[1]).replaceAll("%$1s");
                    this.properties.put(s1, s2);
                }
            }
        }
    }

    private String translateKeyPrivate(String translateKey) {
        String s = this.properties.get(translateKey);
        if (s == null) {
            DynamXMain.log.error("[DOC] Translation for " + translateKey + " not found !");
            return translateKey;
        }
        return s;
    }

    public String format(String translateKey, Object... parameters) {
        String s = this.translateKeyPrivate(translateKey);

        try {
            return String.format(s, parameters);
        } catch (IllegalFormatException var5) {
            return "Format error: " + s;
        }
    }

    public boolean hasKey(String key) {
        return this.properties.containsKey(key);
    }
}
