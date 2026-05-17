package fr.dynamx.client.renders.mesh;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import fr.dynamx.utils.maths.DynamXGeometry;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;
import lombok.Getter;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Vector4f;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Mesh primitive backed by CPU-side float buffers (positions, optional normals, optional UVs)
 * and an optional {@link DxIndexBuffer}.
 * <p>
 * In 1.12.2 this class managed an OpenGL VAO plus per-attribute VBOs through {@code GL15} and
 * issued {@code glDrawArrays} / {@code glDrawElements} in immediate mode. The 1.20.1 core
 * profile requires going through {@code BufferBuilder} with a {@link DefaultVertexFormat} and
 * an explicit shader, so {@link #renderUsing()} now rebuilds a transient vertex stream every
 * frame and submits it through {@link BufferUploader}. CPU-side mutation helpers (rotate,
 * scale, transform, normal generation, UV generation, smoothing) are preserved verbatim so
 * mesh-building call sites such as {@code BoxMesh}, {@code IcosphereGLMesh} or
 * {@code OctasphereMesh} keep working unchanged.
 * <p>
 * TODO port:1.20.1 - normals are still computed and stored CPU-side but are not emitted to
 * the GPU because no built-in 1.20.1 vanilla shader matches {@code POSITION_TEX_NORMAL}.
 * Meshes that rely on per-vertex lighting (smooth-shaded debug spheres for example) will look
 * flat-shaded until a custom shader or a {@code RenderType}-based path is wired in.
 */
public class GLMesh implements jme3utilities.lbj.Mesh {
    // Legacy GL draw-mode constants kept as bare ints to preserve the public API used by
    // BoxMesh / OctasphereMesh / IcosphereGLMesh / GridGLMesh / ArrowMesh.
    public static final int GL_POINTS = 0x0000;
    public static final int GL_LINES = 0x0001;
    public static final int GL_LINE_LOOP = 0x0002;
    public static final int GL_LINE_STRIP = 0x0003;
    public static final int GL_TRIANGLES = 0x0004;
    public static final int GL_TRIANGLE_STRIP = 0x0005;
    public static final int GL_TRIANGLE_FAN = 0x0006;
    public static final int GL_QUADS = 0x0007;

    protected static final int numAxes = 3;
    protected static final int vpe = 2;
    protected static final int vpt = 3;

    private boolean mutable = true;
    private boolean uploaded = false;
    private DxIndexBuffer indices;
    private final int drawMode;
    private final int vertexCount;
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
        this.uploaded = false;
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

    /**
     * Emit the mesh through a {@link BufferBuilder}, using a {@link DefaultVertexFormat} that
     * matches the available attributes (positions only, or positions + UVs) and the matching
     * built-in shader. Indices, if present, drive the emission order; otherwise vertices are
     * walked in storage order. The first call locks the mesh as immutable to preserve the
     * legacy invariant where rendering a mesh fixed its CPU buffers.
     */
    public void renderUsing() {
        if (vertexCount == 0) {
            return;
        }
        if (!uploaded) {
            this.uploaded = true;
            this.mutable = false;
        }

        VertexFormat.Mode mode = mapDrawMode(drawMode);
        boolean hasUv = textureCoordinates != null;

        VertexFormat format;
        if (hasUv) {
            format = DefaultVertexFormat.POSITION_TEX;
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
        } else {
            format = DefaultVertexFormat.POSITION;
            RenderSystem.setShader(GameRenderer::getPositionShader);
        }

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(mode, format);

        int emitCount = (indices == null) ? vertexCount : indices.capacity();
        for (int i = 0; i < emitCount; ++i) {
            int idx = (indices == null) ? i : indices.getIndex(i);
            int posBase = idx * numAxes;
            float x = positions.get(posBase);
            float y = positions.get(posBase + 1);
            float z = positions.get(posBase + 2);
            builder.vertex(x, y, z);
            if (hasUv) {
                int uvBase = idx * textureCoordinates.fpv;
                builder.uv(textureCoordinates.get(uvBase), textureCoordinates.get(uvBase + 1));
            }
            builder.endVertex();
        }
        BufferUploader.drawWithShader(builder.end());
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

            // JOML Vector4f exposes public x/y/z/w fields (no getters), unlike the legacy javax.vecmath one.
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

    private static VertexFormat.Mode mapDrawMode(int drawMode) {
        switch (drawMode) {
            case GL_LINES:
                return VertexFormat.Mode.LINES;
            case GL_LINE_STRIP:
                return VertexFormat.Mode.LINE_STRIP;
            case GL_TRIANGLES:
                return VertexFormat.Mode.TRIANGLES;
            case GL_TRIANGLE_STRIP:
                return VertexFormat.Mode.TRIANGLE_STRIP;
            case GL_TRIANGLE_FAN:
                return VertexFormat.Mode.TRIANGLE_FAN;
            case GL_QUADS:
                return VertexFormat.Mode.QUADS;
            case GL_POINTS:
            case GL_LINE_LOOP:
            default:
                // TODO port:1.20.1 - GL_POINTS and GL_LINE_LOOP have no direct VertexFormat.Mode mapping;
                // emit them by expanding to LINES (loop) or by using a dedicated shader path when needed.
                throw new IllegalStateException("drawMode = " + drawMode
                        + " has no direct VertexFormat.Mode mapping in the 1.20.1 core profile.");
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
