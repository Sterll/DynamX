package fr.dynamx.common.objloader;

import fr.dynamx.common.objloader.data.Material;
import fr.dynamx.common.objloader.data.ObjObjectData;
import fr.dynamx.utils.DynamXUtils;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OBJLoader {

    private static final String COMMENT = "#";
    private static final String FACE = "f";
    private static final String POSITION = "v";
    private static final String TEX_COORDS = "vt";
    private static final String NORMAL = "vn";
    private static final String NEW_OBJECT = "o";
    private static final String NEW_GROUP = "g";
    private static final String USE_MATERIAL = "usemtl";
    private static final String NEW_MATERIAL = "mtllib";
    private static final String SMOOTH = "s";

    @Getter
    private static final List<MTLLoader> mtlLoaders = new ArrayList<>();

    private boolean hasNormals = false;
    private boolean hasTexCoords = false;
    private final List<ObjObjectData> objObjects;
    private final Map<String, Material> materials;

    public OBJLoader(List<ObjObjectData> objects, Map<String, Material> materials) {
        this.objObjects = objects;
        this.materials = materials;
    }

    public static String[] trim(String[] split) {
        List<String> strings = new ArrayList<>();
        for (String s : split)
            if (s != null && !s.trim().isEmpty())
                strings.add(s.trim());
        return strings.toArray(new String[0]);
    }

    /**
     * Reads an OBJ model and parses associated MTL files.
     *
     * @param location   Path of the obj model's parent directory (namespace + folder). Null on
     *                   server-side (ignores mtl/texture references).
     * @param mtlResolver Function that opens an InputStream for the given mtllib name relative to
     *                    {@code location}. May be null on server-side.
     * @param objContent Content of the .obj file.
     */
    public void readAndLoadModel(@Nullable ResourceLocation location,
                                 @Nullable MtlResolver mtlResolver,
                                 String objContent) {
        try {
            // hasNormals must start false so the computeNormals() fallback runs for OBJs that
            // ship no `vn` lines. parseOBJIndex flips it true the moment a face token references
            // a normal index, which is the correct signal.
            hasNormals = false;
            IndexedModel result = new IndexedModel();

            List<Vector3f> positions = new ArrayList<>();
            List<Vector2f> texCoords = new ArrayList<>();
            List<Vector3f> normals = new ArrayList<>();
            List<IndexedModel.OBJIndex> indices = new ArrayList<>();
            List<String> indicedMaterials = new ArrayList<>();
            List<Integer> smoothGroups = new ArrayList<>();
            Map<ObjObjectData, List<Integer>> objectSmoothGroups = new HashMap<>();
            int currentSmoothGroup = 0;

            String currentMaterial = null;
            Map<ObjObjectData, IndexedModel> objects = new HashMap<>();
            ObjObjectData currentObject = null;

            String[] lines = objContent.split("[\n\r]");
            for (int j = 0; j < lines.length; j++) {
                String line = lines[j];
                if (line == null || line.trim().isEmpty()) continue;
                try {
                    String[] parts = trim(line.split(" "));
                    if (parts.length == 0) continue;
                    if (!Objects.equals(parts[0], COMMENT) && parts.length == 1) continue;
                    switch (parts[0]) {
                        case COMMENT:
                            break;
                        case POSITION:
                            positions.add(new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
                            break;
                        case FACE:
                            for (int i = 0; i < parts.length - 3; i++) {
                                indicedMaterials.add(currentMaterial);
                                smoothGroups.add(currentSmoothGroup);
                                indices.add(parseOBJIndex(parts[1]));
                                indices.add(parseOBJIndex(parts[2 + i]));
                                indices.add(parseOBJIndex(parts[3 + i]));
                            }
                            break;
                        case SMOOTH:
                            if (parts.length >= 2) {
                                String sg = parts[1];
                                if (sg.equalsIgnoreCase("off")) {
                                    currentSmoothGroup = 0;
                                } else {
                                    try {
                                        currentSmoothGroup = Integer.parseInt(sg);
                                    } catch (NumberFormatException nfe) {
                                        currentSmoothGroup = 0;
                                    }
                                }
                            }
                            break;
                        case NORMAL:
                            normals.add(new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
                            break;
                        case TEX_COORDS:
                            if (location != null && parts.length >= 3) {
                                texCoords.add(new Vector2f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2])));
                                hasTexCoords = true;
                            }
                            break;
                        case NEW_MATERIAL:
                            if (location != null && mtlResolver != null && parts.length >= 2) {
                                try (InputStream is = mtlResolver.open(parts[1])) {
                                    if (is != null) {
                                        MTLLoader m = new MTLLoader();
                                        m.parse(location, new String(DynamXUtils.readInputStream(is), StandardCharsets.UTF_8));
                                        m.getMaterials().forEach(mt -> materials.put(mt.getName().toLowerCase(), mt));
                                        synchronized (mtlLoaders) {
                                            mtlLoaders.add(m);
                                        }
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                            break;
                        case USE_MATERIAL:
                            setMaterialFinalIndex(indices, currentMaterial, result);
                            currentMaterial = parts[1].toLowerCase();
                            result.materials.put(currentMaterial, new Material.IndexPair(indices.size(), -1));
                            break;
                        case NEW_OBJECT:
                        case NEW_GROUP:
                            // close previous object
                            if (currentObject != null) {
                                result.getObjIndices().addAll(indices);
                                result.getIndicedMaterials().addAll(indicedMaterials);
                                setMaterialFinalIndex(indices, currentMaterial, result);
                                objects.put(currentObject, result);
                                objectSmoothGroups.put(currentObject, smoothGroups);
                            }
                            indices = new ArrayList<>();
                            indicedMaterials = new ArrayList<>();
                            smoothGroups = new ArrayList<>();
                            result = new IndexedModel();
                            currentObject = new ObjObjectData(parts[1]);
                            break;
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error at line " + j + ": " + line, e);
                }
            }
            // flush last
            if (currentObject == null) {
                currentObject = new ObjObjectData("main");
            }
            result.getObjIndices().addAll(indices);
            result.getIndicedMaterials().addAll(indicedMaterials);
            setMaterialFinalIndex(indices, currentMaterial, result);
            objects.put(currentObject, result);
            objectSmoothGroups.put(currentObject, smoothGroups);

            objObjects.clear();
            for (Map.Entry<ObjObjectData, IndexedModel> entry : objects.entrySet()) {
                ObjObjectData object = entry.getKey();
                IndexedModel im = entry.getValue();
                List<IndexedModel.OBJIndex> objIdx = im.getObjIndices();
                Map<IndexedModel.OBJIndex, Integer> resultIndexMap = new HashMap<>();

                for (IndexedModel.OBJIndex current : objIdx) {
                    // Never skip an OBJIndex: doing so desynchronises im.indices (per-vertex)
                    // from indicedMaterials (per-triangle), shifting every later triangle's
                    // material and producing the "shattered geometry" look. Out-of-range
                    // positions fall back to the origin so the triangle count stays consistent.
                    Vector3f srcPos = (current.positionIndex >= 0 && current.positionIndex < positions.size())
                            ? positions.get(current.positionIndex) : null;
                    Vector3f pos = srcPos != null ? new Vector3f(srcPos) : new Vector3f();
                    Vector2f srcTc = (hasTexCoords && location != null && current.texCoordsIndex >= 0 && current.texCoordsIndex < texCoords.size())
                            ? texCoords.get(current.texCoordsIndex) : null;
                    Vector2f tc = srcTc != null ? new Vector2f(srcTc) : new Vector2f();
                    Vector3f srcN = (hasNormals && current.normalIndex >= 0 && current.normalIndex < normals.size())
                            ? normals.get(current.normalIndex) : null;
                    Vector3f n = srcN != null ? new Vector3f(srcN) : new Vector3f(0, 1, 0);

                    Integer modelVertexIndex = resultIndexMap.get(current);
                    if (modelVertexIndex == null) {
                        modelVertexIndex = im.getVertices().size();
                        resultIndexMap.put(current, modelVertexIndex);
                        im.getVertices().add(pos);
                        im.getTexCoords().add(tc);
                        im.getNormals().add(n);
                    }
                    im.getIndices().add(modelVertexIndex);
                }

                if (!hasNormals) im.computeNormals();
                List<Integer> sgs = objectSmoothGroups.get(object);
                if (sgs != null) {
                    applySmoothingGroups(im, sgs);
                }
                object.setCenter(im.computeCenter());
                im.toMesh(object);
                objObjects.add(object);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while loading obj model", e);
        }
    }

    /**
     * Re-averages per-vertex normals for each non-zero smoothing group so faces tagged with the
     * same `s N` directive share a continuous normal field at shared vertex positions. Triangles
     * tagged `s off` / `s 0` keep their per-face (or per-vertex from `vn`) normals untouched.
     */
    private static void applySmoothingGroups(IndexedModel im, List<Integer> faceSmoothGroups) {
        List<IndexedModel.OBJIndex> objIdx = im.getObjIndices();
        List<Integer> outIndices = im.getIndices();
        List<Vector3f> normals = im.getNormals();
        int triCount = outIndices.size() / 3;
        if (faceSmoothGroups.size() < triCount) return;

        Map<SmoothKey, Vector3f> sum = new HashMap<>();
        Map<SmoothKey, List<Integer>> members = new HashMap<>();
        boolean any = false;
        for (int t = 0; t < triCount; t++) {
            int sg = faceSmoothGroups.get(t);
            if (sg == 0) continue;
            any = true;
            for (int k = 0; k < 3; k++) {
                int idx = t * 3 + k;
                int posIdx = objIdx.get(idx).positionIndex;
                int outVi = outIndices.get(idx);
                SmoothKey key = new SmoothKey(sg, posIdx);
                Vector3f n = normals.get(outVi);
                sum.computeIfAbsent(key, kk -> new Vector3f()).add(n);
                members.computeIfAbsent(key, kk -> new ArrayList<>()).add(outVi);
            }
        }
        if (!any) return;
        for (Map.Entry<SmoothKey, Vector3f> e : sum.entrySet()) {
            Vector3f avg = e.getValue();
            if (avg.lengthSquared() > 1e-12f) avg.normalize();
            else avg.set(0, 1, 0);
            for (int vi : members.get(e.getKey())) {
                normals.get(vi).set(avg);
            }
        }
    }

    private static final class SmoothKey {
        final int smoothGroup;
        final int positionIndex;
        SmoothKey(int sg, int pi) { this.smoothGroup = sg; this.positionIndex = pi; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof SmoothKey)) return false;
            SmoothKey k = (SmoothKey) o;
            return k.smoothGroup == smoothGroup && k.positionIndex == positionIndex;
        }
        @Override public int hashCode() { return smoothGroup * 31 + positionIndex; }
    }

    private void setMaterialFinalIndex(List<IndexedModel.OBJIndex> indices, String currentMaterial, IndexedModel currentObject) {
        if (currentMaterial == null) return;
        if (currentObject.materials.containsKey(currentMaterial)) {
            currentObject.materials.get(currentMaterial).setFinalIndex(indices.size());
        }
    }

    private IndexedModel.OBJIndex parseOBJIndex(String token) {
        IndexedModel.OBJIndex index = new IndexedModel.OBJIndex();
        // OBJIndex's int fields default to 0 - if a face token omits texcoord or normal
        // (e.g. "f 1//1" or "f 1/2"), leaving them at 0 makes the dedup loop pull texCoords[0]
        // / normals[0] into every such vertex, producing scrambled UVs and shattered shading.
        // Use -1 as the explicit "missing" sentinel so the bounds check falls back to defaults.
        index.texCoordsIndex = -1;
        index.normalIndex = -1;
        String[] values = token.split("/");
        index.positionIndex = Integer.parseInt(values[0]) - 1;
        if (values.length > 1) {
            if (!values[1].isEmpty()) {
                index.texCoordsIndex = Integer.parseInt(values[1]) - 1;
            }
            if (values.length > 2 && !values[2].isEmpty()) {
                index.normalIndex = Integer.parseInt(values[2]) - 1;
                hasNormals = true;
            }
        }
        return index;
    }

    /** Resolves a mtllib reference to an InputStream. */
    @FunctionalInterface
    public interface MtlResolver {
        @Nullable InputStream open(String mtlName) throws Exception;
    }
}
