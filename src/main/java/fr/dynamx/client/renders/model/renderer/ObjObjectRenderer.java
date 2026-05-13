package fr.dynamx.client.renders.model.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.dynamx.client.renders.RenderFrame;
import fr.dynamx.client.renders.model.texture.MaterialTexture;
import fr.dynamx.client.renders.model.texture.TextureVariantData;
import fr.dynamx.common.objloader.data.Material;
import fr.dynamx.common.objloader.data.ObjObjectData;
import fr.dynamx.common.objloader.data.Vertex;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Renderer for a single OBJ object. Owns the vertex/index data of one {@code o ...} block
 * inside the OBJ file and is responsible for pushing those triangles to the active
 * {@link VertexConsumer} obtained from the thread-local {@link RenderFrame}.
 */
public class ObjObjectRenderer {
    @Getter
    private final Map<Byte, VariantRenderData> modelRenderData = new HashMap<>();

    @Getter
    private final ObjObjectData objObjectData;

    @Getter
    @Setter
    private Vector4f objectColor = new Vector4f(1, 1, 1, 1);

    private static final ResourceLocation MISSING_TEXTURE = new ResourceLocation("minecraft", "textures/misc/white.png");

    public ObjObjectRenderer(ObjObjectData objObjectData) {
        this.objObjectData = objObjectData;
    }

    public void uploadVAO() {
        // Immediate-mode rendering: nothing to upload up-front. Textures are loaded lazily
        // on first render via MaterialTexture.uploadTexture().
    }

    public void clearVAO() {
        // Immediate-mode rendering: no GL handles to release.
    }

    public void setTextureVariants(ObjModelRenderer model, Object variants) {
        // Texture variants are not used by the minimal port. Diffuse texture from MTL is used directly.
    }

    public void render(ObjModelRenderer model, byte textureVariantID, boolean forceVanillaRender) {
        if (objObjectData == null) return;
        Vertex[] vertices = objObjectData.getVertices();
        int[] indices = objObjectData.getIndices();
        if (vertices == null || indices == null || indices.length == 0) return;

        RenderFrame.Frame frame = RenderFrame.current();
        if (frame == null) return; // no PoseStack bound -> caller didn't set up a render frame

        PoseStack pose = frame.poseStack();
        MultiBufferSource buffers = frame.bufferSource();
        int packedLight = frame.packedLight();

        String[] perVertex = objObjectData.getMaterialForEachVertex();
        Map<String, Material.IndexPair> matMap = objObjectData.getMaterials();

        // Group triangles by material so we issue one quad per RenderType bind.
        // perVertex is sized to triangle count (one entry per face from the parser),
        // so its length equals indices.length / 3.
        int triCount = indices.length / 3;
        if (matMap == null || matMap.isEmpty() || perVertex == null || perVertex.length != triCount) {
            // No materials known: render everything with a single fallback white texture.
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(MISSING_TEXTURE));
            drawTriangles(pose, vc, vertices, indices, 0, indices.length, 1f, 1f, 1f, packedLight);
            return;
        }

        String activeMat = null;
        VertexConsumer vc = null;
        float r = 1f, g = 1f, b = 1f;
        for (int t = 0; t < triCount; t++) {
            String mat = perVertex[t];
            if (mat == null) mat = "_none_";
            if (!mat.equals(activeMat)) {
                activeMat = mat;
                Material material = model.getMaterials().get(mat);
                ResourceLocation tex = null;
                if (material != null && !material.diffuseTexture.isEmpty()) {
                    MaterialTexture mt = material.diffuseTexture.values().iterator().next();
                    mt.uploadTexture();
                    tex = mt.getEffectivePath();
                }
                if (tex == null) tex = MISSING_TEXTURE;
                vc = buffers.getBuffer(RenderType.entityCutoutNoCull(tex));
                if (material != null) {
                    r = material.diffuseColor.x;
                    g = material.diffuseColor.y;
                    b = material.diffuseColor.z;
                } else {
                    r = g = b = 1f;
                }
            }
            int base = t * 3;
            drawTriangle(pose, vc, vertices, indices, base, r, g, b, packedLight);
        }
    }

    private void drawTriangles(PoseStack pose, VertexConsumer vc, Vertex[] vertices, int[] indices,
                               int from, int count, float r, float g, float b, int packedLight) {
        int end = from + count;
        for (int i = from; i < end; i += 3) {
            drawTriangle(pose, vc, vertices, indices, i, r, g, b, packedLight);
        }
    }

    private void drawTriangle(PoseStack pose, VertexConsumer vc, Vertex[] vertices, int[] indices,
                              int base, float r, float g, float b, int packedLight) {
        if (base + 2 >= indices.length) return;
        int i0 = indices[base];
        int i1 = indices[base + 1];
        int i2 = indices[base + 2];
        if (i0 < 0 || i1 < 0 || i2 < 0 || i0 >= vertices.length || i1 >= vertices.length || i2 >= vertices.length) return;

        Vector3f n0 = vertices[i0].getNormal();
        Vector3f n1 = vertices[i1].getNormal();
        Vector3f n2 = vertices[i2].getNormal();

        emitVertex(pose, vc, vertices[i0].getPos(), vertices[i0].getTexCoords(), n0, r, g, b, packedLight);
        emitVertex(pose, vc, vertices[i1].getPos(), vertices[i1].getTexCoords(), n1, r, g, b, packedLight);
        emitVertex(pose, vc, vertices[i2].getPos(), vertices[i2].getTexCoords(), n2, r, g, b, packedLight);
    }

    private void emitVertex(PoseStack pose, VertexConsumer vc, Vector3f p, Vector2f uv, Vector3f n,
                            float r, float g, float b, int packedLight) {
        vc.vertex(pose.last().pose(), p.x, p.y, p.z)
                .color(r, g, b, 1.0f)
                .uv(uv.x, 1f - uv.y)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.last().normal(), n.x, n.y, n.z)
                .endVertex();
    }

    enum EnumGLPointer {
        VERTEX, TEX_COORDS, NORMAL
    }

    @Override
    public String toString() {
        return "ObjObjectRenderer{name=" + (objObjectData != null ? objObjectData.getName() : "null") + "}";
    }

    @ToString
    @RequiredArgsConstructor
    public static class VariantRenderData {
        private final TextureVariantData baseVariant;
        private final TextureVariantData variant;

        public String getBaseVariant() {
            return baseVariant != null ? baseVariant.getName() : null;
        }

        public String getVariant() {
            return variant != null ? variant.getName() : null;
        }
    }
}
