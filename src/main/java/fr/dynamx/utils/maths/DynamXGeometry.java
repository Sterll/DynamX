package fr.dynamx.utils.maths;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.DynamX;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;

/**
 * General geometrical operations with Vector3f and Quaternions
 *
 * @see DynamXMath
 */
public class DynamXGeometry {

    public static final Vector3f FORWARD_DIRECTION = new Vector3f(0.0f, 0.0f, 1.0f);
    public static final Vector3f LEFT_DIRECTION = new Vector3f(1.0f, 0.0f, 0.0f);
    public static final Vector3f UP_DIRECTION = new Vector3f(0.0f, 1.0f, 0.0f);

    public static final double SINGULARITY_NORTH_POLE = 0.49999;
    public static final double SINGULARITY_SOUTH_POLE = -0.49999;

    public static float angle(Vector3f t, Vector3f v1) {
        double vDot = t.dot(v1) / (t.length() * v1.length());
        if (vDot < -1.0) vDot = -1.0;
        if (vDot > 1.0) vDot = 1.0;
        return ((float) (Math.acos(vDot)));
    }

    public static Vector3f getRotatedPoint(Vector3f pos, float pitch, float yaw, float roll) {
        double a = Math.cos(pitch * 0.017453292F);
        double b = Math.sin(pitch * 0.017453292F);
        double c = Math.cos(yaw * 0.017453292F);
        double d = Math.sin(yaw * 0.017453292F);
        double e = Math.cos(roll * 0.017453292F);
        double f = Math.sin(roll * 0.017453292F);

        float x = (float) (pos.x * (c * e - b * d * f) + pos.y * (-b * d * e - c * f) + pos.z * (-a * d));
        float y = (float) (pos.x * (a * f) + pos.y * (a * e) + pos.z * (-b));
        float z = (float) (pos.x * (d * e + b * c * f) + pos.y * (b * c * e - d * f) + pos.z * (a * c));

        return Vector3fPool.get(x, y, z);
    }

    public static double[] getRotatedPoint(double[] pos, double pitch, double yaw, double roll) {
        double a = Math.cos(pitch * Math.PI / 180);
        double b = Math.sin(pitch * Math.PI / 180);
        double c = Math.cos(yaw * Math.PI / 180);
        double d = Math.sin(yaw * Math.PI / 180);
        double e = Math.cos(roll * Math.PI / 180);
        double f = Math.sin(roll * Math.PI / 180);

        double x = (pos[0] * (c * e - b * d * f) + pos[1] * (-b * d * e - c * f) + pos[2] * (-a * d));
        double y = (pos[0] * (a * f) + pos[1] * (a * e) + pos[2] * (-b));
        double z = (pos[0] * (d * e + b * c * f) + pos[1] * (b * c * e - d * f) + pos[2] * (a * c));

        return new double[]{x, y, z};
    }

    public static Vector3f toAngles(Quaternion quat) {
        Vector3f euler = new Vector3f();
        float sqw = quat.getW() * quat.getW();
        float sqx = quat.getX() * quat.getX();
        float sqy = quat.getY() * quat.getY();
        float sqz = quat.getZ() * quat.getZ();
        float unit = sqx + sqy + sqz + sqw;
        float test = quat.getX() * quat.getY() + quat.getZ() * quat.getW();
        if (test > 0.499 * unit) {
            euler.y = 2 * FastMath.atan2(quat.getX(), quat.getW());
            euler.z = FastMath.HALF_PI;
            euler.x = 0;
        } else if (test < -0.499 * unit) {
            euler.y = -2 * FastMath.atan2(quat.getX(), quat.getW());
            euler.z = -FastMath.HALF_PI;
            euler.x = 0;
        } else {
            euler.y = FastMath.atan2(2 * quat.getY() * quat.getW() - 2 * quat.getX() * quat.getZ(), sqx - sqy - sqz + sqw);
            euler.z = (float) Math.asin(2 * test / unit);
            euler.x = FastMath.atan2(2 * quat.getX() * quat.getW() - 2 * quat.getY() * quat.getZ(), -sqx + sqy - sqz + sqw);
        }
        return euler;
    }

