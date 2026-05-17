package fr.dynamx.client.renders;

import fr.dynamx.client.renders.model.ModelObjArmor;
import fr.dynamx.client.renders.model.renderer.DxModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Client-side wiring of {@code DynamXItemArmor} into the 1.20.1 armor pipeline.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Lazily build a {@link ModelObjArmor} per {@link ArmorObject} (the per-pack
 *       {@link DxModelRenderer} is resolved through {@link DynamXContext#getDxModelRegistry()}).</li>
 *   <li>Expose it through {@code IClientItemExtensions#getHumanoidArmorModel} so that Forge's
 *       vanilla {@code HumanoidArmorLayer} routes rendering through
 *       {@link ModelObjArmor#renderToBuffer} (which uses {@code RenderType.armorCutoutNoCull(...)}
 *       for free via {@code ItemRenderer.getArmorFoilBuffer}).</li>
 *   <li>Forward the current {@code EquipmentSlot} + texture variant id to the model so the future
 *       scene-graph dispatch can pick the right pack parts.</li>
 * </ul>
 * <p>
 * TODO port:1.20.1 - once {@link fr.dynamx.client.renders.scene.node.ArmorNode} is unstubbed,
 * {@link ModelObjArmor#renderToBuffer} will dispatch through the scene graph and this class will no
 * longer need to know about textures (the {@code RenderType.armorCutoutNoCull} buffer is built by
 * the vanilla armor layer from {@code DynamXItemArmor.getArmorTexture}).
 */
public final class RenderDynamXArmor {

    private static final Map<ArmorObject<?>, ModelObjArmor> MODELS = new IdentityHashMap<>();

    private RenderDynamXArmor() {
    }

    /**
     * Returns (and lazily builds) the {@link ModelObjArmor} bound to the given armor pack object.
     * Must be called on the client thread.
     */
    @Nullable
    public static ModelObjArmor armorModelFor(ArmorObject<?> armorObject) {
        if (armorObject == null) {
            return null;
        }
        ModelObjArmor cached = MODELS.get(armorObject);
        if (cached != null) {
            return cached;
        }
        ModelPart root = bakeRoot();
        if (root == null) {
            return null;
        }
        DxModelRenderer model = resolveModel(armorObject);
        ModelObjArmor created = new ModelObjArmor(armorObject, model, root);
        MODELS.put(armorObject, created);
        return created;
    }

    /**
     * Resets the per-pack model cache. Call from the resource-reload pipeline to drop stale
     * {@link DxModelRenderer} references after the model registry has been rebuilt.
     */
    public static void resetCache() {
        MODELS.clear();
    }

    /**
     * Resolves the {@link DxModelRenderer} for the given armor pack. May return {@code null} if the
     * model has not been loaded yet (resource reload race); the caller falls back to the vanilla
     * humanoid skeleton in that case.
     */
    @Nullable
    private static DxModelRenderer resolveModel(ArmorObject<?> armorObject) {
        try {
            return DynamXContext.getDxModelRegistry().getModel(armorObject.getModel());
        } catch (Throwable ignored) {
            // TODO port:1.20.1 - the model registry can throw if called before pack-load completes.
            return null;
        }
    }

    /**
     * Bakes the {@link ModelPart} root used by {@link ModelObjArmor}. Uses the standard humanoid
     * armor layer definition so the part names match what Forge's {@code HumanoidArmorLayer}
     * expects.
     */
    @Nullable
    private static ModelPart bakeRoot() {
        try {
            LayerDefinition def = ModelObjArmor.createBodyLayer();
            return def.bakeRoot();
        } catch (Throwable t) {
            // TODO port:1.20.1 - in headless / server contexts bakeRoot may not be safe; bail out.
            return null;
        }
    }
}
