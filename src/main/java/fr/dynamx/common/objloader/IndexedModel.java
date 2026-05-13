package fr.dynamx.common.objloader;

import fr.dynamx.common.objloader.data.Material;
import fr.dynamx.common.objloader.data.ObjObjectData;
import fr.dynamx.common.objloader.data.Vertex;
import lombok.Getter;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexedModel {

    @Getter
    private final List<Vector3f> vertices = new ArrayList<>();
    @Getter
    private final List<Vector2f> texCoords = new ArrayList<>();
    @Getter
    private final List<Vector3f> normals = new ArrayList<>();
    @Getter
    private final List<Integer> indices = new ArrayList<>();
    @Getter
    private final List<OBJIndex> objIndices = new ArrayList<>();
    @Getter
    private final List<String> indicedMaterials = new ArrayList<>();
    @Getter
    public final Map<String, Material.IndexPair> materials = new HashMap<>();

    public void toMesh(ObjObjectData mesh) {
        int n = Math.min(vertices.size(), Math.min(texCoords.size(), normals.size()));
        Vertex[] arr = new Vertex[n];
        for (int i = 0; i < n; i++) {
            arr[i] = new Vertex(vertices.get(i), texCoords.get(i), normals.get(i));
        }
        int[] indicesArrayInt = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) indicesArrayInt[i] = indices.get(i);
        mesh.setVertices(arr);
        mesh.setIndices(indicesArrayInt);
        mesh.setMaterialForEachVertex(indicedMaterials.toArray(new String[0]));
        mesh.setMaterials(materials);
    }

    public void computeNormals() {
        for (int i = 0; i < indices.size(); i += 3) {
            int i0 = indices.get(i);
            int i1 = indices.get(i + 1);
            int i2 = indices.get(i + 2);

            Vector3f l0 = new Vector3f(vertices.get(i1)).sub(vertices.get(i0));
            Vector3f l1 = new Vector3f(vertices.get(i2)).sub(vertices.get(i0));
            Vector3f n = new Vector3f(l0).cross(l1);

            normals.get(i0).add(n);
            normals.get(i1).add(n);
            normals.get(i2).add(n);
        }
        for (Vector3f n : normals) {
            if (n.lengthSquared() > 1e-8f) n.normalize();
        }
    }

    public Vector3f computeCenter() {
        float x = 0, y = 0, z = 0;
        for (Vector3f p : vertices) {
            x += p.x;
            y += p.y;
            z += p.z;
        }
        int s = Math.max(1, vertices.size());
        return new Vector3f(x / s, y / s, z / s);
    }

    public static final class OBJIndex {
        public int positionIndex;
        public int texCoordsIndex;
        public int normalIndex;

        @Override
        public boolean equals(Object o) {
            if (o instanceof OBJIndex i) {
                return i.normalIndex == normalIndex && i.positionIndex == positionIndex && i.texCoordsIndex == texCoordsIndex;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int r = 17;
            r = 31 * r + positionIndex;
            r = 31 * r + texCoordsIndex;
            r = 31 * r + normalIndex;
            return r;
        }
    }
}
