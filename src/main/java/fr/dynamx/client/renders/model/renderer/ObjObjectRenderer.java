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

    private static final java.util.Set<String> DUMPED_OBJECTS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> RENDER_COUNTERS = new java.util.concurrent.ConcurrentHashMap<>();
    private static long lastCounterDump = System.currentTimeMillis();
    private static final java.util.Set<String> DUMPED_TEXTURES = java.util.concurrent.ConcurrentHashMap.newKeySet();

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

        // TODO port:1.20.1 - TEMP DIAGNOSTIC: count renders per second per object + one-shot geometry dump
        String diagName = objObjectData.getName();
        if (diagName != null) {
            RENDER_COUNTERS.computeIfAbsent(diagName, k -> new java.util.concurrent.atomic.AtomicInteger()).incrementAndGet();
            long now = System.currentTimeMillis();
            if (now - lastCounterDump > 2000) {
                synchronized (RENDER_COUNTERS) {
                    if (now - lastCounterDump > 2000) {
                        StringBuilder sb = new StringBuilder("renders/2s: ");
                        for (var e : RENDER_COUNTERS.entrySet()) sb.append(e.getKey()).append("=").append(e.getValue().getAndSet(0)).append(" ");
                        org.apache.logging.log4j.LogManager.getLogger("DynamX-ObjDump").info(sb.toString());
                        lastCounterDump = now;
                    }
                }
            }
        }
        if (diagName != null && DUMPED_OBJECTS.add(diagName)) {
            int triCountDbg = indices.length / 3;
            float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
            int nanCount = 0;
            for (Vertex v : vertices) {
                Vector3f p = v.getPos();
                if (Float.isNaN(p.x) || Float.isNaN(p.y) || Float.isNaN(p.z)
                        || Float.isInfinite(p.x) || Float.isInfinite(p.y) || Float.isInfinite(p.z)) {
                    nanCount++;
                    continue;
                }
                if (p.x < minX) minX = p.x; if (p.x > maxX) maxX = p.x;
                if (p.y < minY) minY = p.y; if (p.y > maxY) maxY = p.y;
                if (p.z < minZ) minZ = p.z; if (p.z > maxZ) maxZ = p.z;
            }
            int oobIdx = 0;
            for (int idx : indices) {
                if (idx < 0 || idx >= vertices.length) oobIdx++;
            }
            // UV & Normal stats
            float minU = Float.POSITIVE_INFINITY, minV = Float.POSITIVE_INFINITY;
            float maxU = Float.NEGATIVE_INFINITY, maxV = Float.NEGATIVE_INFINITY;
            int zeroUV = 0, zeroNormal = 0;
            float avgNy = 0;
            for (Vertex v : vertices) {
                org.joml.Vector2f uv = v.getTexCoords();
                org.joml.Vector3f nn = v.getNormal();
                if (uv.x == 0 && uv.y == 0) zeroUV++;
                if (uv.x < minU) minU = uv.x; if (uv.x > maxU) maxU = uv.x;
                if (uv.y < minV) minV = uv.y; if (uv.y > maxV) maxV = uv.y;
                if (nn.x == 0 && nn.y == 0 && nn.z == 0) zeroNormal++;
                avgNy += nn.y;
            }
            avgNy /= Math.max(1, vertices.length);
            String matKeys = (matMap == null) ? "null" : matMap.keySet().toString();
            String pvSample = "null";
            if (perVertex != null) {
                int show = Math.min(5, perVertex.length);
                StringBuilder sb = new StringBuilder("len=" + perVertex.length + " first=");
                for (int i = 0; i < show; i++) sb.append(perVertex[i]).append(",");
                pvSample = sb.toString();
            }
            org.apache.logging.log4j.LogManager.getLogger("DynamX-ObjDump").info(
                    "obj={} v={} idx={} tri={} bbox=[{} .. {}] nan={} oobIdx={} mats={} perVertex={} uv=[{},{} .. {},{}] zeroUV={} zeroNrm={} avgNy={}",
                    diagName, vertices.length, indices.length, triCountDbg,
                    String.format("(%.3f,%.3f,%.3f)", minX, minY, minZ),
                    String.format("(%.3f,%.3f,%.3f)", maxX, maxY, maxZ),
                    nanCount, oobIdx, matKeys, pvSample,
                    String.format("%.3f", minU), String.format("%.3f", minV),
                    String.format("%.3f", maxU), String.format("%.3f", maxV),
                    zeroUV, zeroNormal, String.format("%.3f", avgNy));
        }

        // Group triangles by material so we issue one quad per RenderType bind.
        // perVertex is sized to triangle count (one entry per face from the parser),
        // so its length equals indices.length / 3.
        int triCount = indices.length / 3;
        if (matMap == null || matMap.isEmpty() || perVertex == null || perVertex.length != triCount) {
            // No materials known: render everything with a single fallback white texture.
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(MISSING_TEXTURE));
            drawTriangles(pose, vc, vertices, indices, 0, indices.length, 1f, 1f, 1f, packedLight);
            return;
        }

        // Group triangle indices by material first. Switching RenderTypes inside the emit
        // loop (e.g. matA -> matB -> matA) causes MultiBufferSource.BufferSource to draw
        // each transition as its own batch, which can shuffle vertices between batches
        // and produce shattered geometry. Sorting up-front keeps every RenderType opened
        // exactly once per object.
        java.util.LinkedHashMap<String, java.util.List<Integer>> trianglesByMat = new java.util.LinkedHashMap<>();
        for (int t = 0; t < triCount; t++) {
            String mat = perVertex[t];
            if (mat == null) mat = "_none_";
            trianglesByMat.computeIfAbsent(mat, k -> new java.util.ArrayList<>()).add(t);
        }
        for (Map.Entry<String, java.util.List<Integer>> e : trianglesByMat.entrySet()) {
            String mat = e.getKey();
            Material material = model.getMaterials().get(mat);
            ResourceLocation tex = null;
            if (material != null && !material.diffuseTexture.isEmpty()) {
                // The MTL may declare multiple map_Kd lines per material (one base + named
                // light variants like "on" / "position" / "reverse"). HashMap iteration order
                // is unspecified, so picking values().iterator().next() can return a light
                // overlay texture in place of the base diffuse, breaking the car body texture.
                MaterialTexture mt = material.diffuseTexture.get("default");
                if (mt == null) mt = material.diffuseTexture.values().iterator().next();
                mt.uploadTexture();
                tex = mt.getEffectivePath();
            }
            if (tex == null) tex = MISSING_TEXTURE;
            // TEMP DIAGNOSTIC: log effective texture path per material once
            String texKey = (objObjectData.getName() == null ? "?" : objObjectData.getName()) + "|" + mat;
            if (DUMPED_TEXTURES.add(texKey)) {
                String allVariants = "null";
                if (material != null) allVariants = material.diffuseTexture.keySet().toString();
                org.apache.logging.log4j.LogManager.getLogger("DynamX-ObjDump").info(
                        "tex obj={} mat={} pickedKey=default? texPath={} variants={} diffuseRGB=({},{},{})",
                        objObjectData.getName(), mat, tex,
                        allVariants,
                        material != null ? material.diffuseColor.x : "?",
                        material != null ? material.diffuseColor.y : "?",
                        material != null ? material.diffuseColor.z : "?");
            }
            // entityCutoutNoCull draws both sides of every triangle, so back-faces also
            // emit fragments with an inverted normal interpretation. On opaque car body
            // panels that overlays shadow-tinted triangles on top of the lit front face,
            // producing the "shattered" look. Vehicles are closed shells - use the
            // backface-culled variant.
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(tex));
            float r = 1f, g = 1f, b = 1f;
            if (material != null) {
                r = material.diffuseColor.x;
                g = material.diffuseColor.y;
                b = material.diffuseColor.z;
            }
            for (int t : e.getValue()) {
                drawTriangle(pose, vc, vertices, indices, t * 3, r, g, b, packedLight);
            }
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
