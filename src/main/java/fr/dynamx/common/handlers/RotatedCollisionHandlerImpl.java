package fr.dynamx.common.handlers;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.IRotatedCollisionHandler;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Provides helper methods for rotated collisions and handles collisions between vanilla entities
 * and DynamX physics entities / DynamX-blocks.
 *
 * <p>TODO port:1.20.1 - The body of {@code handleCollisionWithBulletEntities} (and the inner
 * {@code collideWith}) is heavily coupled to:
 *   - {@code DynamXContext} / {@code ClientDebugSystem} (Phase 4b / 7)
 *   - {@code PhysicsEntity}, {@code PropsEntity}, {@code PartShape} (Phase 3b / 6)
 *   - {@code WalkingOnPlayerController}, picking objects, walking-player sync (Phase 5/6)
 *   - {@code AABB#calculateXOffset/calculateYOffset/calculateZOffset} — these helpers were removed
 *     in 1.13; replacements (Shapes.collide) have a very different shape API.
 *
 * <p>The unchanged geometric helpers ({@code rotate}, {@code rotateBB}, the static
 * {@code calculate?Offset}) are kept as-is so other modules can reuse them. The full collision
 * pipeline is stubbed and returns the unchanged motion vector.
 */
public class RotatedCollisionHandlerImpl implements IRotatedCollisionHandler {
    @Override
    public Vector3f rotate(Vector3f pos, Quaternion rotation) {
        if (rotation == null) {
            return Vector3fPool.get(pos);
        }
        return DynamXGeometry.rotateVectorByQuaternion(pos, rotation);
    }

    @Override
    public AABB rotateBB(Vector3f offset, Vector3f pos, AABB from, Quaternion rotation) {
        Vector3f tp = rotate(Vector3fPool.get(pos.x - offset.x, pos.y - offset.y, pos.z - offset.z), rotation);
        MutableBoundingBox bb = rotateBB(pos, new MutableBoundingBox(from), rotation);
        bb.offset(Vector3fPool.get(offset.x + tp.x - pos.x, offset.y + tp.y - pos.y, offset.z + tp.z - pos.z));
        return bb.toBB();
    }

    @Override
    public MutableBoundingBox rotateBB(Vector3f pos, MutableBoundingBox from, Quaternion rotation) {
        from.offset(pos.multLocal(-1));
        pos.multLocal(-1);
        Vector3f v1 = rotate(Vector3fPool.get((float) from.minX, 0, 0), rotation);
        Vector3f v2 = rotate(Vector3fPool.get(0, (float) from.minY, 0), rotation);
        Vector3f v3 = rotate(Vector3fPool.get(0, 0, (float) from.minZ), rotation);
        Vector3f v4 = rotate(Vector3fPool.get((float) from.maxX, 0, 0), rotation);
        Vector3f v5 = rotate(Vector3fPool.get(0, (float) from.maxY, 0), rotation);
        Vector3f v6 = rotate(Vector3fPool.get(0, 0, (float) from.maxZ), rotation);
        MutableBoundingBox n = new MutableBoundingBox(
                DynamXMath.getMin(v1.x, v2.x, v3.x, v4.x, v5.x, v6.x),
                DynamXMath.getMin(v1.y, v2.y, v3.y, v4.y, v5.y, v6.y),
                DynamXMath.getMin(v1.z, v2.z, v3.z, v4.z, v5.z, v6.z),
                DynamXMath.getMax(v1.x, v2.x, v3.x, v4.x, v5.x, v6.x),
                DynamXMath.getMax(v1.y, v2.y, v3.y, v4.y, v5.y, v6.y),
                DynamXMath.getMax(v1.z, v2.z, v3.z, v4.z, v5.z, v6.z));
        n.offset(pos);
        return n;
    }

    public static double eps = 1e-1;

    public static float calculateXOffset(MutableBoundingBox against, AABB other, float offsetX) {
        if (other.maxY > against.minY && other.minY < against.maxY && other.maxZ > against.minZ && other.minZ < against.maxZ) {
            if (offsetX > 0.0D && other.maxX - eps <= against.minX) {
                float d1 = (float) (against.minX - other.maxX);
                if (d1 < offsetX) offsetX = d1;
            } else if (offsetX < 0.0D && other.minX + eps >= against.maxX) {
                float d0 = (float) (against.maxX - other.minX);
                if (d0 > offsetX) offsetX = d0;
            }
            return offsetX;
        }
        return offsetX;
    }

    public static float calculateYOffset(MutableBoundingBox against, AABB other, float offsetY) {
        if (other.maxX > against.minX && other.minX < against.maxX && other.maxZ > against.minZ && other.minZ < against.maxZ) {
            if (offsetY > 0.0D && other.maxY - eps <= against.minY) {
                float d1 = (float) (against.minY - other.maxY);
                if (d1 < offsetY) offsetY = d1;
            } else if (offsetY < 0.0D && other.minY + eps >= against.maxY) {
                float d0 = (float) (against.maxY - other.minY);
                if (d0 > offsetY) offsetY = d0;
            }
            return offsetY;
        }
        return offsetY;
    }

    public static float calculateZOffset(MutableBoundingBox against, AABB other, float offsetZ) {
        if (other.maxX > against.minX && other.minX < against.maxX && other.maxY > against.minY && other.minY < against.maxY) {
            if (offsetZ > 0.0D && other.maxZ - eps <= against.minZ) {
                float d1 = (float) (against.minZ - other.maxZ);
                if (d1 < offsetZ) offsetZ = d1;
            } else if (offsetZ < 0.0D && other.minZ + eps >= against.maxZ) {
                float d0 = (float) (against.maxZ - other.minZ);
                if (d0 > offsetZ) offsetZ = d0;
            }
            return offsetZ;
        }
        return offsetZ;
    }

    private boolean motionChanged;

    @Override
    public boolean motionHasChanged() {
        return motionChanged;
    }

    private volatile Set<Pattern> compiledIgnorePatterns;

    @SuppressWarnings("unused")
    private boolean shouldHandleCollision(Entity entity) {
        // TODO port:1.20.1 - PhysicsEntity check restored once entities are fully wired.
        if (compiledIgnorePatterns == null) {
            compiledIgnorePatterns = new HashSet<>();
            for (String pattern : DynamXConfig.ignoreCollisionEntities) {
                String regex = pattern.replace(".", "\\.").replace("*", ".*");
                compiledIgnorePatterns.add(Pattern.compile("^" + regex + "$"));
            }
        }
        String className = entity.getClass().getName();
        for (Pattern pattern : compiledIgnorePatterns) {
            if (pattern.matcher(className).matches()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public double[] handleCollisionWithBulletEntities(Entity entity, MoverType moverType, double mx, double my, double mz) {
        // TODO port:1.20.1 - Full collision pipeline depends on PhysicsEntity, DynamXChunkData attachment
        // accessors, Level.getEntitiesWithinAABB replacement, PartShape, WalkingOnPlayerController and
        // the network resync packets. All of those land in later phases. Returning unchanged motion
        // preserves vanilla collision behavior in the meantime.
        motionChanged = false;
        return new double[]{mx, my, mz};
    }
}
