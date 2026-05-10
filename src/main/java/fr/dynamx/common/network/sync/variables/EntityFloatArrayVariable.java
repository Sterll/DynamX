package fr.dynamx.common.network.sync.variables;

import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;

import java.util.function.BiConsumer;

public class EntityFloatArrayVariable extends EntityVariable<float[]> {
    private static final float FLOAT_EPSILON = 0.001f;

    public EntityFloatArrayVariable(SynchronizationRules synchronizationRule, float[] initialValue) {
        super(synchronizationRule, initialValue);
    }

    public EntityFloatArrayVariable(BiConsumer<EntityVariable<float[]>, float[]> receiveCallback, SynchronizationRules synchronizationRule, float[] initialValue) {
        super(receiveCallback, synchronizationRule, initialValue);
    }

    public float get(int i) {
        return get()[i];
    }

    public void set(int i, float value) {
        // TODO port:1.20.1 - SyncHelper.different not yet ported; inlined epsilon compare.
        if (changed || Math.abs(get()[i] - value) > FLOAT_EPSILON) {
            get()[i] = value;
            setChanged(true);
        }
    }
}
