package fr.dynamx.client.renders.model.texture;

import com.mojang.blaze3d.platform.NativeImage;
import fr.dynamx.common.DynamXMain;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.Optional;

/**
 * Wraps a single texture (diffuse / normal / specular) attached to a material.
 * <p>
 * Two paths:
 * <ul>
 *   <li>If the texture exists in the Minecraft resource manager (vanilla / resource pack), {@link #getPath()}
 *       is used directly as the bind target.</li>
 *   <li>Otherwise, the texture is loaded from the originating .dnxpack ZIP via the
 *       {@link MaterialTexture#registerFromPack(InputStream)} helper and bound under a synthetic
 *       {@link ResourceLocation} returned by {@link #getEffectivePath()}.</li>
 * </ul>
 */
public class MaterialTexture {
    @Getter
    private final ResourceLocation path;
    @Getter
    private final String textureVariantName;
    @Getter
    private ResourceLocation effectivePath;
    private boolean attempted;

    public MaterialTexture(ResourceLocation path, String textureVariantName) {
        this.path = path;
        this.textureVariantName = textureVariantName;
        this.effectivePath = path;
    }

    /** Resource-manager check; called on render thread before uploadTexture. No-op safe. */
    public void loadTexture() {
        // Texture loading deferred to first render via uploadTexture.
    }

    /**
     * Ensures the texture is bindable. Tries the Minecraft resource manager first; falls back to
     * the pack's input-stream loader if a {@link PackTextureProvider} is set.
     */
    public void uploadTexture() {
        if (attempted) return;
        attempted = true;
        try {
            Optional<Resource> r = Minecraft.getInstance().getResourceManager().getResource(path);
            if (r.isPresent()) {
                // Vanilla resource manager will handle binding on demand.
                effectivePath = path;
                return;
            }
        } catch (Exception ignored) {
        }
        PackTextureProvider provider = PACK_PROVIDER;
        if (provider != null) {
            try (InputStream is = provider.open(path)) {
                if (is != null) registerFromPack(is);
            } catch (Exception e) {
                DynamXMain.log.warn("Failed to load pack texture " + path + ": " + e.getMessage());
            }
        }
    }

    private void registerFromPack(InputStream is) {
        try {
            NativeImage img = NativeImage.read(is);
            DynamicTexture dyn = new DynamicTexture(img);
            ResourceLocation rl = new ResourceLocation(path.getNamespace(),
                    "dnxpack/" + path.getPath().replace('/', '_').replace('.', '_'));
            Minecraft.getInstance().getTextureManager().register(rl, dyn);
            effectivePath = rl;
        } catch (Exception e) {
            DynamXMain.log.warn("Failed to register pack texture " + path + ": " + e.getMessage());
        }
    }

    /** Per-pack-file resolver for textures stored inside .dnxpack ZIPs. Set globally during model load. */
    @FunctionalInterface
    public interface PackTextureProvider {
        InputStream open(ResourceLocation textureLocation) throws Exception;
    }

    private static volatile PackTextureProvider PACK_PROVIDER;

    public static void setPackProvider(PackTextureProvider provider) {
        PACK_PROVIDER = provider;
    }
}
