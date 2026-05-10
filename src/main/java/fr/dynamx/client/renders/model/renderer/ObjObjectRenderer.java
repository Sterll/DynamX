package fr.dynamx.client.renders.model.renderer;

import fr.dynamx.client.renders.model.texture.TextureVariantData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.vecmath.Vector4f;
import java.util.HashMap;
import java.util.Map;

/**
 * Renderer for a single OBJ object.
 * <p>
 * TODO port:1.20.1 - OBJ loader is being dropped. This class is kept as a stub to preserve
 * external references; all rendering / VAO / VBO logic must be deleted or rewritten on top of
 * the GLTF + BufferBuilder pipeline. Most accessors here are no-ops.
 */
public class ObjObjectRenderer {
    // TODO port:1.20.1 - OBJ loader is being dropped; values kept to preserve external constant references
    public static final int COLOR_MAP_INDEX = 0x84C0;    // GL13.GL_TEXTURE0
    public static final int NORMAL_MAP_INDEX = 0x84C2;   // GL13.GL_TEXTURE2
    public static final int SPECULAR_MAP_INDEX = 0x84C3; // GL13.GL_TEXTURE3

    @Getter
    private final Map<Byte, VariantRenderData> modelRenderData = new HashMap<>();

    @Getter
    private final Object objObjectData; // TODO port:1.20.1 - was ObjObjectData; OBJ loader dropped

    @Getter
    @Setter
    private Vector4f objectColor = new Vector4f(1, 1, 1, 1);

    public ObjObjectRenderer(Object objObjectData) {
        // TODO port:1.20.1 - OBJ loader is being dropped
        this.objObjectData = objObjectData;
    }

    public void uploadVAO() {
        // TODO port:1.20.1 - OBJ loader is being dropped (was VAO/VBO + EBO upload)
    }

    public void clearVAO() {
        // TODO port:1.20.1 - OBJ loader is being dropped (was glDeleteBuffers)
    }

    public void setTextureVariants(ObjModelRenderer model, Object variants) {
        // TODO port:1.20.1 - OBJ loader is being dropped
    }

    public void render(ObjModelRenderer model, byte textureVariantID, boolean forceVanillaRender) {
        // TODO port:1.20.1 - OBJ loader is being dropped (was glDrawElements + material binding)
    }

    enum EnumGLPointer {
        VERTEX, TEX_COORDS, NORMAL
    }

    @Override
    public String toString() {
        return "ObjObjectRenderer{stubbed - OBJ loader dropped}";
    }

    @ToString
    @RequiredArgsConstructor
    public static class VariantRenderData {
        private final TextureVariantData baseVariant;
        private final TextureVariantData variant;
        private int vaoId = -1;
        private int vboPositions = -1;
        private int vboNormals = -1;
        private int vboTexCoords = -1;
        private int ebo = -1;

        public String getBaseVariant() {
            return baseVariant != null ? baseVariant.getName() : null;
        }

        public String getVariant() {
            return variant != null ? variant.getName() : null;
        }
    }
}
