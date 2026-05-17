package fr.dynamx.common.handlers;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.IRotatedCollisionHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.PartShape;
import fr.dynamx.common.entities.IDynamXObject;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.PropsEntity;
import fr.dynamx.common.physics.player.WalkingOnPlayerController;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.SubClassPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        if (entity instanceof fr.dynamx.common.entities.PhysicsEntity) {
            return false;
        }
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
        // TODO port:1.20.1 - DynamX TileEntity (block) collisions still depend on the DynamXChunkData
        //  capability which is stubbed in this port. Once Phase 8 lands, plug the getCollidableTileEntities
        //  branch here so physics-blocks push players as well. For now we only handle PhysicsEntity-vs-Entity
        //  collisions, which is what vehicles need.
        double icollidableCheckRadius = DynamXConfig.blockCollisionRadius;
        AABB copy = entity.getBoundingBox();

        // 1.20.1 replacement for World.getCollisionBoxes(entity, aabb): expand the query AABB to
        // cover the swept motion and collect the returned VoxelShapes. Shapes.collide does the
        // axis sweep for us (Iterable<VoxelShape> -> clamped delta).
        int multiplier = entity instanceof Player ? 2 : 1;
        AABB swept = copy.expandTowards(mx * multiplier, my * multiplier, mz * multiplier);
        List<VoxelShape> blockShapes = new ArrayList<>();
        for (VoxelShape shape : entity.level().getBlockCollisions(entity, swept)) {
            if (!shape.isEmpty()) {
                blockShapes.add(shape);
            }
        }

        double nx = mx, ny = my, nz = mz;
        if (ny != 0.0D) {
            ny = Shapes.collide(Direction.Axis.Y, copy, blockShapes, ny);
            if (ny != 0) copy = copy.move(0.0D, ny, 0.0D);
        }
        if (nx != 0.0D) {
            nx = Shapes.collide(Direction.Axis.X, copy, blockShapes, nx);
            if (nx != 0.0D) copy = copy.move(nx, 0.0D, 0.0D);
        }
        if (nz != 0.0D) {
            nz = Shapes.collide(Direction.Axis.Z, copy, blockShapes, nz);
        }

        motionChanged = false;

        if (shouldHandleCollision(entity)) {
            Vector3fPool.openPool(SubClassPool.ROTATED_COLLS_HANDLER_0);

            List<PhysicsEntity> entities = entity.level().getEntitiesOfClass(PhysicsEntity.class,
                    entity.getBoundingBox().inflate(icollidableCheckRadius));
            for (PhysicsEntity e : entities) {
                if (DynamXContext.getPlayerPickingObjects().containsValue(e.getId())) {
                    continue;
                }
                Vector3fPool.openPool(SubClassPool.ROTATED_COLLS_HANDLER_2);
                QuaternionPool.openPool(SubClassPool.ROTATED_COLLS_HANDLER_2);
                float castx = (float) nx, casty = (float) ny, castz = (float) nz;
                Vector3f withPos = Vector3fPool.get((float) e.getX(), (float) e.getY(), (float) e.getZ());
                Vector3f n = collideWith(entity, e, withPos, castx, casty, castz);
                if (castx != n.x) { nx = n.x; motionChanged = true; }
                if (casty != n.y) { ny = n.y; motionChanged = true; }
                if (castz != n.z) { nz = n.z; motionChanged = true; }
                QuaternionPool.closePool();
                Vector3fPool.closePool();
            }

            Vector3fPool.closePool();
        }

        my = ny;
        mx = nx;
        mz = nz;

        // If a DynamX entity clamped the motion, re-apply the vanilla block clamp using the new
        // motion so we don't push the entity into a wall after the rotated push. We can't mutate
        // the bounding box here (vanilla move() owns that via setPos), so the re-clamp is a
        // best-effort axis sweep against the un-advanced box.
        if (motionChanged) {
            if (my != 0.0D) my = Shapes.collide(Direction.Axis.Y, entity.getBoundingBox(), blockShapes, my);
            if (mx != 0.0D) mx = Shapes.collide(Direction.Axis.X, entity.getBoundingBox(), blockShapes, mx);
            if (mz != 0.0D) mz = Shapes.collide(Direction.Axis.Z, entity.getBoundingBox(), blockShapes, mz);
        }
        return new double[]{mx, my, mz};
    }

    private static double min(double a, double b) {
        if (Math.abs(a) > Math.abs(b))
            return b;
        return a;
    }

    /**
     * Sweeps the entity AABB against a rotated DynamX object's collision shapes and returns the
     * clamped motion vector. The motion vector is rotated into the object's local frame, swept
     * against axis-aligned local boxes, then rotated back into world space.
     *
     * <p>TODO port:1.20.1 - The 1.12 implementation also tracked which faces collided so the
     * vehicle could push the player. The WalkingOnPlayerController path is wired but only triggers
     * when the local player walks on a Prop with a box shape (parity with the 1.12 behaviour).
     */
    private Vector3f collideWith(Entity entity, IDynamXObject with, Vector3f withPosition, float mx, float my, float mz) {
        float oldx = mx, oldy = my, oldz = mz;
        eps = 0.1;

        Vector3f data = Vector3fPool.get(mx, my, mz);
        Quaternion withRotation = with.getCollidableRotation();
        Quaternion inversedWithRotation = withRotation.inverse();
        if (inversedWithRotation == null) // can happen while the world is still loading
            return Vector3fPool.get(oldx, oldy, oldz);
        data = rotate(data, inversedWithRotation);
        mx = data.x; my = data.y; mz = data.z;
        float ox = mx, oy = my, oz = mz;

        List<Direction> collisionFaces = new ArrayList<>();
        List<MutableBoundingBox> shapes = with.getCollisionBoxes();
        AABB tempBB = rotateBB(withPosition,
                Vector3fPool.get((float) entity.getX(), (float) entity.getY(), (float) entity.getZ()),
                entity.getBoundingBox(), inversedWithRotation);
        Vector3f offset = with.getCollisionOffset();
        offset = DynamXGeometry.rotateVectorByQuaternion(offset, inversedWithRotation);
        tempBB = tempBB.move(-offset.x, -offset.y, -offset.z);

        // Y axis
        for (MutableBoundingBox box : shapes) {
            float ny = calculateYOffset(box, tempBB, my);
            if (ny < my) { collisionFaces.add(Direction.DOWN); my = ny; }
            else if (ny > my) { collisionFaces.add(Direction.UP); my = ny; }
        }
        if (my != 0) tempBB = tempBB.move(0, my, 0);
        // X axis
        for (MutableBoundingBox box : shapes) {
            float nx = calculateXOffset(box, tempBB, mx);
            if (nx < mx) { collisionFaces.add(Direction.WEST); mx = nx; }
            else if (nx > mx) { collisionFaces.add(Direction.EAST); mx = nx; }
        }
        if (mx != 0) tempBB = tempBB.move(mx, 0, 0);
        // Z axis
        for (MutableBoundingBox box : shapes) {
            float nz = calculateZOffset(box, tempBB, mz);
            if (nz < mz) { collisionFaces.add(Direction.NORTH); mz = nz; }
            else if (nz > mz) { collisionFaces.add(Direction.SOUTH); mz = nz; }
        }

        data = Vector3fPool.get(mx, my, mz);

        if (mx != ox || my != oy || mz != oz) {
            data = rotate(data, withRotation);
            if (Math.abs(data.x - oldx) < eps / 5) data.x = oldx;
            if (Math.abs(data.y - oldy) < eps / 5) data.y = oldy;
            if (Math.abs(data.z - oldz) < eps / 5) data.z = oldz;

            // WalkingOnPlayerController: the local player can stand on top of a Prop with a box
            // collision shape. We don't trigger this for vehicles (they handle players via seats).
            if (with instanceof PhysicsEntity && entity.level().isClientSide && entity instanceof Player
                    && (!(with instanceof PropsEntity)
                        || ((PropsEntity<?>) with).getPackInfo().getCollisionsHelper().getShapes().isEmpty()
                        || ((PropsEntity<?>) with).getPackInfo().getCollisionsHelper().getShapes().get(0).getShapeType() == PartShape.EnumPartType.BOX)
                    && !collisionFaces.isEmpty()
                    && WalkingOnPlayerController.controller == null
                    && isLocalPlayer((Player) entity)
                    && !DynamXContext.getPlayerPickingObjects().containsKey(entity.getId())) {
                PhysicsEntity<?> collidingWith = (PhysicsEntity<?>) with;
                for (Direction f : collisionFaces) {
                    if (collisionFaces.contains(f.getOpposite())) continue; // stuck between two boxes
                    Vector3f vehMotion = rotate(Vector3fPool.get(
                            (float) collidingWith.getDeltaMovement().x,
                            (float) collidingWith.getDeltaMovement().y,
                            (float) collidingWith.getDeltaMovement().z), inversedWithRotation);
                    float proj = Vector3fPool.get(vehMotion.x, vehMotion.y, vehMotion.z).dot(
                            Vector3fPool.get(f.getNormal().getX(), f.getNormal().getY(), f.getNormal().getZ()));
                    if (proj == 0) continue;
                    switch (f) {
                        case DOWN:
                            data.y += collidingWith.getDeltaMovement().y;
                            break;
                        case UP:
                            if (collidingWith.canPlayerStandOnTop()) {
                                Vector3f stand = Vector3fPool.get(
                                        (float) (entity.getX() - collidingWith.getX() + data.x),
                                        (float) (entity.getY() - collidingWith.getY() + data.y),
                                        (float) (entity.getZ() - collidingWith.getZ() + data.z));
                                stand = rotate(stand, inversedWithRotation);
                                stand = Vector3fPool.getPermanentVector(stand);
                                WalkingOnPlayerController.controller = new WalkingOnPlayerController((Player) entity, collidingWith, f, stand);
                                collidingWith.walkingOnPlayers.put((Player) entity, WalkingOnPlayerController.controller);
                                DynamXContext.getWalkingPlayers().put((Player) entity, collidingWith);
                                collidingWith.getSynchronizer().onWalkingPlayerChange(entity.getId(), stand, (byte) f.get3DDataValue());
                            } else {
                                data.y += collidingWith.getDeltaMovement().y;
                            }
                            break;
                        default:
                            // horizontal faces: no automatic player push (1.12 left these as TODO too).
                            break;
                    }
                }
            }
        } else {
            data = Vector3fPool.get(oldx, oldy, oldz);
        }
        return data;
    }

    /**
     * Equivalent of {@code EntityPlayer.isUser()} (1.12). On dedicated server this always returns
     * false; on client it returns true iff {@code player} is the local player. The dist check
     * keeps {@code Minecraft}/{@code LocalPlayer} off the server classloader.
     */
    private static boolean isLocalPlayer(Player player) {
        if (player.level().isClientSide
                && net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            return net.minecraft.client.Minecraft.getInstance().player == player;
        }
        return false;
    }
}
