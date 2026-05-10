package fr.dynamx.client.renders.mesh.shapes;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import fr.dynamx.client.renders.mesh.DxIndexBuffer;
import fr.dynamx.client.renders.mesh.GLMesh;
import fr.dynamx.client.renders.mesh.VertexBuffer;
import fr.dynamx.utils.maths.DynamXGeometry;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A GL_TRIANGLES mesh (with texture coordinates) approximating a sphere via octahedron subdivision.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class OctasphereMesh extends GLMesh {
    private static final int GL_TRIANGLES = 0x0004;

    private static final int[] octaIndices = {
            0, 2, 5,
            1, 9, 3,
            6, 3, 7,
            1, 10, 2,
            0, 4, 2,
            1, 3, 10,
            6, 8, 3,
            1, 2, 9
    };

    private static final Vector3f[] octaLocations = {
            new Vector3f(-1f, 0f, 0f),
            new Vector3f(+1f, 0f, 0f),
            new Vector3f(0f, -1f, 0f),
            new Vector3f(0f, +1f, 0f),
            new Vector3f(0f, 0f, -1f),
            new Vector3f(0f, 0f, +1f)
    };

    private int nextVertexIndex = 0;
    private final List<Float> uOverrides;
    private final List<Vector3f> locations;
    private final Map<Long, Integer> midpointCache;
    private static final OctasphereMesh[] sharedMeshes = new OctasphereMesh[14];

    private Vector3f tmpVector = new Vector3f();
    private VertexBuffer posBuffer;
    private VertexBuffer uvBuffer;

    public OctasphereMesh(int numRefineSteps) {
        this(numRefineSteps, true);
    }

    public OctasphereMesh(int numRefineSteps, boolean withIndices) {
        super(GL_TRIANGLES, countVertices(numRefineSteps, withIndices));
        Validate.inRange(numRefineSteps, "number of refinement steps", 0, 13);

        int numVertices = super.countVertices();
        uOverrides = new ArrayList<>(numVertices);
        locations = new ArrayList<>(numVertices);
        midpointCache = new HashMap<>(numVertices);

        addVertex(octaLocations[0], -1f);
        addVertex(octaLocations[1], 0f);
        addVertex(octaLocations[2], null);
        addVertex(octaLocations[3], null);
        addVertex(octaLocations[4], -1f);
        addVertex(octaLocations[5], -1f);
        addVertex(octaLocations[0], +1f);
        addVertex(octaLocations[4], +1f);
        addVertex(octaLocations[5], +1f);
        addVertex(octaLocations[4], 0f);
        addVertex(octaLocations[5], 0f);

        List<Integer> faces = new ArrayList<>(24);
        for (int octaIndex : octaIndices) {
            faces.add(octaIndex);
        }

        for (int stepIndex = 0; stepIndex < numRefineSteps; ++stepIndex) {
            List<Integer> newFaces = new ArrayList<>(4 * faces.size());
            for (int j = 0; j < faces.size(); j += vpt) {
                int v1 = faces.get(j);
                int v2 = faces.get(j + 1);
                int v3 = faces.get(j + 2);

                int a = midpointIndex(v1, v2);
                int b = midpointIndex(v2, v3);
                int c = midpointIndex(v3, v1);

                newFaces.add(v1);
                newFaces.add(a);
                newFaces.add(c);

                newFaces.add(v2);
                newFaces.add(b);
                newFaces.add(a);

                newFaces.add(v3);
                newFaces.add(c);
                newFaces.add(b);

                newFaces.add(a);
                newFaces.add(b);
                newFaces.add(c);
            }
            faces = newFaces;
        }

        assert locations.size() == uOverrides.size();
        midpointCache.clear();

        posBuffer = super.createPositions();
        uvBuffer = super.createUvs();

        if (withIndices) {
            assert locations.size() == numVertices : locations.size() + " != " + numVertices;

            DxIndexBuffer indexBuffer = super.createIndices(faces.size());
            for (int vertexIndex : faces) {
                indexBuffer.put(vertexIndex);
            }
            indexBuffer.flip();
            assert indexBuffer.limit() == indexBuffer.capacity();

            for (int vIndex = 0; vIndex < locations.size(); ++vIndex) {
                putVertex(vIndex);
            }
        } else {
            assert faces.size() == numVertices;

            for (int vertexIndex : faces) {
                putVertex(vertexIndex);
            }
        }

        posBuffer.flip();
        assert posBuffer.limit() == posBuffer.capacity();
        uvBuffer.flip();
        assert uvBuffer.limit() == uvBuffer.capacity();

        locations.clear();
        uOverrides.clear();
        tmpVector = null;
    }

    public static OctasphereMesh getMesh(int numSteps) {
        Validate.inRange(numSteps, "number of refinement steps", 0, 13);

        if (sharedMeshes[numSteps] == null) {
            sharedMeshes[numSteps] = new OctasphereMesh(numSteps);
            sharedMeshes[numSteps].makeImmutable();
        }
        return sharedMeshes[numSteps];
    }

    private int addVertex(Vector3f location, Float uOverride) {
        float length = location.length();
        locations.add(location.mult(1f / length));
        uOverrides.add(uOverride);
        assert locations.size() == uOverrides.size();

        int result = nextVertexIndex;
        ++nextVertexIndex;
        return result;
    }

    private static int countVertices(int numRefineSteps, boolean withIndices) {
        if (withIndices) {
            switch (numRefineSteps) {
                case 0:
                    return 11;
                case 1:
                    return 29;
                case 2:
                    return 89;
                case 3:
                    return 305;
                case 4:
                    return 1_121;
                case 5:
                    return 4_289;
                case 6:
                    return 16_769;
                case 7:
                    return 66_305;
                case 8:
                    return 263_681;
                case 9:
                    return 1_051_649;
                case 10:
                    return 4_200_449;
                default:
                    throw new IllegalArgumentException("num refine steps = " + numRefineSteps);
            }
        } else {
            return 24 << (2 * numRefineSteps);
        }
    }

    private int midpointIndex(int p1, int p2) {
        boolean firstIsSmaller = p1 < p2;
        long smallerIndex = firstIsSmaller ? p1 : p2;
        long greaterIndex = firstIsSmaller ? p2 : p1;
        long key = (smallerIndex << 32) + greaterIndex;
        Integer cachedIndex = midpointCache.get(key);
        if (cachedIndex != null) {
            return cachedIndex;
        }
        Vector3f loc1 = locations.get(p1);
        Vector3f loc2 = locations.get(p2);
        Vector3f middleLocation = MyVector3f.midpoint(loc1, loc2, null);

        Float middleUOverride = null;
        if (middleLocation.y == 0f) {
            middleUOverride = uOverrides.get(p1);
            assert uOverrides.get(p2).equals(middleUOverride);
        } else {
            assert uOverrides.get(p1) == null || uOverrides.get(p2) == null;
        }
        int newIndex = addVertex(middleLocation, middleUOverride);
        midpointCache.put(key, newIndex);
        return newIndex;
    }

    private void putVertex(int vIndex) {
        Vector3f pos = locations.get(vIndex);
        posBuffer.put(pos);

        tmpVector.set(pos);
        DynamXGeometry.toSpherical(tmpVector);

        float u;
        if (pos.y == 0f) {
            u = uOverrides.get(vIndex);
        } else {
            assert uOverrides.get(vIndex) == null;
            u = tmpVector.y / FastMath.PI;
        }
        float v = tmpVector.z / FastMath.PI;
        uvBuffer.put(u).put(v);
    }
}
