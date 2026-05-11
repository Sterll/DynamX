package fr.dynamx.client.renders.mesh;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import fr.dynamx.utils.maths.DynamXGeometry;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import lombok.Getter;

import org.joml.Vector4f;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulate a vertex array object (VAO), to which vertex buffer objects (VBOs) are attached.
 * <p>
 * TODO port:1.20.1 - VAO/VBO bindings replaced by no-op stubs; the rendering pipeline
 * must be reworked on top of BufferBuilder/VertexFormat/RenderType to comply with the
 * 1.20.1 core profile. Public method signatures are preserved for callers.
 */
public class GLMesh implements jme3utilities.lbj.Mesh {
    // GL primitive constants kept as literal hex values to avoid depending on GL11 import.
    private static final int GL_POINTS = 0x0000;
    private static final int GL_LINES = 0x0001;
    private static final int GL_LINE_LOOP = 0x0002;
    private static final int GL_LINE_STRIP = 0x0003;
    private static final int GL_TRIANGLES = 0x0004;
    private static final int GL_TRIANGLE_STRIP = 0x0005;
    private static final int GL_TRIANGLE_FAN = 0x0006;
    private static final int GL_QUADS = 0x0007;

    protected static final int numAxes = 3;
    protected static final int vpe = 2;
    protected static final int vpt = 3;

    private boolean mutable = true;
    private DxIndexBuffer indices;
    private final int drawMode;
    private final int vertexCount;
    private Integer vaoId;
    private VertexBuffer normals;
    private VertexBuffer positions;
    @Getter
    private VertexBuffer textureCoordinates;

    public GLMesh(int drawMode, float... positionsArray) {
        this(drawMode, positionsArray.length / numAxes);
        Validate.require(positionsArray.length % numAxes == 0, "length a multiple of 3");

        FloatBuffer data = BufferUtils.createFloatBuffer(positionsArray);
        this.positions = new VertexBuffer(data, numAxes, 0);
    }

    protected GLMesh(int drawMode, FloatBuffer positionsBuffer) {
        this(drawMode, positionsBuffer.capacity() / numAxes);
        int capacity = positionsBuffer.capacity();
        Validate.require(capacity % numAxes == 0, "capacity a multiple of 3");

        positionsBuffer.rewind();
        positionsBuffer.limit(capacity);

        this.positions = new VertexBuffer(positionsBuffer, numAxes, 0);
    }

    public GLMesh(int drawMode, Vector3f... positionsArray) {
        this(drawMode, positionsArray.length);

        FloatBuffer data = BufferUtils.createFloatBuffer(positionsArray);
        this.positions = new VertexBuffer(data, numAxes, 0);
    }

    protected GLMesh(int drawMode, int vertexCount) {
        Validate.nonNegative(vertexCount, "vertex count");

        this.drawMode = drawMode;
        this.vertexCount = vertexCount;
    }

    public void cleanUp() {
        // TODO port:1.20.1 - replace VAO/VBO cleanup with BufferBuilder pipeline (no explicit deletes)
        if (vaoId == null) {
            return;
        }
        if (indices != null) {
            indices.cleanUp();
        }
        if (positions != null) {
            positions.cleanUp();
        }
        if (normals != null) {
            normals.cleanUp();
        }
        if (textureCoordinates != null) {
            textureCoordinates.cleanUp();
        }
        this.vaoId = null;
    }

    public int countIndexedVertices() {
        return (indices == null) ? vertexCount : indices.capacity();
    }

    public int countLines() {
        int numIndices = countIndexedVertices();
        switch (drawMode) {
            case GL_LINES:
                return numIndices / 2;
            case GL_LINE_LOOP:
                return numIndices;
            case GL_LINE_STRIP:
                return numIndices - 1;
            case GL_POINTS:
            case GL_TRIANGLES:
            case GL_TRIANGLE_STRIP:
            case GL_TRIANGLE_FAN:
            case GL_QUADS:
                return 0;
            default:
                throw new IllegalStateException("drawMode = " + drawMode);
        }
    }