    public static Vector3f quaternionToEuler(Quaternion quat) {
        return new Vector3f(toYaw(quat), toPitch(quat), toRoll(quat));
    }

    private static float toRoll(Quaternion quat) {
        double test = quat.getX() * quat.getY() + quat.getZ() * quat.getW();
        if (test > SINGULARITY_NORTH_POLE) return 0;
        if (test < SINGULARITY_SOUTH_POLE) return 0;
        return (float) Math.atan2(
                2 * quat.getX() * quat.getW() - 2 * quat.getY() * quat.getZ(),
                1 - 2 * quat.getX() * quat.getX() - 2 * quat.getZ() * quat.getZ());
    }

    private static float toPitch(Quaternion quat) {
        double test = quat.getX() * quat.getY() + quat.getZ() * quat.getW();
        if (test > SINGULARITY_NORTH_POLE) return (float) (Math.PI / 2);
        if (test < SINGULARITY_SOUTH_POLE) return (float) (-Math.PI / 2);
        return (float) Math.asin(2 * test);
    }

    private static float toYaw(Quaternion quat) {
        double test = quat.getX() * quat.getY() + quat.getZ() * quat.getW();
        if (test > SINGULARITY_NORTH_POLE) return (float) (2 * Math.atan2(quat.getX(), quat.getW()));
        if (test < SINGULARITY_SOUTH_POLE) return (float) (-2 * Math.atan2(quat.getX(), quat.getW()));
        return (float) Math.atan2(
                2 * quat.getY() * quat.getW() - 2 * quat.getX() * quat.getZ(),
                1 - 2 * quat.getY() * quat.getY() - 2 * quat.getZ() * quat.getZ());
    }

    public static Quaternion rotationYawToQuaternion(int horizontalDir) {
        double pitch = horizontalDir * -22.5f * Math.PI / 180f;
        double cp = Math.cos(pitch * 0.5);
        double sp = Math.sin(pitch * 0.5);
        return QuaternionPool.get(0f, (float) sp, 0f, (float) cp);
    }

    public static Vector3f rotateVectorByQuaternion(Vector3f v, Quaternion q) {
        Vector3f u = Vector3fPool.get(q.getX(), q.getY(), q.getZ());
        float s = q.getW();
        Vector3f v1 = Vector3fPool.get(u);
        v1.multLocal(2 * u.dot(v));
        Vector3f v2 = Vector3fPool.get(v);
        v2.multLocal(s * s - u.dot(u));
        v1.addLocal(v2);
        v2.set(u.cross(v, v2));
        v1.addLocal(v2.multLocal(2 * s));
        return v1;
    }

    public static Quaternion rotationYawToQuaternion(float yawDeg) {
        return QuaternionPool.get().fromAngleNormalAxis((float) Math.toRadians(-yawDeg), new Vector3f(0, 1, 0));
    }

    public static Quaternion eulerToQuaternion(float roll, float yaw, float pitch) {
        return QuaternionPool.get().fromAngles((float) Math.toRadians(-pitch), (float) Math.toRadians(-yaw), (float) Math.toRadians(-roll));
    }

    public static double distanceBetween(Vector3f vec, Vector3f vec2) {
        double d0 = vec2.x - vec.x;
        double d1 = vec2.y - vec.y;
        double d2 = vec2.z - vec.z;
        return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
    }

    public static void normalizeVector(Vector3f vecToNormalize) {
        float length = vecToNormalize.length();
        if (length != 1f && length != 0f) {
            length = 1.0f / FastMath.sqrt(length);
            vecToNormalize.set(vecToNormalize.x * length, vecToNormalize.y * length, vecToNormalize.z * length);
        }
    }

    public static Quaternion inverseQuaternion(Quaternion quat, Quaternion result) {
        if (result == null) result = new Quaternion();
        float norm = quat.norm();
        if (norm > 0.0) {
            float invNorm = 1.0f / norm;
            result.set(-quat.getX() * invNorm, -quat.getY() * invNorm, -quat.getZ() * invNorm, quat.getW() * invNorm);
        }
        return result;
    }

