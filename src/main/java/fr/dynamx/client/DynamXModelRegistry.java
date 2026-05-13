package fr.dynamx.client;

import com.modularmods.mcgltf.dynamx.MCglTF;
import fr.aym.acslib.ACsLib;
import fr.aym.acslib.api.services.ThreadedLoadingService;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.aym.acslib.services.impl.thrload.DynamXThreadedModLoader;
import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.EnumDxModelFormats;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.loader.InfoList;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import fr.dynamx.common.objloader.MTLLoader;
import fr.dynamx.common.objloader.OBJLoader;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static fr.dynamx.common.DynamXMain.log;

/**
 * The DynamX dx-model loader. All models should be registered here before DynamX pre-initialization.
 *
 * <p>TODO port:1.20.1 - 1.12 -> 1.20.1 surface changes:</p>
 * <ul>
 *   <li>{@code net.minecraft.util.ResourceLocation} package moved to {@code net.minecraft.resources}.</li>
 *   <li>{@code Minecraft.getMinecraft()} -> {@code Minecraft.getInstance()}.</li>
 *   <li>{@code ProgressManager} is gone in NeoForge 1.20.1 - replaced with a simple no-op.</li>
 *   <li>{@code SplashProgress} class gone, removed pause/resume calls.</li>
 *   <li>{@code DxItemModelLoader} / {@code DxModelRenderer} / {@code GltfModelRenderer} / {@code ObjModelRenderer} /
 *       {@code MissingObjModel} live in {@code client/renders/*} which the Phase 7 agent owns; we reference them by
 *       {@code Object} placeholders so this file compiles before Phase 7 lands.</li>
 * </ul>
 */
public class DynamXModelRegistry implements IPackInfoReloadListener {
    private static final int LOADER_POOL_SIZE = 6;

    // TODO port:1.20.1 - typed against client/renders/* once Phase 7 lands. Object placeholders for now.
    private static final Object OBJ_ITEM_MODEL_LOADER = null;
    private static final Map<DxModelPath, IModelTextureVariantsSupplier> MODELS_REGISTRY = new HashMap<>();
    private static final Map<ResourceLocation, Object> MODELS = new ConcurrentHashMap<>();
    private static final List<ResourceLocation> FAULTY_MODELS = new ArrayList<>();

    /**
     * A missing model rendered when the right model isn't found.
     * TODO port:1.20.1 - typed against ObjModelRenderer once Phase 7 lands.
     */
    public static final Object MISSING_MODEL = null;

    private static boolean REGISTRY_CLOSED;

    public void registerModel(DxModelPath location) {
        registerModel(location, null);
    }

    public void registerModel(DxModelPath location, IModelTextureVariantsSupplier customTextures) {
        if (REGISTRY_CLOSED) {
            throw new IllegalStateException("Model registry closed, you should register your model before DynamX pre-initialization");
        }
        if (!MODELS_REGISTRY.containsKey(location)) {
            MODELS_REGISTRY.put(location, customTextures);
            if (location.getFormat() == EnumDxModelFormats.GLTF) {
                MCglTF.getInstance().registerModel(location);
            }
        } else if (customTextures != null && customTextures.hasTextureVariants()) {
            IModelTextureVariantsSupplier previousSupplier = MODELS_REGISTRY.get(location);
            if (previousSupplier == null || !previousSupplier.hasTextureVariants()) {
                log.debug("Replacing model texture supplier of '" + location + "' from '" + previousSupplier + "' to '" + customTextures + "'");
                MODELS_REGISTRY.put(location, customTextures);
                if (location.getFormat() == EnumDxModelFormats.GLTF) {
                    MCglTF.getInstance().registerModel(location);
                }
            } else {
                DynamXErrorManager.addPackError(customTextures.getPackName(), "obj_duplicated_custom_textures", ErrorLevel.HIGH, location.getName(), "Duplicate registration");
            }
        }
    }

    public Object getModel(ResourceLocation name) {
        if (!MODELS.containsKey(name)) {
            if (!FAULTY_MODELS.contains(name)) {
                log.error("Dx model " + name + " isn't registered !");
                FAULTY_MODELS.add(name);
            }
            return MISSING_MODEL;
        }
        return MODELS.get(name);
    }

    @Deprecated
    public Object getModel(String name) {
        return getModel(new ResourceLocation(DynamXConstants.ID, String.format("models/%s", name)));
    }