    public int countPoints() {
        int numIndices = countIndexedVertices();
        switch (drawMode) {
            case GL_POINTS:
                return numIndices;
            case GL_LINES:
            case GL_LINE_LOOP:
            case GL_LINE_STRIP:
            case GL_TRIANGLES:
            case GL_TRIANGLE_STRIP:
            case GL_TRIANGLE_FAN:
            case GL_QUADS:
                return 0;
            default:
                throw new IllegalStateException("drawMode = " + drawMode);
        }
    }

    public int countTriangles() {
        int numIndices = countIndexedVertices();
        switch (drawMode) {
            case GL_POINTS:
            case GL_LINES:
            case GL_LINE_LOOP:
            case GL_LINE_STRIP:
            case GL_QUADS:
                return 0;
            case GL_TRIANGLES:
                return numIndices / vpt;
            case GL_TRIANGLE_STRIP:
            case GL_TRIANGLE_FAN:
                return numIndices - 2;
            default:
                throw new IllegalStateException("drawMode = " + drawMode);
        }
    }

    public int countVertices() {
        return vertexCount;
    }

    public int drawMode() {
        return drawMode;
    }

    public GLMesh generateFacetNormals() {
        verifyMutable();
        if (drawMode != GL_TRIANGLES) {
            throw new IllegalStateException("drawMode = " + drawMode);
        }
        if (indices != null) {
            throw new IllegalStateException("must be non-indexed");
        }
        int numTriangles = countTriangles();
        assert vertexCount == vpt * numTriangles;

        Vector3f posA = new Vector3f();
        Vector3f posB = new Vector3f();
        Vector3f posC = new Vector3f();
        Vector3f ac = new Vector3f();
        Vector3f normal = new Vector3f();

        createNormals();
        for (int triIndex = 0; triIndex < numTriangles; ++triIndex) {
            int trianglePosition = triIndex * vpt * numAxes;
            positions.get(trianglePosition, posA);
            positions.get(trianglePosition + numAxes, posB);
            positions.get(trianglePosition + 2 * numAxes, posC);

            posB.subtract(posA, normal);
            posC.subtract(posA, ac);
            normal.cross(ac, normal);
            MyVector3f.normalizeLocal(normal);

            for (int j = 0; j < vpt; ++j) {
                normals.put(normal);
            }
        }
        normals.flip();
        assert normals.limit() == normals.capacity();

        return this;
    }

    public GLMesh generateNormals(NormalsOption option) {
        switch (option) {
            case Facet:
                generateFacetNormals();
                break;
            case None:
                this.normals = null;
                break;
            case Smooth:
                generateFacetNormals();
                smoothNormals();
                break;
            case Sphere:
                generateSphereNormals();
                break;
            default:
                throw new IllegalArgumentException("option = " + option);
        }

        return this;
    }

    public GLMesh generateSphereNormals() {
        verifyMutable();
        Vector3f tmpVector = new Vector3f();

        createNormals();
        for (int vertIndex = 0; vertIndex < vertexCount; ++vertIndex) {
            int vPosition = vertIndex * numAxes;
            positions.get(vPosition, tmpVector);
            MyVector3f.normalizeLocal(tmpVector);

            normals.put(tmpVector.x).put(tmpVector.y).put(tmpVector.z);
        }
        normals.flip();
        assert normals.limit() == normals.capacity();

        return this;
    }

    public GLMesh generateUvs(UvsOption option, Vector4f uCoefficients, Vector4f vCoefficients) {
        verifyMutable();
        if (option == UvsOption.None) {
            textureCoordinates = null;
            return this;
        }
        createUvs();

        Vector3f tmpVector = new Vector3f();
        for (int vertIndex = 0; vertIndex < vertexCount; ++vertIndex) {
            int inPosition = vertIndex * numAxes;
            positions.get(inPosition, tmpVector);
            switch (option) {
                case Linear:
                    break;
                case Spherical:
                    DynamXGeometry.toSpherical(tmpVector);
                    tmpVector.y /= FastMath.PI;
                    tmpVector.z /= FastMath.PI;
                    break;
                default:
                    throw new IllegalArgumentException("option = " + option);
            }

            float u = uCoefficients.dot(new Vector4f(tmpVector.x, tmpVector.y, tmpVector.z, 1f));
            float v = vCoefficients.dot(new Vector4f(tmpVector.x, tmpVector.y, tmpVector.z, 1f));
            textureCoordinates.put(u).put(v);
        }
        textureCoordinates.flip();
        assert textureCoordinates.limit() == textureCoordinates.capacity();

        return this;
    }

