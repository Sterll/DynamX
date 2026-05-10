package fr.dynamx.client.renders.mesh;

import jme3utilities.Validate;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Wrapper class for the index buffer of a mesh.
 * <p>
 * TODO port:1.20.1 - GL15 element-array buffer logic must be replaced with BufferBuilder index emission.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DxIndexBuffer extends jme3utilities.lbj.IndexBuffer {
    private boolean isModified = true;
    private final int elementType;
    private Integer vbo;

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
        // TODO port:1.20.1 - replace GL15.glDeleteBuffers with 1.20.1 buffer cleanup
    }

    public DxIndexBuffer clear() {
        getBuffer().clear();
        return this;
    }

    void drawElements(int drawMode) {
        // TODO port:1.20.1 - replace immediate-mode glDrawElements with BufferBuilder/RenderType draw call
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
        // TODO port:1.20.1 - usage hint is implicit in BufferBuilder pipeline
        return this;
    }

    public DxIndexBuffer setModified() {
        verifyMutable();
        this.isModified = true;
        return this;
    }
}