    public static Vector3f getCenter(Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4) {
        Vector3f min = Vector3fPool.get(DynamXMath.getMin(p1.x, p2.x, p3.x, p4.x), DynamXMath.getMin(p1.y, p2.y, p3.y, p4.y), DynamXMath.getMin(p1.z, p2.z, p3.z, p4.z));
        return Vector3fPool.get(
                (DynamXMath.getMax(p1.x, p2.x, p3.x, p4.x) - min.x) / 2,
                (DynamXMath.getMax(p1.y, p2.y, p3.y, p4.y) - min.y) / 2,
                (DynamXMath.getMax(p1.z, p2.z, p3.z, p4.z) - min.z) / 2).addLocal(min);
    }

    public static Vector3f getRotationColumn(Quaternion quaternion, int i) {
        return getRotationColumn(quaternion, i, null);
    }

    public static Vector3f getRotationColumn(Quaternion quaternion, int i, Vector3f store) {
        if (store == null) store = Vector3fPool.get();
        float norm = quaternion.norm();
        if (norm != 1.0f) norm = DynamXMath.invSqrt(norm);

        float xx = quaternion.getX() * quaternion.getX() * norm;
        float xy = quaternion.getX() * quaternion.getY() * norm;
        float xz = quaternion.getX() * quaternion.getZ() * norm;
        float xw = quaternion.getX() * quaternion.getW() * norm;
        float yy = quaternion.getY() * quaternion.getY() * norm;
        float yz = quaternion.getY() * quaternion.getZ() * norm;
        float yw = quaternion.getY() * quaternion.getW() * norm;
        float zz = quaternion.getZ() * quaternion.getZ() * norm;
        float zw = quaternion.getZ() * quaternion.getW() * norm;

        switch (i) {
            case 0:
                store.x = 1 - 2 * (yy + zz);
                store.y = 2 * (xy + zw);
                store.z = 2 * (xz - yw);
                break;
            case 1:
                store.x = 2 * (xy - zw);
                store.y = 1 - 2 * (xx + zz);
                store.z = 2 * (yz + xw);
                break;
            case 2:
                store.x = 2 * (xz + yw);
                store.y = 2 * (yz - xw);
                store.z = 1 - 2 * (xx + yy);
                break;
            default:
                DynamX.LOGGER.warn("Invalid column index.");
                throw new IllegalArgumentException("Invalid column index. " + i);
        }
        return store;
    }

    public static float getYawFromRotationVector(Vector3f forwardRotationVector) {
        Vector3f horizontalRotationVec = Vector3fPool.get(forwardRotationVector.x, 0, forwardRotationVector.z);
        Vector3f tmpVector = LEFT_DIRECTION.clone();
        float yaw = (horizontalRotationVec.length() == 0 ? 0 : DynamXGeometry.angle(tmpVector, horizontalRotationVec) + FastMath.HALF_PI);
        tmpVector.cross(horizontalRotationVec, tmpVector);
        if (tmpVector.y < 0) yaw = FastMath.PI - yaw;
        return (float) Math.toDegrees(-yaw);
    }

    public static float getPitchFromRotationVector(Vector3f forwardRotationVector) {
        return (float) Math.toDegrees(FastMath.HALF_PI - DynamXGeometry.angle(UP_DIRECTION, forwardRotationVector));
    }

    public static float getRollFromRotationVector(Vector3f leftRotationVector, Vector3f forwardRotationVector) {
        Vector3f tmpVector = Vector3fPool.get();
        UP_DIRECTION.cross(forwardRotationVector, tmpVector);
        float roll = -DynamXGeometry.angle(tmpVector, leftRotationVector);
        if (leftRotationVector.y > 0) roll = -roll;
        return (float) Math.toDegrees(roll);
    }

    public static void toSpherical(Vector3f vec) {
        double xx = vec.x;
        double yy = vec.y;
        float rxy = (float) Math.sqrt(xx * xx + yy * yy);
        float phi = FastMath.atan2(rxy, vec.z);
        float theta = FastMath.atan2(vec.y, vec.x);
        float r = vec.length();

        vec.x = r;
        vec.y = theta;
        vec.z = phi;
    }
}