    public void setUvs(VertexBuffer uvs) {
        verifyMutable();
        this.textureCoordinates = new VertexBuffer(uvs.getBuffer(), uvs.fpv, 2);
    }

    public VertexBuffer getPositions() {
        return positions;
    }

    public GLMesh makeImmutable() {
        this.mutable = false;
        positions.makeImmutable();
        if (normals != null) {
            normals.makeImmutable();
        }
        if (textureCoordinates != null) {
            textureCoordinates.makeImmutable();
        }
        if (indices != null) {
            indices.makeImmutable();
        }
        return this;
    }

    public void render() {
        renderUsing();
    }

    public void renderUsing() {
        // TODO port:1.20.1 - replace VAO bind + glDrawArrays/glDrawElements with BufferBuilder/RenderType draw call
        enableAttributes();
        if (indices == null) {
            // TODO port:1.20.1 - GL11.glDrawArrays(drawMode, 0, vertexCount)
        } else {
            indices.drawElements(drawMode);
        }
        disableAttributes();
    }

    public GLMesh rotate(float xAngle, float yAngle, float zAngle) {
        if (xAngle == 0f && yAngle == 0f && zAngle == 0f) {
            return this;
        }
        verifyMutable();

        Quaternion quaternion = new Quaternion().fromAngles(xAngle, yAngle, zAngle);

        positions.rotate(quaternion);
        if (normals != null) {
            normals.rotate(quaternion);
        }

        return this;
    }

    public GLMesh scale(float scaleFactor) {
        if (scaleFactor == 1f) {
            return this;
        }
        verifyMutable();

        int numFloats = vertexCount * numAxes;
        for (int floatIndex = 0; floatIndex < numFloats; ++floatIndex) {
            float floatValue = positions.get(floatIndex);
            floatValue *= scaleFactor;
            positions.put(floatIndex, floatValue);
        }
        return this;
    }

    public GLMesh transform(Transform transform) {
        Validate.nonNull(transform, "transform");
        if (MyMath.isIdentity(transform)) {
            return this;
        }
        verifyMutable();

        positions.transform(transform);

        if (normals != null) {
            Transform normalsTransform = transform.clone();
            normalsTransform.getTranslation().zero();
            normalsTransform.setScale(1f);

            normals.transform(normalsTransform);
        }
        return this;
    }

    public GLMesh transformUvs(Vector4f uCoefficients, Vector4f vCoefficients) {
        verifyMutable();
        if (textureCoordinates == null) {
            throw new IllegalStateException("There are no UVs in the mesh.");
        }

        for (int vIndex = 0; vIndex < vertexCount; ++vIndex) {
            int startPosition = 2 * vIndex;
            float oldU = textureCoordinates.get(startPosition);
            float oldV = textureCoordinates.get(startPosition + 1);

            // TODO port:1.20.1 - JOML Vector4f exposes public x/y/z/w fields (no getters).
            float newU = uCoefficients.w
                    + uCoefficients.x * oldU
                    + uCoefficients.y * oldV;
            float newV = vCoefficients.w
                    + vCoefficients.x * oldU
                    + vCoefficients.y * oldV;

            textureCoordinates.put(startPosition, newU);
            textureCoordinates.put(startPosition + 1, newV);
        }
        return this;
    }

    protected DxIndexBuffer createIndices(int capacity) {
        verifyMutable();
        this.indices = new DxIndexBuffer(vertexCount, capacity);
        return indices;
    }

    protected VertexBuffer createNormals() {
        verifyMutable();
        if (countTriangles() == 0) {
            throw new IllegalStateException("The mesh doesn't contain any triangles.");
        }
        this.normals = new VertexBuffer(vertexCount, numAxes, 1);
        return normals;
    }

    protected VertexBuffer createPositions() {
        verifyMutable();
        this.positions = new VertexBuffer(vertexCount, numAxes, 0);
        return positions;
    }

