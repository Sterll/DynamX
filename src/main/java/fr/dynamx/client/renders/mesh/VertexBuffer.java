package fr.dynamx.client.renders.mesh;

import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import jme3utilities.Validate;
import jme3utilities.math.MyBuffer;
import jme3utilities.math.MyQuaternion;

import java.nio.FloatBuffer;

/**
 * Wrapper for a vertex attribute (positions, normals or texture coordinates) stored in a
 * direct {@link FloatBuffer}. In the legacy 1.12.2 pipeline this class also owned an OpenGL
 * VBO populated via {@code GL15.glBufferData} and consumed by {@code glVertexPointer} /
 * {@code glNormalPointer} / {@code glTexCoordPointer}.
 * <p>
 * 1.20.1 runs in the core profile where client arrays and named attribute pointers are gone:
 * uploads are performed implicitly by {@code BufferBuilder} when the owning {@link GLMesh}
 * emits its vertices, so this class is now pure CPU-side storage with mutation helpers used
 * by mesh generation code (normals, UV transforms, ragdoll skinning, etc.).
 */
public class VertexBuffer {
    private boolean isModified = true;
    private boolean isMutable = true;
    private final FloatBuffer dataBuffer;
    public final int fpv;
    private final int attribIndex;

    VertexBuffer(float[] data, int fpv, int attribIndex) {
        Validate.nonNull(data, "data");
        Validate.inRange(fpv, "floats per vertex", 1, 4);
        Validate.require(data.length % fpv == 0, "length a multiple of " + fpv);
        this.dataBuffer = BufferUtils.createFloatBuffer(data);
        this.fpv = fpv;
        this.attribIndex = attribIndex;
    }

    VertexBuffer(FloatBuffer data, int fpv, int attribIndex) {
        Validate.nonNull(data, "data");
        Validate.inRange(fpv, "floats per vertex", 1, 4);
        Validate.require(data.capacity() % fpv == 0, "capacity a multiple of " + fpv);
        this.dataBuffer = data;
        this.fpv = fpv;
        this.attribIndex = attribIndex;
    }

    VertexBuffer(int numVertices, int fpv, int attribIndex) {
        Validate.nonNegative(numVertices, "number of vertices");
        Validate.inRange(fpv, "floats per vertex", 1, 4);
        this.dataBuffer = BufferUtils.createFloatBuffer(numVertices * fpv);
        this.fpv = fpv;
        this.attribIndex = attribIndex;
    }

    public int capacity() {
        return dataBuffer.capacity();
    }

    void cleanUp() {
        // No GL resources owned in the 1.20.1 BufferBuilder pipeline.
    }

    public VertexBuffer flip() {
        dataBuffer.flip();
        return this;
    }

    public float get(int position) {
        return dataBuffer.get(position);
    }

    public Vector3f get(int position, Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;
        MyBuffer.get(dataBuffer, position, result);
        return result;
    }

    public FloatBuffer getBuffer() {
        verifyMutable();
        return dataBuffer;
    }

    public int getAttribIndex() {
        return attribIndex;
    }

    public boolean isModified() {
        return isModified;
    }

    public void clearModified() {
        this.isModified = false;
    }

    public int limit() {
        return dataBuffer.limit();
    }

    public VertexBuffer makeImmutable() {
        this.isMutable = false;
        return this;
    }

    public int position() {
        return dataBuffer.position();
    }

    public VertexBuffer put(float fValue) {
        verifyMutable();
        dataBuffer.put(fValue);
        setModified();
        return this;
    }

    public VertexBuffer put(int position, float fValue) {
        verifyMutable();
        dataBuffer.put(position, fValue);
        setModified();
        return this;
    }

    public VertexBuffer put(int position, Vector3f vector) {
        verifyMutable();
        MyBuffer.put(dataBuffer, position, vector);
        setModified();
        return this;
    }

    public VertexBuffer put(Vector3f vector) {
        verifyMutable();
        dataBuffer.put(vector.x);
        dataBuffer.put(vector.y);
        dataBuffer.put(vector.z);
        setModified();
        return this;
    }

    public VertexBuffer rotate(Quaternion quaternion) {
        if (MyQuaternion.isRotationIdentity(quaternion)) {
            return this;
        }
        verifyMutable();
        MyBuffer.rotate(dataBuffer, 0, capacity(), quaternion);
        setModified();
        return this;
    }

    public VertexBuffer scale(float scaleFactor) {
        if (scaleFactor == 1f) {
            return this;
        }
        verifyMutable();
        int numFloats = capacity();
        for (int floatIndex = 0; floatIndex < numFloats; ++floatIndex) {
            put(floatIndex, get(floatIndex) * scaleFactor);
        }
        setModified();
        return this;
    }

    public VertexBuffer setDynamic() {
        // Usage hint was a GL15 concern; BufferBuilder re-emits every frame so it is implicit.
        return this;
    }

    public VertexBuffer setModified() {
        verifyMutable();
        this.isModified = true;
        return this;
    }

    public VertexBuffer transform(Transform transform) {
        verifyMutable();
        if (fpv != GLMesh.numAxes) {
            throw new IllegalStateException("fpv = " + fpv);
        }
        MyBuffer.transform(dataBuffer, 0, capacity(), transform);
        setModified();
        return this;
    }

    public void verifyMutable() {
        if (!isMutable) {
            throw new IllegalStateException("The vertex buffer is no longer mutable.");
        }
    }
}
