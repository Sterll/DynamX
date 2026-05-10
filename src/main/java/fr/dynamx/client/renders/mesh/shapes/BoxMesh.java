package fr.dynamx.client.renders.mesh.shapes;

import com.jme3.math.Vector3f;
import fr.dynamx.client.renders.mesh.GLMesh;
import fr.dynamx.client.renders.mesh.VertexBuffer;

/**
 * A GL_TRIANGLES mesh that renders an axis-aligned box.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class BoxMesh extends GLMesh {
    private static final int GL_TRIANGLES = 0x0004;

    private static final int[] cubeIndices = {
            0, 1, 2, 1, 3, 2,
            4, 7, 5, 4, 6, 7,
            0, 4, 1, 1, 4, 5,
            2, 3, 7, 2, 7, 6,
            0, 6, 4, 0, 2, 6,
            1, 5, 3, 3, 5, 7
    };

    private static final Vector3f[] cubeLocations = {
            new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 1f),
            new Vector3f(0f, 1f, 0f), new Vector3f(0f, 1f, 1f),
            new Vector3f(1f, 0f, 0f), new Vector3f(1f, 0f, 1f),
            new Vector3f(1f, 1f, 0f), new Vector3f(1f, 1f, 1f)
    };

    private static BoxMesh bm111;

    public BoxMesh(float x1, float y1, float z1, float x2, float y2, float z2) {
        super(GL_TRIANGLES, 36);

        VertexBuffer posBuffer = super.createPositions();
        for (int vertexIndex : cubeIndices) {
            Vector3f loc = cubeLocations[vertexIndex];
            float x = x1 + loc.x * (x2 - x1);
            float y = y1 + loc.y * (y2 - y1);
            float z = z1 + loc.z * (z2 - z1);
            posBuffer.put(x).put(y).put(z);
        }
        posBuffer.flip();
        assert posBuffer.limit() == posBuffer.capacity();
    }

    public BoxMesh(float halfExtent) {
        this(new Vector3f(halfExtent, halfExtent, halfExtent));
    }

    public BoxMesh(Vector3f halfExtent) {
        this(halfExtent.x, halfExtent.y, halfExtent.z, -halfExtent.x, -halfExtent.y, -halfExtent.z);
    }

    public static BoxMesh getMesh() {
        if (bm111 == null) {
            bm111 = new BoxMesh(-1f, -1f, -1f, 1f, 1f, 1f);
            bm111.generateFacetNormals();
            bm111.makeImmutable();
        }
        return bm111;
    }
}
