package fr.dynamx.utils.optimization;

import com.jme3.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.io.Serializable;

public class MutableBoundingBox implements Serializable {
    //For terrain save, don't touch !
    private static final long serialVersionUID = -9207351705409830894L;

    /**
     * The minimum X coordinate of this bounding box. Guaranteed to always be less than or equal to {@link #maxX}.
     */
    public double minX;
    /**
     * The minimum Y coordinate of this bounding box. Guaranteed to always be less than or equal to {@link #maxY}.
     */
    public double minY;
    /**
     * The minimum Y coordinate of this bounding box. Guaranteed to always be less than or equal to {@link #maxZ}.
     */
    public double minZ;
    /**
     * The maximum X coordinate of this bounding box. Guaranteed to always be greater than or equal to {@link #minX}.
     */
    public double maxX;
    /**
     * The maximum Y coordinate of this bounding box. Guaranteed to always be greater than or equal to {@link #minY}.
     */
    public double maxY;
    /**
     * The maximum Z coordinate of this bounding box. Guaranteed to always be greater than or equal to {@link #minZ}.
     */
    public double maxZ;

    public MutableBoundingBox(double x1, double y1, double z1, double x2, double y2, double z2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public MutableBoundingBox(Vector3f vec) {
        this(vec.x, vec.y, vec.z, -vec.x, -vec.y, -vec.z);
    }

    public MutableBoundingBox(BlockPos pos1, BlockPos pos2) {
        this(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
    }

    public MutableBoundingBox(Vec3 min, Vec3 max) {
        this(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    public MutableBoundingBox(AABB from) {
        this(from.minX, from.minY, from.minZ, from.maxX, from.maxY, from.maxZ);
    }

    public MutableBoundingBox(MutableBoundingBox from) {
        this(from.minX, from.minY, from.minZ, from.maxX, from.maxY, from.maxZ);
    }

    public MutableBoundingBox() {
        this(0, 0, 0, 0, 0, 0);
    }

    public void setTo(MutableBoundingBox from) {
        this.minX = from.minX;
        this.maxX = from.maxX;
        this.minY = from.minY;
        this.maxY = from.maxY;
        this.minZ = from.minZ;
        this.maxZ = from.maxZ;
    }

    public void setTo(AABB from) {
        this.minX = from.minX;
        this.maxX = from.maxX;
        this.minY = from.minY;
        this.maxY = from.maxY;
        this.minZ = from.minZ;
        this.maxZ = from.maxZ;
    }

    public boolean equals(Object p_equals_1_) {
        if (this == p_equals_1_) {
            return true;
        } else if (p_equals_1_ instanceof AABB) {
            AABB axisalignedbb = (AABB) p_equals_1_;
            if (Double.compare(axisalignedbb.minX, this.minX) != 0) {
                return false;
            } else if (Double.compare(axisalignedbb.minY, this.minY) != 0) {
                return false;
            } else if (Double.compare(axisalignedbb.minZ, this.minZ) != 0) {
                return false;
            } else if (Double.compare(axisalignedbb.maxX, this.maxX) != 0) {
                return false;
            } else if (Double.compare(axisalignedbb.maxY, this.maxY) != 0) {
                return false;
            } else {
                return Double.compare(axisalignedbb.maxZ, this.maxZ) == 0;
            }
        } else if (p_equals_1_ instanceof MutableBoundingBox) {
            MutableBoundingBox axisalignedbb = (MutableBoundingBox) p_equals_1_;
            if (Double.compare(axisalignedbb.minX, this.minX) != 0) {
                return false;
            } else if (Double.compare(axisalignedbb.minY, this.minY) != 0) {
                return false;
            } else if (Double.compare(axisalignedbb.minZ, this.minZ) != 0) {
                return false;
            } else if (Double.compare(axisalignedbb.maxX, this.maxX) != 0) {
                return false;
            } else if (Double.compare(axisalignedbb.maxY, this.maxY) != 0) {
                return false;
            } else {
                return Double.compare(axisalignedbb.maxZ, this.maxZ) == 0;
            }
        }
        return false;
    }

    public int hashCode() {
        long i = Double.doubleToLongBits(this.minX);
        int j = (int) (i ^ i >>> 32);
        i = Double.doubleToLongBits(this.minY);
        j = 31 * j + (int) (i ^ i >>> 32);
        i = Double.doubleToLongBits(this.minZ);
        j = 31 * j + (int) (i ^ i >>> 32);
        i = Double.doubleToLongBits(this.maxX);
        j = 31 * j + (int) (i ^ i >>> 32);
        i = Double.doubleToLongBits(this.maxY);
        j = 31 * j + (int) (i ^ i >>> 32);
        i = Double.doubleToLongBits(this.maxZ);
        j = 31 * j + (int) (i ^ i >>> 32);
        return j;
    }

    public void scale(double x, double y, double z) {
        minX *= x;
        minY *= y;
        minZ *= z;
        maxX *= x;
        maxY *= y;
        maxZ *= z;
    }

    /**
     * Creates a new {@link AABB} that has been contracted by the given amount, with positive changes
     * decreasing max values and negative changes increasing min values.
     * <br>
     * If the amount to contract by is larger than the length of a side, then the side will wrap (still creating a valid
     * AABB - see last sample).
     *
     * <h3>Samples:</h3>
     * <table>
     * <tr><th>Input</th><th>Result</th></tr>
     * <tr><td><pre><code>new AxisAlignedBB(0, 0, 0, 4, 4, 4).contract(2, 2, 2)</code></pre></td><td><pre><samp>box[0.0,
     * 0.0, 0.0 -> 2.0, 2.0, 2.0]</samp></pre></td></tr>
     * <tr><td><pre><code>new AxisAlignedBB(0, 0, 0, 4, 4, 4).contract(-2, -2, -
     * 2)</code></pre></td><td><pre><samp>box[2.0, 2.0, 2.0 -> 4.0, 4.0, 4.0]</samp></pre></td></tr>
     * <tr><td><pre><code>new AxisAlignedBB(5, 5, 5, 7, 7, 7).contract(0, 1, -
     * 1)</code></pre></td><td><pre><samp>box[5.0, 5.0, 6.0 -> 7.0, 6.0, 7.0]</samp></pre></td></tr>
     * <tr><td><pre><code>new AxisAlignedBB(-2, -2, -2, 2, 2, 2).contract(4, -4,
     * 0)</code></pre></td><td><pre><samp>box[-8.0, 2.0, -2.0 -> -2.0, 8.0, 2.0]</samp></pre></td></tr>
     * </table>
     *
     * <h3>See Also:</h3>
     * <ul>
     * <li>{@link #expand(double, double, double)} - like this, except for expanding.</li>
     * <li>{@link #grow(double, double, double)} and {@link #grow(double)} - expands in all directions.</li>
     * <li>{@link #shrink(double)} - contracts in all directions (like {@link #grow(double)})</li>
     * </ul>
     */
    public void contract(double x, double y, double z) {
        if (x < 0.0D) {
            minX -= x;
        } else if (x > 0.0D) {
            maxX -= x;
        }

        if (y < 0.0D) {
            minY -= y;
        } else if (y > 0.0D) {
            maxY -= y;
        }

        if (z < 0.0D) {
            minZ -= z;
        } else if (z > 0.0D) {
            maxZ -= z;
        }
    }

    /**
     * Creates a new {@link AABB} that has been expanded by the given amount, with positive changes increasing
     * max values and negative changes decreasing min values.
     *
     * <h3>Samples:</h3>
     * <table>
     * <tr><th>Input</th><th>Result</th></tr>
     * <tr><td><pre><code>new AxisAlignedBB(0, 0, 0, 1, 1, 1).expand(2, 2, 2)</code></pre></td><td><pre><samp>box[0, 0,
     * 0 -> 3, 3, 3]</samp></pre></td><td>
     * <tr><td><pre><code>new AxisAlignedBB(0, 0, 0, 1, 1, 1).expand(-2, -2, -2)</code></pre></td><td><pre><samp>box[-2,
     * -2, -2 -> 1, 1, 1]</samp></pre></td><td>
     * <tr><td><pre><code>new AxisAlignedBB(5, 5, 5, 7, 7, 7).expand(0, 1, -1)</code></pre></td><td><pre><samp>box[5, 5,
     * 4, 7, 8, 7]</samp></pre></td><td>
     * </table>
     *
     * <h3>See Also:</h3>
     * <ul>
     * <li>{@link #contract(double, double, double)} - like this, except for shrinking.</li>
     * <li>{@link #grow(double, double, double)} and {@link #grow(double)} - expands in all directions.</li>
     * <li>{@link #shrink(double)} - contracts in all directions (like {@link #grow(double)})</li>
     * </ul>
     * <br>
     * This bounding box is modified and will always be equal or greater in volume to this bounding box.
     */
    public void expand(double x, double y, double z) {
        if (x < 0.0D) {
            minX += x;
        } else if (x > 0.0D) {
            maxX += x;
        }

        if (y < 0.0D) {
            minY += y;
        } else if (y > 0.0D) {
            maxY += y;
        }

        if (z < 0.0D) {
            minZ += z;
        } else if (z > 0.0D) {
            maxZ += z;
        }
    }

    /**
     * Creates a new {@link AABB} that has been contracted by the given amount in both directions. Negative
     * values will shrink the AABB instead of expanding it.
     * <br>
     * Side lengths will be increased by 2 times the value of the parameters, since both min and max are changed.
     * <br>
     * If contracting and the amount to contract by is larger than the length of a side, then the side will wrap (still
     * creating a valid AABB - see last ample).
     *
     * <h3>Samples:</h3>
     * <table>
     * <tr><th>Input</th><th>Result</th></tr>
     * <tr><td><pre><code>new AxisAlignedBB(0, 0, 0, 1, 1, 1).grow(2, 2, 2)</code></pre></td><td><pre><samp>box[-2.0, -
     * 2.0, -2.0 -> 3.0, 3.0, 3.0]</samp></pre></td></tr>
     * <tr><td><pre><code>new AxisAlignedBB(0, 0, 0, 6, 6, 6).grow(-2, -2, -2)</code></pre></td><td><pre><samp>box[2.0,
     * 2.0, 2.0 -> 4.0, 4.0, 4.0]</samp></pre></td></tr>
     * <tr><td><pre><code>new AxisAlignedBB(5, 5, 5, 7, 7, 7).grow(0, 1, -1)</code></pre></td><td><pre><samp>box[5.0,
     * 4.0, 6.0 -> 7.0, 8.0, 6.0]</samp></pre></td></tr>
     * <tr><td><pre><code>new AxisAlignedBB(1, 1, 1, 3, 3, 3).grow(-4, -2, -3)</code></pre></td><td><pre><samp>box[-1.0,
     * 1.0, 0.0 -> 5.0, 3.0, 4.0]</samp></pre></td></tr>
     * </table>
     *
     * <h3>See Also:</h3>
     * <ul>
     * <li>{@link #expand(double, double, double)} - expands in only one direction.</li>
     * <li>{@link #contract(double, double, double)} - contracts in only one direction.</li>
     * <lu>{@link #grow(double)} - version of this that expands in all directions from one parameter.</li>
     * <li>{@link #shrink(double)} - contracts in all directions</li>
     * </ul>
     */
    public void grow(double x, double y, double z) {
        this.minX -= x;
        this.minY -= y;
        this.minZ -= z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
    }

    /**
     * Creates a new {@link AABB} that is expanded by the given value in all directions. Equivalent to {@link
     * #grow(double, double, double)} with the given value for all 3 params. Negative values will shrink the AABB.
     * <br>
     * Side lengths will be increased by 2 times the value of the parameter, since both min and max are changed.
     * <br>
     * If contracting and the amount to contract by is larger than the length of a side, then the side will wrap (still
     * creating a valid AABB - see samples on {@link #grow(double, double, double)}).
     */
    public MutableBoundingBox grow(double value) {
        this.grow(value, value, value);
        return this;
    }

    /**
     * Checks if the bounding box intersects with another.
     */
    public boolean intersects(MutableBoundingBox other) {
        return this.intersects(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
    }

    public boolean intersects(AABB other) {
        return this.intersects(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
    }

    public boolean intersects(double x1, double y1, double z1, double x2, double y2, double z2) {
        return this.minX < x2 && this.maxX > x1 && this.minY < y2 && this.maxY > y1 && this.minZ < z2 && this.maxZ > z1;
    }

    /**
     * Returns if the supplied Vec3D is completely inside the bounding box
     */
    public boolean contains(Vec3 vec) {
        if (vec.x > this.minX && vec.x < this.maxX) {
            if (vec.y > this.minY && vec.y < this.maxY) {
                return vec.z > this.minZ && vec.z < this.maxZ;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean contains(Vec3i vec) {
        if (vec.getX() > this.minX && vec.getX() < this.maxX) {
            if (vec.getY() > this.minY && vec.getY() < this.maxY) {
                return vec.getZ() > this.minZ && vec.getZ() < this.maxZ;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Creates a new {@link AABB} that is expanded by the given value in all directions. Equivalent to {@link
     * #grow(double)} with value set to the negative of the value provided here. Passing a negative value to this method
     * values will grow the AABB.
     * <br>
     * Side lengths will be decreased by 2 times the value of the parameter, since both min and max are changed.
     * <br>
     * If contracting and the amount to contract by is larger than the length of a side, then the side will wrap (still
     * creating a valid AABB - see samples on {@link #grow(double, double, double)}).
     */
    public void shrink(double value) {
        this.grow(-value);
    }

    @Nullable
    public BlockHitResult calculateIntercept(Vec3 vecA, Vec3 vecB) {
        Vec3 vec3d = this.collideWithXPlane(this.minX, vecA, vecB);
        Direction enumfacing = Direction.WEST;
        Vec3 vec3d1 = this.collideWithXPlane(this.maxX, vecA, vecB);

        if (vec3d1 != null && this.isClosest(vecA, vec3d, vec3d1)) {
            vec3d = vec3d1;
            enumfacing = Direction.EAST;
        }

        vec3d1 = this.collideWithYPlane(this.minY, vecA, vecB);

        if (vec3d1 != null && this.isClosest(vecA, vec3d, vec3d1)) {
            vec3d = vec3d1;
            enumfacing = Direction.DOWN;
        }

        vec3d1 = this.collideWithYPlane(this.maxY, vecA, vecB);

        if (vec3d1 != null && this.isClosest(vecA, vec3d, vec3d1)) {
            vec3d = vec3d1;
            enumfacing = Direction.UP;
        }

        vec3d1 = this.collideWithZPlane(this.minZ, vecA, vecB);

        if (vec3d1 != null && this.isClosest(vecA, vec3d, vec3d1)) {
            vec3d = vec3d1;
            enumfacing = Direction.NORTH;
        }

        vec3d1 = this.collideWithZPlane(this.maxZ, vecA, vecB);

        if (vec3d1 != null && this.isClosest(vecA, vec3d, vec3d1)) {
            vec3d = vec3d1;
            enumfacing = Direction.SOUTH;
        }

        return vec3d == null ? null : new BlockHitResult(vec3d, enumfacing, BlockPos.ZERO, false);
    }

    boolean isClosest(Vec3 p_186661_1_, @Nullable Vec3 p_186661_2_, Vec3 p_186661_3_) {
        return p_186661_2_ == null || p_186661_1_.distanceToSqr(p_186661_3_) < p_186661_1_.distanceToSqr(p_186661_2_);
    }

    @Nullable
    Vec3 collideWithXPlane(double p_186671_1_, Vec3 p_186671_3_, Vec3 p_186671_4_) {
        Vec3 vec3d = getIntermediateWithXValue(p_186671_3_, p_186671_4_, p_186671_1_);
        return vec3d != null && this.intersectsWithYZ(vec3d) ? vec3d : null;
    }

    @Nullable
    Vec3 collideWithYPlane(double p_186663_1_, Vec3 p_186663_3_, Vec3 p_186663_4_) {
        Vec3 vec3d = getIntermediateWithYValue(p_186663_3_, p_186663_4_, p_186663_1_);
        return vec3d != null && this.intersectsWithXZ(vec3d) ? vec3d : null;
    }

    @Nullable
    Vec3 collideWithZPlane(double p_186665_1_, Vec3 p_186665_3_, Vec3 p_186665_4_) {
        Vec3 vec3d = getIntermediateWithZValue(p_186665_3_, p_186665_4_, p_186665_1_);
        return vec3d != null && this.intersectsWithXY(vec3d) ? vec3d : null;
    }

    /**
     * Re-implementation of Vec3d.getIntermediateWithXValue from MC 1.12 (removed in 1.13+).
     * Returns the intermediate point along the segment [from -> to] where x == targetX, or null
     * if such a point does not lie on the segment.
     */
    @Nullable
    private static Vec3 getIntermediateWithXValue(Vec3 from, Vec3 to, double targetX) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        if (dx * dx < 1.0E-7D) {
            return null;
        }
        double t = (targetX - from.x) / dx;
        return t >= 0.0D && t <= 1.0D ? new Vec3(from.x + dx * t, from.y + dy * t, from.z + dz * t) : null;
    }

    @Nullable
    private static Vec3 getIntermediateWithYValue(Vec3 from, Vec3 to, double targetY) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        if (dy * dy < 1.0E-7D) {
            return null;
        }
        double t = (targetY - from.y) / dy;
        return t >= 0.0D && t <= 1.0D ? new Vec3(from.x + dx * t, from.y + dy * t, from.z + dz * t) : null;
    }

    @Nullable
    private static Vec3 getIntermediateWithZValue(Vec3 from, Vec3 to, double targetZ) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        if (dz * dz < 1.0E-7D) {
            return null;
        }
        double t = (targetZ - from.z) / dz;
        return t >= 0.0D && t <= 1.0D ? new Vec3(from.x + dx * t, from.y + dy * t, from.z + dz * t) : null;
    }

    public boolean intersectsWithYZ(Vec3 vec) {
        return vec.y >= this.minY && vec.y <= this.maxY && vec.z >= this.minZ && vec.z <= this.maxZ;
    }

    public boolean intersectsWithXZ(Vec3 vec) {
        return vec.x >= this.minX && vec.x <= this.maxX && vec.z >= this.minZ && vec.z <= this.maxZ;
    }

    public boolean intersectsWithXY(Vec3 vec) {
        return vec.x >= this.minX && vec.x <= this.maxX && vec.y >= this.minY && vec.y <= this.maxY;
    }

    public String toString() {
        return "box[" + this.minX + ", " + this.minY + ", " + this.minZ + ". Size " + getSize() + "]";
    }

    public MutableBoundingBox offset(Vector3f position) {
        minX += position.x;
        maxX += position.x;
        minY += position.y;
        maxY += position.y;
        minZ += position.z;
        maxZ += position.z;
        return this;
    }

    /**
     * @return Creates a new AABB with dimensions of this box
     */
    public AABB toBB() {
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Modifies the box to contain the old box and the box passed in argument
     */
    public void growTo(MutableBoundingBox o) {
        minX = Math.min(minX, o.minX);
        minY = Math.min(minY, o.minY);
        minZ = Math.min(minZ, o.minZ);
        maxX = Math.max(maxX, o.maxX);
        maxY = Math.max(maxY, o.maxY);
        maxZ = Math.max(maxZ, o.maxZ);
    }

    public MutableBoundingBox offset(double x, double y, double z) {
        minX += x;
        maxX += x;
        minY += y;
        maxY += y;
        minZ += z;
        maxZ += z;
        return this;
    }

    public Vector3f getPosition() {
        return Vector3fPool.get((float) (minX + maxX) / 2, (float) (minY + maxY) / 2, (float) (minZ + maxZ) / 2);
    }

    public Vector3f getSize() {
        return Vector3fPool.get((float) (maxX - minX) / 2, (float) (maxY - minY) / 2, (float) (maxZ - minZ) / 2);
    }
}
