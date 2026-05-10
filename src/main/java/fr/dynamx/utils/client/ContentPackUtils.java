package fr.dynamx.utils.client;

import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.common.contentpack.type.ObjectInfo;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static fr.dynamx.common.DynamXMain.log;
import static fr.dynamx.utils.DynamXConstants.ID;

/**
 * <p>TODO port:1.20.1 -
 * <ul>
 *   <li>{@code ModelLoader.setCustomStateMapper(block, mapper)} and the whole
 *       {@code IStateMapper}/{@code StateMapperBase}/{@code ModelResourceLocation} blockstate
 *       system are gone in 1.20.1 (replaced by {@code BlockStateModel}/JSON blockstates).
 *       The {@link #registerDynamXBlockStateMapper}/{@link #registerBlockWithNoModel} entry points
 *       are kept as no-op stubs so callers still compile; the equivalent feature must be
 *       reimplemented via {@code ModelEvent.RegisterModels} / dynamic JSON blockstates.</li>
 *   <li>{@code net.minecraft.client.resources.I18n#hasKey} -> {@link I18n#exists(String)}.</li>
 *   <li>{@code Minecraft.getMinecraft().getResourceManager().getResource(rl)} now returns an
 *       {@code Optional<Resource>}.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class ContentPackUtils {
    private static final Map<String, String> packToLangFile = new HashMap<>();

    public static void addMissingJSONs(IResourcesOwner item, ObjectInfo<?> objectInfo, File dynxDir, byte metadata) {
        if (dynxDir.isDirectory()) {
            File modelDir = new File(dynxDir, objectInfo.getPackName() + "/assets/" + ID + "/models/item");
            createItemJsonFile(modelDir, item.getJsonName(metadata), objectInfo.getIconFileName(metadata));
        }
    }

    private static void createItemJsonFile(File dir, String fileName, String iconName) {
        ResourceLocation location = new ResourceLocation(ID, "models/item/" + fileName + ".json");
        try {
            // TODO port:1.20.1 - getResource now returns Optional<Resource>; presence-check it.
            if (Minecraft.getInstance().getResourceManager().getResource(location).isEmpty()) {
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                writeInFile(new File(dir, fileName + ".json"),
                        "{ \"parent\": \"builtin/generated\", \"textures\": { \"layer0\": \"" + ID + ":icons/" + iconName + "\" }, \"display\": { "
                                + "\"thirdperson_lefthand\": { \"rotation\": [ 0, 90, -35 ], \"translation\": [ 0, 1.25, -2.5 ], \"scale\": [ 0.85, 0.85, 0.85 ] }, "
                                + "\"thirdperson_righthand\": { \"rotation\": [ 0, 90, -35 ], \"translation\": [ 0, 1.25, -2.5 ], \"scale\": [ 0.85, 0.85, 0.85 ] }, "
                                + "\"firstperson_lefthand\": { \"rotation\": [ 0, -45, 25 ], \"translation\": [ 0, 4, 2 ], \"scale\": [ 0.85, 0.85, 0.85 ] }, "
                                + "\"firstperson_righthand\": { \"rotation\": [ 0, -45, 25 ], \"translation\": [ 0, 4, 2 ], \"scale\": [ 0.85, 0.85, 0.85 ] }"
                                + " } }");
            }
        } catch (FileNotFoundException e) {
            // ignored: same semantics as legacy
        } catch (IOException e) {
            log.error("Failed to create item json file " + fileName, e);
        }
    }

    public static void createBlockJson(IResourcesOwner item, ObjectInfo<?> objectInfo, File dynxDir) {
        if (dynxDir.isDirectory()) {
            File modelDir = new File(dynxDir, objectInfo.getPackName() + "/assets/" + ID + "/blockstates");
            createBlockstateJsonFile(modelDir, item.getJsonName(0));
        }
    }

    private static void createBlockstateJsonFile(File dir, String fileName) {
        try {
            writeInFile(new File(dir, fileName + ".json"),
                    "{ \"variants\": { \"metadata=0\": { \"model\": \"" + ID + ":" + fileName + "\"}," +
                            "\"metadata=1\": { \"model\": \"" + ID + ":" + fileName + "\", \"y\": 90}," +
                            "\"metadata=2\": { \"model\": \"" + ID + ":" + fileName + "\", \"y\": 180}," +
                            "\"metadata=3\": { \"model\": \"" + ID + ":" + fileName + "\", \"y\": 270}" + " } }");
        } catch (IOException e) {
            log.error("Failed to create item json file " + fileName, e);
        }
    }

    private static void writeInFile(File file, String contents) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(contents);
            out.close();
            log.info(file.getName() + " not found so we created one");
        }
    }

    private static boolean existsCaseSensitive(File dir, String filename) {
        String[] files = dir.list();
        if (files == null) return false;
        for (String file : files)
            if (file.equals(filename))
                return true;
        return false;
    }

    public static File getPackLangFile(File dynxDir, String packName) throws IOException {
        File langPath = new File(dynxDir, packName + "/assets/" + ID + "/lang/");
        if (!langPath.exists()) {
            langPath.mkdirs();
        }
        if (packToLangFile.containsKey(packName)) {
            return new File(langPath, packToLangFile.get(packName));
        }
        File mcmetaFile = new File(dynxDir, packName + "/pack.mcmeta");
        File langUsFile = new File(langPath, "en_us.lang");
        File langUSFile = new File(langPath, "en_US.lang");
        if (mcmetaFile.exists()) {
            packToLangFile.put(packName, "en_us.lang");
            if (existsCaseSensitive(langPath, "en_US.lang")) {
                log.info("[AUTO-LANG] Renaming " + langUSFile.getPath() + " to " + langUsFile.getPath());
                if (!langUSFile.renameTo(langUsFile))
                    log.warn("[AUTO-LANG] Failed to rename");
                return langUSFile;
            } else {
                langUsFile.createNewFile();
                return langUsFile;
            }
        } else {
            packToLangFile.put(packName, "en_US.lang");
            if (existsCaseSensitive(langPath, "en_us.lang")) {
                log.info("[AUTO-LANG] Renaming " + langUsFile.getPath() + " to " + langUSFile.getPath());
                if (!langUsFile.renameTo(langUSFile))
                    log.warn("[AUTO-LANG] Failed to rename");
                return langUsFile;
            } else {
                langUSFile.createNewFile();
                return langUSFile;
            }
        }
    }

    public static void addMissingLangTranslation(File dynxDir, String packName, String translationKey, String translationValue) {
        // TODO port:1.20.1 - I18n.hasKey -> I18n.exists.
        if (I18n.exists(translationKey)) {
            return;
        }
        try {
            writeInLangFile(getPackLangFile(dynxDir, packName), translationKey + "=" + translationValue);
        } catch (IOException e) {
            log.error("Failed to add missing translation for " + packName + " : " + translationKey, e);
        }
    }

    public static void writeInLangFile(File langFile, String translation) throws IOException {
        BufferedReader inputStream = new BufferedReader(new InputStreamReader(Files.newInputStream(langFile.toPath())));
        if (inputStream.lines().noneMatch(s -> s.contains(translation.substring(0, translation.lastIndexOf("="))))) {
            BufferedWriter out = new BufferedWriter(new FileWriter(langFile, true));
            out.write(translation + "\n");
            out.close();
        }
        inputStream.close();
    }

    /**
     * <p>TODO port:1.20.1 - {@code ModelLoader.setCustomStateMapper} is gone. Stubbed.</p>
     */
    public static void registerBlockWithStateMapper(Block block, Object stateMapper) {
        // TODO port:1.20.1 - implement via ModelEvent.BakingCompleted or a JSON blockstate file.
    }

    public static void registerDynamXBlockStateMapper(IDynamXItem<BlockObject<?>> block) {
        // TODO port:1.20.1 - replace with a 1.20.1 blockstate generator / ModelEvent.
        registerBlockWithStateMapper((Block) block, null);
    }

    public static void registerBlockWithNoModel(Block block) {
        registerBlockWithStateMapper(block, null);
    }
}
