package fr.dynamx.utils.optimization;

import org.joml.Quaternionf;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class GlQuaternionPool extends ClassPool<Quaternionf> {
    private static final GlQuaternionPool INSTANCE = new GlQuaternionPool();

    public static void openPool() {
        getINSTANCE().openSubPool(SubClassPool.GL_QUATERNION_DEFAULT);
    }

    public static void openPool(String identifier) {
        getINSTANCE().openSubPool(identifier);
    }

    public static void closePool() {
        getINSTANCE().closeSubPool();
    }

    public static Quaternionf get() {
        Quaternionf v = getINSTANCE().provideNewInstance();
        v.set(0, 0, 0, 0);
        return v;
    }

    public static Quaternionf get(Quaternionf from) {
        Quaternionf v = getINSTANCE().provideNewInstance();
        v.set(from);
        return v;
    }

    public static Quaternionf get(com.jme3.math.Quaternion from) {
        Quaternionf v = getINSTANCE().provideNewInstance();
        v.set(from.getX(), from.getY(), from.getZ(), from.getW());
        return v;
    }

    public GlQuaternionPool() {
        super(100, 10);
    }

    @Override
    public Quaternionf[] createNewPool(int newInstancesStart, int size) {
        Quaternionf[] pool = new Quaternionf[size];
        for (int i = newInstancesStart; i < size; i++)
            pool[i] = new Quaternionf();
        return pool;
    }

    @Override
    public int getGrowthSize() {
        return 10;
    }

    public static GlQuaternionPool getINSTANCE() {
        return INSTANCE;
    }

    public static Quaternionf newGlQuaternion(com.jme3.math.Quaternion fromJmeQuaternion) {
        return fromJmeQuaternion == null ? null : new Quaternionf(fromJmeQuaternion.getX(), fromJmeQuaternion.getY(), fromJmeQuaternion.getZ(), fromJmeQuaternion.getW());
    }
}
