package fr.dynamx.common.network.sync.variables;

import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.EntityVariableSerializer;
import fr.dynamx.utils.optimization.PooledHashMap;
import io.netty.buffer.ByteBuf;

/**
 * The received value of a synchronized entity variable, before it is applied to the variable.
 */
public class SynchronizedEntityVariableSnapshot<T> {
    private final EntityVariableSerializer<T> serializer;
    private T value;
    private boolean updated;

    public SynchronizedEntityVariableSnapshot(EntityVariableSerializer<T> serializer, T initialValue) {
        this.value = initialValue;
        this.serializer = serializer;
    }

    public T get() {
        return value;
    }

    public void updateVariable(EntityVariable<T> variable) {
        if (!updated) {
            return;
        }
        variable.receiveValue(value);
        updated = false;
        if (value instanceof PooledHashMap) {
            ((PooledHashMap<?, ?>) value).release();
        }
    }

    public void read(ByteBuf buf) {
        value = serializer.readObject(buf);
        updated = true;
    }

    @Override
    public String toString() {
        return "SynchronizedEntityVariableSnapshot{" +
                "serializer=" + serializer +
                ", value=" + value +
                ", updated=" + updated +
                '}';
    }
}
