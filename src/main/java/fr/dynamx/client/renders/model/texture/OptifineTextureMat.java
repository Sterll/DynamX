package fr.dynamx.client.renders.model.texture;

import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Optifine multi-texture support shim.
 * <p>
 * TODO port:1.20.1 - OBJ loader is being dropped and ACsLib ThreadedTexture is not available;
 * this Optifine hook is fully stubbed. It will be revisited once the GLTF material pipeline is wired
 * to the 1.20.1 texture stack (and if Optifine/Oculus support is still required).
 */
public class OptifineTextureMat extends SimpleTexture {
    private final Object material;
    private final String textureVariantName;

    public OptifineTextureMat(Object material, ResourceLocation textureResourceLocation, String textureVariantName) {
        super(textureResourceLocation);
        this.material = material;
        this.textureVariantName = textureVariantName;
    }

    // TODO port:1.20.1 - reimplement Optifine MultiTexID hook against the 1.20.1 texture pipeline
}
