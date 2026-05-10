package fr.dynamx.utils.doc;

import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.common.contentpack.loader.PackFilePropertyData;
import fr.dynamx.common.contentpack.loader.SubInfoTypeAnnotationCache;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>TODO port:1.20.1 - The original DocGeneratorMain relied on the legacy 1.12 FML mod-discovery
 * pipeline ({@code ASMDataTable}, {@code ModCandidate}, {@code DirectoryDiscoverer},
 * {@code ContainerType}, {@code ModContainerFactory}, {@code ReflectionHelper.setPrivateValue}).
 * None of that exists in NeoForge 1.20.1.</p>
 *
 * <p>The replacement should walk the {@code @PackFileProperty}-annotated classes via the
 * {@code ModFileScanData} ({@code event.getMod().getOwningFile().getFile().getScanResult()}) or
 * via a Spongepowered {@code @AnnotationVisitor} plugin during mod setup. For now this entry point
 * is stubbed and only keeps the public {@code main} signature so addons that hand-launch it still
 * link.</p>
 */
public class DocGeneratorMain implements INamedObject {
    public static void main(String[] args) {
        Map<String, Class<?>> classesToLoad = new HashMap<>();
        // TODO port:1.20.1 - rewrite discovery using NeoForge's ModFileScanData / forgespi
        // and feed `classesToLoad` with classes annotated by @PackFileProperty.
        System.out.println("[DocGeneratorMain] discovery stubbed for 1.20.1 port - please port to ModFileScanData.");
        exportDocInLang(classesToLoad, "en_us");
        exportDocInLang(classesToLoad, "fr_fr");
    }

    private static void exportDocInLang(Map<String, Class<?>> classesToLoad, String lang) {
        System.out.println("=-=-=-=-=-=-=-=-=-=-=-=");
        System.out.println("Exporting doc in locale " + lang);
        long start = System.currentTimeMillis();
        DocLocale locale = new DocLocale();
        ContentPackDocGenerator.reset();
        locale.loadLocaleDataFiles(new File(new File(new File("run", "Doc"), "langs"), "doc_" + lang + ".lang"));
        File docDir = new File(new File("run", "Doc"), lang);
        for (Class<?> clazz : classesToLoad.values()) {
            System.out.println(clazz.getName());
            Map<String, PackFilePropertyData<?>> packFileProperties = SubInfoTypeAnnotationCache.getOrLoadData(clazz);
            ContentPackDocGenerator.generateDoc(locale, docDir, clazz, clazz.getSimpleName(), packFileProperties.values());
            System.out.println("Found " + packFileProperties.size() + " fields in " + clazz.getName());
        }
        System.out.println("Finished in " + (System.currentTimeMillis() - start) + "ms");
        System.out.println("=-=-=-=-=-=-=-=-=-=-=-=");
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getPackName() {
        return null;
    }
}
