package fr.dynamx.client.renders.mesh.shapes;

import com.jme3.math.Vector3f;
import fr.dynamx.client.renders.mesh.DxIndexBuffer;
import fr.dynamx.client.renders.mesh.GLMesh;
import fr.dynamx.client.renders.mesh.VertexBuffer;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A GL_TRIANGLES mesh that approximates a sphere via icosahedron subdivision.
 *
 * @author jayfella
 */
public class IcosphereGLMesh extends GLMesh {
    private static final int GL_TRIANGLES = 0x0004;

    private static final float phi = MyMath.phi;

    private static final int[] icoIndices = {
            0, 11, 5, 0, 5, 1, 0, 1, 7, 0, 7, 10, 0, 10, 11,
            1, 5, 9, 5, 11, 4, 11, 10, 2, 10, 7, 6, 7, 1, 8,
            3, 9, 4, 3, 4, 2, 3, 2, 6, 3, 6, 8, 3, 8, 9,
            4, 9, 5, 2, 4, 11, 6, 2, 10, 8, 6, 7, 9, 8, 1
    };

    private static final Vector3f[] icoLocations = {
            new Vector3f(-1f, +phi, 0f),
            new Vector3f(+1f, +phi, 0f),
            new Vector3f(-1f, -phi, 0f),
            new Vector3f(+1f, -phi, 0f),
            new Vector3f(0f, -1f, +phi),
            new Vector3f(0f, +1f, +phi),
            new Vector3f(0f, -1f, -phi),
            new Vector3f(0f, +1f, -phi),
            new Vector3f(+phi, 0f, -1f),
            new Vector3f(+phi, 0f, +1f),
            new Vector3f(-phi, 0f, -1f),
            new Vector3f(-phi, 0f, +1f)
    };

    private int nextVertexIndex = 0;
    private final List<Vector3f> locations;
    private final Map<Long, Integer> midpointCache;
    private static final IcosphereGLMesh[] sharedMeshes = new IcosphereGLMesh[14];

    public IcosphereGLMesh(int numRefineSteps, boolean withIndices) {
        super(GL_TRIANGLES, countVertices(numRefineSteps, withIndices));
        Validate.inRange(numRefineSteps, "number of refinement steps", 0, 13);

        int numVertices = super.countVertices();
        locations = new ArrayList<>(numVertices);
        midpointCache = new HashMap<>(numVertices);

        for (Vector3f icoLocation : icoLocations) {
            addVertex(icoLocation);
        }

        List<Integer> faces = new ArrayList<>(60);
        for (int icoIndex : icoIndices) {
            faces.add(icoIndex);
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

        midpointCache.clear();

        VertexBuffer posBuffer = super.createPositions();
        if (withIndices) {
            assert locations.size() == numVertices : locations.size() + " != " + numVertices;

            DxIndexBuffer indexBuffer = super.createIndices(faces.size());
            for (int vertexIndex : faces) {
                indexBuffer.put(vertexIndex);
            }
            indexBuffer.flip();
            assert indexBuffer.limit() == indexBuffer.capacity();

            for (Vector3f pos : locations) {
                posBuffer.put(pos);
            }
        } else {
            assert faces.size() == numVertices;

            for (int vertexIndex : faces) {
                Vector3f pos = locations.get(vertexIndex);
                posBuffer.put(pos);
            }
        }

        posBuffer.flip();
        assert posBuffer.limit() == posBuffer.capacity();

        locations.clear();
    }

    public static IcosphereGLMesh getMesh(int numSteps) {
        Validate.inRange(numSteps, "number of refinement steps", 0, 13);

        if (sharedMeshes[numSteps] == null) {
            sharedMeshes[numSteps] = new IcosphereGLMesh(numSteps, true);
            sharedMeshes[numSteps].makeImmutable();
        }
        return sharedMeshes[numSteps];
    }

    private int addVertex(Vector3f location) {
        float length = location.length();
        locations.add(location.mult(1f / length));

        int result = nextVertexIndex;
        ++nextVertexIndex;
        return result;
    }

    private static int countVertices(int numRefineSteps, boolean withIndices) {
        if (withIndices) {
            return 2 + (10 << (2 * numRefineSteps));
        } else {
            return 60 << (2 * numRefineSteps);
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
        int newIndex = addVertex(middleLocation);
        midpointCache.put(key, newIndex);
        return newIndex;
    }
}