    public VertexBuffer createUvs() {
        verifyMutable();
        this.textureCoordinates = new VertexBuffer(vertexCount, 2, 2);
        return textureCoordinates;
    }

    protected void setNormals(float... normalsArray) {
        verifyMutable();
        Validate.require(normalsArray.length == vertexCount * numAxes, "correct length");
        this.normals = new VertexBuffer(normalsArray, numAxes, 1);
    }

    protected void setPositions(float... positionArray) {
        verifyMutable();
        Validate.require(positionArray.length == vertexCount * numAxes, "correct length");
        this.positions = new VertexBuffer(positionArray, numAxes, 0);
    }

    protected void setUvs(float... uvArray) {
        verifyMutable();
        Validate.require(uvArray.length == 2 * vertexCount, "correct length");
        this.textureCoordinates = new VertexBuffer(uvArray, 2, 2);
    }

    @Override
    public DxIndexBuffer getIndexBuffer() {
        assert indices != null;
        return indices;
    }

    @Override
    public FloatBuffer getNormalsData() {
        return normals.getBuffer();
    }

    @Override
    public FloatBuffer getPositionsData() {
        return positions.getBuffer();
    }

    @Override
    public boolean isPureLines() {
        return drawMode == GL_LINES;
    }

    @Override
    public boolean isPureTriangles() {
        return drawMode == GL_TRIANGLES;
    }

    @Override
    public void setNormalsModified() {
        normals.setModified();
    }

    @Override
    public void setPositionsModified() {
        positions.setModified();
    }

    private void enableAttributes() {
        // TODO port:1.20.1 - replace VAO generation/bind + VBO attribute setup with BufferBuilder/VertexFormat
        if (vaoId == null) {
            this.vaoId = 0;
            this.mutable = false;
        }
        positions.prepareToDraw();
        if (normals != null) {
            normals.prepareToDraw();
        }
        if (textureCoordinates != null) {
            textureCoordinates.prepareToDraw();
        }
    }

    private void disableAttributes() {
        positions.stopDraw();
        if (normals != null) {
            normals.stopDraw();
        }
        if (textureCoordinates != null) {
            textureCoordinates.stopDraw();
        }
    }

    private void smoothNormals() {
        verifyMutable();
        assert indices == null;
        assert normals != null;

        Map<Vector3f, Integer> mapPosToDpid = new HashMap<>(vertexCount);
        int numDistinctPositions = 0;
        for (int vertexIndex = 0; vertexIndex < vertexCount; ++vertexIndex) {
            int start = vertexIndex * numAxes;
            Vector3f position = new Vector3f();
            positions.get(start, position);
            MyVector3f.standardize(position, position);
            if (!mapPosToDpid.containsKey(position)) {
                mapPosToDpid.put(position, numDistinctPositions);
                ++numDistinctPositions;
            }
        }
        Vector3f[] normalSums = new Vector3f[numDistinctPositions];
        for (int dpid = 0; dpid < numDistinctPositions; ++dpid) {
            normalSums[dpid] = new Vector3f();
        }

        Vector3f tmpPosition = new Vector3f();
        Vector3f tmpNormal = new Vector3f();
        for (int vertexIndex = 0; vertexIndex < vertexCount; ++vertexIndex) {
            int start = vertexIndex * numAxes;
            positions.get(start, tmpPosition);
            MyVector3f.standardize(tmpPosition, tmpPosition);
            int dpid = mapPosToDpid.get(tmpPosition);

            normals.get(start, tmpNormal);
            normalSums[dpid].addLocal(tmpNormal);
        }
        for (Vector3f normal : normalSums) {
            MyVector3f.normalizeLocal(normal);
        }
        for (int vertexIndex = 0; vertexIndex < vertexCount; ++vertexIndex) {
            int start = vertexIndex * numAxes;
            positions.get(start, tmpPosition);
            MyVector3f.standardize(tmpPosition, tmpPosition);
            int dpid = mapPosToDpid.get(tmpPosition);
            normals.put(start, normalSums[dpid]);
        }
    }

    private void verifyMutable() {
        if (!mutable) {
            throw new IllegalStateException("The mesh is no longer mutable.");
        }
    }
}
