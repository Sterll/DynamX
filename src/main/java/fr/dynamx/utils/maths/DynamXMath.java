package fr.dynamx.utils.maths;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.util.Mth;

/**
 * General math and interpolation methods
 *
 * @see DynamXGeometry
 */
public class DynamXMath {
    public static final float TO_RADIAN = FastMath.PI / 180.f;

    public static float interpolateLinear(float scale, float startValue, float endValue) {
        if (startValue == endValue) return startValue;
        if (scale <= 0f) return startValue;
        if (scale >= 1f) return endValue;
        return ((1f - scale) * startValue) + (scale * endValue);
    }

    public static Vector3f interpolateLinear(float scale, Vector3f startValue, Vector3f endValue, Vector3f store) {
        if (store == null) store = Vector3fPool.get();
        store.x = interpolateLinear(scale, startValue.x, endValue.x);
        store.y = interpolateLinear(scale, startValue.y, endValue.y);
        store.z = interpolateLinear(scale, startValue.z, endValue.z);
        return store;
    }

    public static Vector3f interpolateLinear(float scale, Vector3f startValue, Vector3f endValue) {
        return interpolateLinear(scale, startValue, endValue, null);
    }

    public static Quaternion slerp(float scale, Quaternion startValue, Quaternion endValue) {
        return slerp(scale, startValue, endValue, QuaternionPool.get());
    }

    public static Quaternion slerp(float scale, Quaternion startValue, Quaternion endValue, Quaternion store) {
        // TODO port:1.20.1 - Libbulletjme's stripped Quaternion has no slerp(); delegate to jme3utilities MyQuaternion.
        return jme3utilities.math.MyQuaternion.slerp(scale, startValue, endValue, store);
    }

    public static float[] interpolateAngle(float netAngle, float entityAngle, int step) {
        double da = Mth.wrapDegrees(netAngle - entityAngle);
        float newAngle = (float) (entityAngle + da / step);
        if (newAngle > 180) {
            newAngle -= 360;
            entityAngle -= 360;
        } else if (newAngle < -180) {
            newAngle += 360;
            entityAngle += 360;
        }
        return new float[]{entityAngle, newAngle};
    }

    public static double interpolateDoubleDelta(double netValue, double value, int step) {
        return (netValue - value) / (double) (step);
    }

    public static float clamp(float input, float min, float max) {
        return (input < min) ? min : Math.min(input, max);
    }

    public static float normalize(float value, float min, float max) {
        return (value - min) / (max - min);
    }

    public static float normalizeBetween(float value, float valueRangeMin, float valueRangeMax, float valueMin, float valueMax) {
        return normalize(value, valueRangeMin, valueRangeMax) * (valueMax - valueMin) + valueMin;
    }

    public static float invSqrt(float fValue) {
        return (float) (1.0f / Math.sqrt(fValue));
    }

    public static float getMin(float a, float b, float c, float d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    public static float getMax(float a, float b, float c, float d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    public static double getMin(double a, double b, double c, double d, double e, double f) {
        return Math.min(Math.min(Math.min(a, b), Math.min(c, d)), Math.min(e, f));
    }

    public static double getMax(double a, double b, double c, double d, double e, double f) {
        return Math.max(Math.max(Math.max(a, b), Math.max(c, d)), Math.max(e, f));
    }

    public static int preciseRound(double of) {
        int c = (int) of;
        return of - c > 0.5 ? c + 1 : of - c < -0.5 ? c - 1 : c;
    }

    public static float roundFloat(float f, float precision) {
        return (float) Math.round(f * precision) / precision;
    }
}
