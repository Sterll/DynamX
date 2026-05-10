package fr.dynamx.api.network.sync;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.neoforged.fml.LogicalSide;

import java.util.Map;
import java.util.function.BiConsumer;

//TODO NEW SYNC DOC
// TODO port:1.20.1 - fr.dynamx.utils.debug.SyncHelper is not yet ported; inlined a small float
// comparison (epsilon 0.001f, matches SyncHelper.different) so the API doesn't depend on the legacy class.
public class EntityVariable<T> {
    private static final float FLOAT_EPSILON = 0.001f;

    @Getter
    private final BiConsumer<EntityVariable<T>, T> receiveCallback;
    @Getter
    private final SynchronizationRules synchronizationRule;
    @Getter
    private EntityVariableSerializer<T> serializer;
    private T value;
    protected boolean changed = true; //first sync
    @Getter
    private String name = "not_loaded";

    public EntityVariable(SynchronizationRules synchronizationRule, T initialValue) {
        this(null, synchronizationRule, initialValue);
    }

    public EntityVariable(BiConsumer<EntityVariable<T>, T> receiveCallback, SynchronizationRules synchronizationRule) {
        this(receiveCallback, synchronizationRule, null);
    }

    public EntityVariable(BiConsumer<EntityVariable<T>, T> receiveCallback, SynchronizationRules synchronizationRule, T initialValue) {
        this.receiveCallback = receiveCallback;
        this.synchronizationRule = synchronizationRule;
        this.value = initialValue;
    }

    public void init(String name, EntityVariableSerializer<?> type) {
        this.name = name;
        this.serializer = (EntityVariableSerializer<T>) type;
        //System.out.println("INIT " + name+" with " + type);
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        // todo wtf here
        if ((value instanceof Float && floatChanged((Float) value, (Float) this.value)) || (!(value instanceof Float) && value != this.value)) {
            this.value = value;
            changed = true;
        }
    }

    private static boolean floatChanged(Float a, Float b) {
        if (a == null || b == null) return a != b;
        return Math.abs(a - b) > FLOAT_EPSILON;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public void receiveValue(T value) {
        if (receiveCallback != null)
            receiveCallback.accept(this, value);
        if (value instanceof Map) { //TODO PUT IN SEPARATE CLASS
            ((Map) this.value).clear();
            ((Map) this.value).putAll((Map) value);
        } else
            this.value = value;
        setChanged(true); //server will send changes to clients
    }

    // TODO port:1.20.1 - Forge Side replaced by NeoForge LogicalSide.
    public SyncTarget getSyncTarget(SimulationHolder simulationHolder, LogicalSide side) {
        return changed ? synchronizationRule.getSyncTarget(simulationHolder, side) : SyncTarget.NONE;
    }

    public void writeValue(ByteBuf buffer, boolean lightData) {
        serializer.writeObject(buffer, get());
    }

    @Override
    public String toString() {
        return "SynchronizedEntityVariable{" +
                "receiveCallback=" + receiveCallback +
                ", synchronizationRule=" + synchronizationRule +
                ", serializer=" + serializer +
                ", value=" + value +
                ", changed=" + changed +
                ", name='" + name + '\'' +
                '}';
    }
}
