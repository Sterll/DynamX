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
            hasNormals = true;
            IndexedModel result = new IndexedModel();

            List<Vector3f> positions = new ArrayList<>();
            List<Vector2f> texCoords = new ArrayList<>();
            List<Vector3f> normals = new ArrayList<>();
            List<IndexedModel.OBJIndex> indices = new ArrayList<>();
            List<String> indicedMaterials = new ArrayList<>();

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
                                indices.add(parseOBJIndex(parts[1]));
                                indices.add(parseOBJIndex(parts[2 + i]));
                                indices.add(parseOBJIndex(parts[3 + i]));
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
                            }
                            indices = new ArrayList<>();
                            indicedMaterials = new ArrayList<>();
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

            objObjects.clear();
            for (Map.Entry<ObjObjectData, IndexedModel> entry : objects.entrySet()) {
                ObjObjectData object = entry.getKey();
                IndexedModel im = entry.getValue();
                List<IndexedModel.OBJIndex> objIdx = im.getObjIndices();
                Map<IndexedModel.OBJIndex, Integer> resultIndexMap = new HashMap<>();

                for (IndexedModel.OBJIndex current : objIdx) {
                    if (current.positionIndex < 0 || current.positionIndex >= positions.size()) continue;
                    Vector3f pos = positions.get(current.positionIndex);
                    Vector2f tc = hasTexCoords && location != null && current.texCoordsIndex >= 0 && current.texCoordsIndex < texCoords.size()
                            ? texCoords.get(current.texCoordsIndex)
                            : new Vector2f();
                    Vector3f n = hasNormals && current.normalIndex >= 0 && current.normalIndex < normals.size()
                            ? normals.get(current.normalIndex)
                            : new Vector3f(0, 1, 0);

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
                object.setCenter(im.computeCenter());
                im.toMesh(object);
                objObjects.add(object);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while loading obj model", e);
        }
    }

    private void setMaterialFinalIndex(List<IndexedModel.OBJIndex> indices, String currentMaterial, IndexedModel currentObject) {
        if (currentMaterial == null) return;
        if (currentObject.materials.containsKey(currentMaterial)) {
            currentObject.materials.get(currentMaterial).setFinalIndex(indices.size());
        }
    }

    private IndexedModel.OBJIndex parseOBJIndex(String token) {
        IndexedModel.OBJIndex index = new IndexedModel.OBJIndex();
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