    /**
     * Reloads all dx models, may take some time. Don't call this directly, use {@link DynamXLoadingTasks} instead.
     *
     * <p>TODO port:1.20.1 - body keeps the loader pipeline but skips actual obj/gltf renderer instantiation
     * until Phase 7 ports {@code client/renders/model/renderer/*}.</p>
     */
    public void reloadModels() {
        REGISTRY_CLOSED = true;
        FAULTY_MODELS.clear();
        DynamXContext.getDxModelDataCache().clear();
        DynamXErrorManager.getErrorManager().clear(DynamXErrorManager.MODEL_ERRORS);

        ThreadedLoadingService threadedLoadingService = ACsLib.getPlatform().provideService(ThreadedLoadingService.class);
        ExecutorService modelLoader = Executors.newScheduledThreadPool(LOADER_POOL_SIZE, new DynamXThreadedModLoader.DefaultThreadFactory("DnxModelLoader"));
        threadedLoadingService.addTask(ThreadedLoadingService.ModLoadingSteps.FINISH_LOAD, "model_load", () -> {
            try {
                List<Callable<?>> loadObjTasks = new ArrayList<>();
                for (Map.Entry<DxModelPath, IModelTextureVariantsSupplier> name : MODELS_REGISTRY.entrySet()) {
                    if (MODELS.containsKey(name.getKey().getModelPath())) {
                        continue;
                    }
                    loadObjTasks.add(() -> {
                        log.debug("Loading dx model " + name.getKey());
                        // TODO port:1.20.1 - call ObjModelRenderer.loadObjModel / new GltfModelRenderer once Phase 7 lands.
                        MODELS.put(name.getKey().getModelPath(), MISSING_MODEL);
                        return null;
                    });
                }
                long start = System.currentTimeMillis();
                modelLoader.invokeAll((List) loadObjTasks);
                log.info("Took " + (System.currentTimeMillis() - start) + " ms to load " + loadObjTasks.size() + " dx models");

                long time = System.currentTimeMillis();
                while (Minecraft.getInstance().getTextureManager() == null) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                DynamXMain.log.debug("Tex manager wait took " + (System.currentTimeMillis() - time) + " ms");

                List<Callable<?>> loadTexturesTasks = new ArrayList<>();
                OBJLoader.getMtlLoaders().forEach(mtlLoader -> loadTexturesTasks.add(() -> {
                    mtlLoader.loadTextures();
                    return null;
                }));
                start = System.currentTimeMillis();
                modelLoader.invokeAll((List) loadTexturesTasks);
                log.info("Took " + (System.currentTimeMillis() - start) + " ms to load " + loadTexturesTasks.size() + " materials");
                modelLoader.shutdown();

                DynamXObjectLoaders.ARMORS.getInfos().values().forEach(ArmorObject::initArmorModel);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, () -> {
            // TODO port:1.20.1 - ProgressManager.push/step/pop is gone. No-op for now.
            log.info("Loading GLTF models...");
            long start = System.currentTimeMillis();
            try {
                MCglTF.getInstance().createShaderSkinningProgram();
                MCglTF.getInstance().reloadModels();
            } catch (Exception e) {
                log.fatal("Exception loading GLTF models", e);
            }
            log.info("MCgLTF took " + (System.currentTimeMillis() - start) + " ms to load " + MCglTF.lookup.size() + " gltf models");
            log.info("Loading model textures...");
            synchronized (OBJLoader.getMtlLoaders()) {
                OBJLoader.getMtlLoaders().forEach(MTLLoader::uploadTextures);
                OBJLoader.getMtlLoaders().clear();
            }
            if (ClientEventHandler.MC.level != null)
                uploadVAOs();
            DynamXLoadingTasks.endTask(DynamXLoadingTasks.MODEL);
        });
    }

    /**
     * <p>TODO port:1.20.1 - body stubbed: requires DxModelRenderer (Phase 7).</p>
     */
    public void uploadVAOs() {
        log.info("Loading model vaos...");
        long t1 = System.currentTimeMillis();
        // TODO port:1.20.1 - MODELS.values().forEach(DxModelRenderer::uploadVAOs);
        DynamXMain.log.info("VAO upload took " + (System.currentTimeMillis() - t1) + "ms");
    }

    public Object getItemRenderer() {
        return OBJ_ITEM_MODEL_LOADER;
    }

    public int getLoadedModelCount() {
        return MODELS_REGISTRY.size();
    }

    @Override
    public void onPackInfosReloaded() {
        MODELS_REGISTRY.keySet().removeIf(path -> path.getPackLocations().get(0).getPackType() != ContentPackType.BUILTIN);
        REGISTRY_CLOSED = false;
        for (InfoList<?> infoLoader : DynamXObjectLoaders.getInfoLists()) {
            for (INamedObject namedObject : infoLoader.getInfos().values()) {
                if (namedObject instanceof IModelPackObject && ((IModelPackObject) namedObject).shouldRegisterModel()) {
                    // TODO port:1.20.1 - IModelTextureVariantsSupplier was implemented by every pack
                    //  info in 1.12 via the now-stubbed model pipeline. Until that interface is
                    //  re-implemented in Phase 5/8, skip pack infos that don't supply variants.
                    if (!(namedObject instanceof IModelTextureVariantsSupplier)) {
                        continue;
                    }
                    DxModelPath modelPath = DynamXUtils.getModelPath(namedObject.getPackName(), ((IModelPackObject) namedObject).getModel());
                    registerModel(modelPath, (IModelTextureVariantsSupplier) namedObject);
                }
            }
        }
        REGISTRY_CLOSED = true;
        log.info("Registered " + getLoadedModelCount() + " dx models");
    }
}
