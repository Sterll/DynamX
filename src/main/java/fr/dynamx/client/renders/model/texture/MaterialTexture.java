package fr.dynamx.client.renders.model.texture;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

/**
 * Wraps a single texture (diffuse / normal / specular) attached to a material.
 * <p>
 * TODO port:1.20.1 - threaded/async texture loading via ACsLib's ThreadedTexture is not available yet.
 * Both {@link #loadTexture(TextureManager)} and {@link #uploadTexture(TextureManager)} are stubbed
 * and must be reimplemented on top of the 1.20.1 TextureManager + AbstractTexture API.
 */
@RequiredArgsConstructor
@ToString
public class MaterialTexture {
    @Getter
    private final ResourceLocation path;
    @Getter
    private final String textureVariantName;
    @Getter
    private int glTextureId;

    public void loadTexture(TextureManager man) {
        // TODO port:1.20.1 - replace ACsLib ThreadedTexture with 1.20.1 TextureManager.register / SimpleTexture flow
    }

    public void uploadTexture(TextureManager man) {
        // TODO port:1.20.1 - replace ACsLib ThreadedTexture upload with 1.20.1 AbstractTexture upload pipeline
    }
}
