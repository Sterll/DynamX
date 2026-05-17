package fr.dynamx.client.renders.mesh;

import jme3utilities.Validate;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Index buffer of a {@link GLMesh}. In 1.12.2 this class owned an
 * {@code GL_ELEMENT_ARRAY_BUFFER} VBO and issued {@code glDrawElements} directly. In the
 * 1.20.1 core profile the draw is performed by {@link GLMesh} through {@code BufferBuilder},
 * so this class is now a CPU-side index store with a tagged element width (byte / short / int)
 * inherited from {@code jme3utilities.lbj.IndexBuffer}.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DxIndexBuffer extends jme3utilities.lbj.IndexBuffer {
    private boolean isModified = true;
    private final int elementType;

    public DxIndexBuffer(int maxVertices, int capacity) {
        super(maxVertices, capacity);
        Validate.nonNegative(maxVertices, "max vertices");
        Validate.nonNegative(capacity, "capacity");

        Buffer buffer = super.getBuffer();
        if (buffer instanceof ByteBuffer) {
            this.elementType = 0x1401; // GL_UNSIGNED_BYTE
        } else if (buffer instanceof ShortBuffer) {
            this.elementType = 0x1403; // GL_UNSIGNED_SHORT
        } else {
            assert buffer instanceof IntBuffer;
            this.elementType = 0x1405; // GL_UNSIGNED_INT
        }
    }

    public int capacity() {
        return getBuffer().capacity();
    }

    void cleanUp() {
        // No GL resources owned in the 1.20.1 BufferBuilder pipeline.
    }

    public DxIndexBuffer clear() {
        getBuffer().clear();
        return this;
    }

    public DxIndexBuffer flip() {
        getBuffer().flip();
        return this;
    }

    public boolean isReadOnly() {
        return getBuffer().isReadOnly();
    }

    public int limit() {
        return getBuffer().limit();
    }

    @Override
    public DxIndexBuffer makeImmutable() {
        super.makeImmutable();
        return this;
    }

    public int position() {
        return getBuffer().position();
    }

    public DxIndexBuffer position(int newPosition) {
        getBuffer().position(newPosition);
        return this;
    }

    @Override
    public DxIndexBuffer put(int index) {
        super.put(index);
        setModified();
        return this;
    }

    public DxIndexBuffer rewind() {
        getBuffer().rewind();
        return this;
    }

    public DxIndexBuffer setDynamic() {
        // Usage hint was a GL15 concern; BufferBuilder re-emits each frame so it is implicit.
        return this;
    }

    public DxIndexBuffer setModified() {
        verifyMutable();
        this.isModified = true;
        return this;
    }

    public boolean isModifiedFlag() {
        return isModified;
    }

    public int getElementType() {
        return elementType;
    }

    /**
     * Read an index value at the given position regardless of the underlying buffer width
     * (byte / short / int). Used by {@link GLMesh} when emitting an indexed primitive through
     * {@code BufferBuilder}.
     */
    public int getIndex(int position) {
        Buffer buffer = getBuffer();
        if (buffer instanceof ByteBuffer) {
            return ((ByteBuffer) buffer).get(position) & 0xFF;
        }
        if (buffer instanceof ShortBuffer) {
            return ((ShortBuffer) buffer).get(position) & 0xFFFF;
        }
        return ((IntBuffer) buffer).get(position);
    }
}
