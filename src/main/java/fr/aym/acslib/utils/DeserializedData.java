// TODO port:1.20.1 stub - DeserializedData wraps raw NBT-deserialized data
package fr.aym.acslib.utils;

import java.util.List;

public class DeserializedData {
    private final int version;
    private final List<Object> data;

    public DeserializedData(int version, List<Object> data) {
        this.version = version;
        this.data = data;
    }

    public int getVersion() { return version; }
    public List<Object> getData() { return data; }

    @SuppressWarnings("unchecked")
    public <T> T get(int idx) {
        return data == null ? null : (T) data.get(idx);
    }
}
