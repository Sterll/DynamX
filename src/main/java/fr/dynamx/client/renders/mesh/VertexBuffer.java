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
 * Wrapper class for a named attribute in a mesh, including its data buffer.
 * <p>
 * TODO port:1.20.1 - VBO/attribute pointer setup must be reworked to use BufferBuilder/VertexFormat,
 * the legacy GL15/GL_ARRAY_BUFFER + glVertexPointer pipeline is gone in 1.20.1 core profile.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class VertexBuffer {
    private boolean isModified = true;
    private boolean isMutable = true;
    private final FloatBuffer dataBuffer;
    public final int fpv;
    private Integer vbo;
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
        // TODO port:1.20.1 - replace GL15.glDeleteBuffers with 1.20.1 buffer cleanup
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

    void prepareToDraw() {
        // TODO port:1.20.1 - replace immediate mode VBO bind + glVertex/Normal/TexCoordPointer with BufferBuilder/RenderType
    }

    void stopDraw() {
        // TODO port:1.20.1 - replace immediate mode client-state disable with no-op (BufferBuilder is self-contained)
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
        // TODO port:1.20.1 - usage hint is implicit in BufferBuilder pipeline
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

    public void unbindVbo() {
        // TODO port:1.20.1 - explicit VBO unbind unnecessary in BufferBuilder pipeline
    }
}
