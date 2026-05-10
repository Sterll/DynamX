package fr.dynamx.common.slopes;

import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.common.physics.terrain.element.CustomSlopeTerrainElement;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates slope terrain elements from either control points or a bounding region.
 *
 * <p>TODO port:1.20.1 - {@code BlockState#getCollisionBoundingBox} and
 * {@code BlockPos.MutableBlockPos.retain()} are gone in 1.20.1. The ray-tracing helper that depended
 * on them has been stubbed; full collision-shape based reimplementation needs Phase 5/6 terrain APIs.
 */
public class SlopeGenerator {
    public static Map<VerticalChunkPos, List<ITerrainElement.IPersistentTerrainElement>> generateSlopesFromControlPoints(List<Vector3f> controlPoints) {
        Map<VerticalChunkPos, List<ITerrainElement.IPersistentTerrainElement>> list = new HashMap<>();
        int number = controlPoints.size() / 2;

        for (int j = 0; j < number - 1; j++) {
            Vector3f p = Vector3fPool.get();

            Vector3f[] realPositions = new Vector3f[4];
            int i = j;
            realPositions[0] = new Vector3f(controlPoints.get(i).x + p.x,
                    controlPoints.get(i).y + p.y,
                    controlPoints.get(i).z + p.z);
            i = j + 1;
            realPositions[1] = new Vector3f(controlPoints.get(i).x + p.x,
                    controlPoints.get(i).y + p.y,
                    controlPoints.get(i).z + p.z);
            i = controlPoints.size() - 2 - j;
            realPositions[2] = new Vector3f(controlPoints.get(i).x + p.x,
                    controlPoints.get(i).y + p.y,
                    controlPoints.get(i).z + p.z);
            i = controlPoints.size() - 1 - j;
            realPositions[3] = new Vector3f(controlPoints.get(i).x + p.x,
                    controlPoints.get(i).y + p.y,
                    controlPoints.get(i).z + p.z);
            bakeSlope(list, realPositions);
        }
        return list;
    }

    public static Map<VerticalChunkPos, List<ITerrainElement.IPersistentTerrainElement>> generateSlopesInBox(Level world, SlopeBuildingConfig config, BlockPos p1, BlockPos p2) {
        Map<VerticalChunkPos, List<ITerrainElement.IPersistentTerrainElement>> list = new HashMap<>();
        BlockPos start = p1.getY() <= p2.getY() ? p1 : p2;
        p2 = start == p1 ? p2 : p1;

        PosRotator rotator = new PosRotator(config.getFacing());
        start = new BlockPos(rotator.counterFixBorderX(start.getX()), start.getY(), rotator.counterFixBorderZ(start.getZ()));
        p2 = new BlockPos(rotator.counterFixBorderX(p2.getX()), p2.getY(), rotator.counterFixBorderZ(p2.getZ()));

        BlockPos fixedStart = new BlockPos(rotator.getLittleX(start.getX(), p2.getX()), start.getY(), rotator.getLittleZ(start.getZ(), p2.getZ()));
        BlockPos fixedEnd = new BlockPos(rotator.getBigX(start.getX(), p2.getX()), p2.getY(), rotator.getBigZ(start.getZ(), p2.getZ()));

        int len = Math.abs(rotator.getTheOtherDir(fixedEnd) - rotator.getTheOtherDir(fixedStart));

        List<List<Vector3f[]>> points = new ArrayList<>();
        if (config.getDiagDir() > 0) {
            for (int i = 0; i <= len; i++) {
                int dirpos = rotator.getTheOtherDir(fixedStart) + i;

                List<Vector3f[]>[] slopes = rayTrace(world, config, rotator, rotator.setTheOtherDir(fixedStart, dirpos), rotator.setTheOtherDir(fixedEnd, dirpos + 1));
                points.add(slopes[0]);
                points.add(slopes[1]);
            }
        } else {
            for (int i = len; i >= 0; i--) {
                int dirpos = rotator.getTheOtherDir(fixedStart) + i - 1;

                List<Vector3f[]>[] slopes = rayTrace(world, config, rotator, rotator.setTheOtherDir(fixedStart, dirpos), rotator.setTheOtherDir(fixedEnd, dirpos + 1));
                points.add(slopes[1]);
                points.add(slopes[0]);
            }
        }
        if (!points.isEmpty()) {
            List<Vector3f[]> prev = new ArrayList<>(points.get(0));
            List<Vector3f[]> compare = new ArrayList<>(points.get(0));
            int[] curSize = new int[256];
            for (int i = 1; i < points.size(); i++) {
                List<Vector3f[]> my = points.get(i);
                if (compare.size() == my.size()) {
                    for (int j = 0; j < my.size(); j++) {
                        Vector3f[] arr = my.get(j);
                        Vector3f[] pArr = compare.get(j);

                        if (i != (points.size() - 1) && arr[0].y == pArr[0].y && rotator.getTheDir(arr[0]) == rotator.getTheDir(pArr[0]) &&
                                arr[1].y == pArr[1].y && rotator.getTheDir(arr[1]) == rotator.getTheDir(pArr[1]) && curSize[j] < 32) {
                            curSize[j]++;
                        } else {
                            curSize[j] = 0;

                            Vector3f[] poses = new Vector3f[4];
                            poses[0] = prev.get(j)[0];
                            poses[1] = prev.get(j)[1];
                            poses[2] = points.get(i - 1).get(j)[1];
                            poses[3] = points.get(i - 1).get(j)[0];
                            if (rotator.getTheOtherDir(poses[0]) != rotator.getTheOtherDir(poses[2]) || rotator.getTheOtherDir(poses[1]) != rotator.getTheOtherDir(poses[3])) {
                                bakeSlope(list, poses);

                                prev.set(j, points.get(i - 1).get(j));
                                compare.set(j, points.get(i).get(j));
                            }
                        }
                    }
                } else {
                    for (int j = 0; j < prev.size(); j++) {
                        Vector3f[] poses = new Vector3f[4];
                        poses[0] = prev.get(j)[0];
                        poses[1] = prev.get(j)[1];
                        poses[2] = points.get(i - 1).get(j)[1];
                        poses[3] = points.get(i - 1).get(j)[0];
                        if (rotator.getTheOtherDir(poses[0]) != rotator.getTheOtherDir(poses[2]) || rotator.getTheOtherDir(poses[1]) != rotator.getTheOtherDir(poses[3])) {
                            bakeSlope(list, poses);
                        }
                    }
                    prev = points.get(i);
                    compare = points.get(i);
                }
            }
        }
        return list;
    }

    private static void bakeSlope(Map<VerticalChunkPos, List<ITerrainElement.IPersistentTerrainElement>> list, Vector3f[] poses) {
        Vector3f p = DynamXGeometry.getCenter(poses[0], poses[1], poses[2], poses[3]);
        VerticalChunkPos cp = new VerticalChunkPos((int) (p.x) >> 4, (int) (p.y) >> 4, (int) (p.z) >> 4);
        p = Vector3fPool.get(cp.x * 16 + 8, cp.y * 16, cp.z * 16 + 8).multLocal(-1);
        for (int k = 0, posesLength = poses.length; k < posesLength; k++) {
            poses[k] = poses[k].add(p);
        }
        CustomSlopeTerrainElement element = new CustomSlopeTerrainElement(poses);
        if (!list.containsKey(cp))
            list.put(cp, new ArrayList<>());
        list.get(cp).add(element);
    }

    @SuppressWarnings("unchecked")
    private static List<Vector3f[]>[] rayTrace(Level world, SlopeBuildingConfig config, PosRotator rotator, BlockPos fixedStart, BlockPos fixedEnd) {
        float y = fixedStart.getY();
        // TODO port:1.20.1 - BlockState.getCollisionBoundingBox(World, BlockPos) -> getCollisionShape(level, pos).bounds()
        AABB box = aabbOf(world, fixedStart);
        float curRatio = 0;
        int curLen = 0;
        float curHeight = 0;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(fixedStart.getX(), fixedStart.getY(), fixedStart.getZ());
        int len = Math.abs(rotator.getTheDir(fixedEnd) - rotator.getTheDir(fixedStart));
        Vector3f curStart = Vector3fPool.get(fixedStart.getX(), y, fixedStart.getZ());
        Vector3f curEnd = null, lastPlacedAtPos = null;

        ArrayList<Vector3f[]> list = new ArrayList<>();
        ArrayList<Vector3f[]> list1 = new ArrayList<>();

        BlockState support = world.getBlockState(fixedStart.below());
        box = aabbOf(world, fixedStart.below());
        boolean hasStartPoint = box != null && (box.maxX - box.minX == 1 && box.maxZ - box.minZ == 1) && box.maxY > 0 && config.isValidBlock(support);
        if (hasStartPoint) {
            BlockPos authority = fixedStart;
            support = world.getBlockState(authority);
            box = aabbOf(world, authority);
            hasStartPoint = !(box != null && (box.maxX - box.minX == 1 && box.maxZ - box.minZ == 1) && box.maxY > 0);
        }

        boolean placing = false;
        for (int i = 0; i < len; i++) {
            pos.set(fixedStart.getX(), (int) y, fixedStart.getZ());
            pos = rotator.mute(i, pos);

            box = aabbOf(world, pos);
            if (box != null && (box.maxX - box.minX == 1 && box.maxZ - box.minZ == 1) && box.maxY > 0 && box.maxY + Mth.floor(y) > y && config.isValidBlock(world.getBlockState(pos))) {
                y = (float) (Mth.floor(y) + box.maxY);
                if (y > fixedEnd.getY())
                    break;
                curLen = Math.abs(rotator.getTheDir(pos) - rotator.getTheDir(curStart));
                curHeight = (float) Math.abs(pos.getY() + box.maxY - curStart.y);
                float ratio = (curHeight) / (curLen);
                if (ratio >= curRatio && false) {
                    curRatio = curHeight / (curLen);
                    curEnd = rotator.mute(i, fixedStart.getX(), y, fixedStart.getZ());
                    placing = true;
                } else {
                    if (curEnd == null) {
                        curEnd = rotator.mute(i, fixedStart.getX(), y, fixedStart.getZ());
                        placing = false;
                    } else
                        placing = true;

                    if (hasStartPoint) {
                        Vector3f[] poses = new Vector3f[2];
                        Vector3f[] poses1 = new Vector3f[2];
                        int fxs = rotator.fixBorderX((int) curStart.x);
                        int fzs = rotator.fixBorderZ((int) curStart.z);
                        switch (config.getFacing().getAxis()) {
                            case X:
                                int fx = rotator.fixBorderX((int) curEnd.x);
                                int fz = rotator.fixBorderZ(fixedEnd.getZ());
                                poses[0] = new Vector3f(fxs, curStart.y, fzs);
                                poses1[0] = new Vector3f(fxs, curStart.y, fz);
                                poses[1] = new Vector3f(fx, curEnd.y, fzs);
                                poses1[1] = new Vector3f(fx, curEnd.y, fz);
                                break;
                            case Z:
                                fx = rotator.fixBorderX(fixedEnd.getX());
                                fz = rotator.fixBorderZ((int) curEnd.z);
                                poses[0] = new Vector3f(fxs, curStart.y, fzs);
                                poses1[0] = new Vector3f(fx, curStart.y, fzs);
                                poses[1] = new Vector3f(fxs, curEnd.y, fz);
                                poses1[1] = new Vector3f(fx, curEnd.y, fz);
                                break;
                            default:
                                break;
                        }
                        list.add(poses);
                        list1.add(poses1);
                    }

                    curStart = curEnd;
                    lastPlacedAtPos = Vector3fPool.get(pos.getX(), (float) (pos.getY() + box.maxY), pos.getZ());
                    curEnd = null;
                    curRatio = ratio;

                    BlockPos authority = pos.offset(0, 1, 0);
                    support = world.getBlockState(authority);
                    box = aabbOf(world, authority);
                    hasStartPoint = !(box != null && (box.maxX - box.minX == 1 && box.maxZ - box.minZ == 1) && box.maxY > 0);
                }
            } else {
                if (!hasStartPoint) {
                    support = world.getBlockState(pos.below());
                    box = aabbOf(world, pos.below());
                    hasStartPoint = box != null && (box.maxX - box.minX == 1 && box.maxZ - box.minZ == 1) && box.maxY > 0 && config.isValidBlock(support);
                    if (hasStartPoint) {
                        BlockPos authority = pos;
                        support = world.getBlockState(authority);
                        box = aabbOf(world, authority);
                        hasStartPoint = !(box != null && (box.maxX - box.minX == 1 && box.maxZ - box.minZ == 1) && box.maxY > 0);
                        if (hasStartPoint)
                            curStart = rotator.mute(i, fixedStart.getX(), y, fixedStart.getZ());
                    }
                }
                curLen++;
            }
        }
        if (placing && hasStartPoint) {
            if (curEnd == null) {
                if (lastPlacedAtPos == null)
                    curEnd = Vector3fPool.get(fixedEnd.getX(), fixedEnd.getY(), fixedEnd.getZ());
                else
                    curEnd = lastPlacedAtPos;
            }
            Vector3f[] poses = new Vector3f[2];
            Vector3f[] poses1 = new Vector3f[2];
            int fxs = rotator.fixBorderX((int) curStart.x);
            int fzs = rotator.fixBorderZ((int) curStart.z);
            switch (config.getFacing().getAxis()) {
                case X:
                    int fx = rotator.fixBorderX((int) curEnd.x);
                    int fz = rotator.fixBorderZ(fixedEnd.getZ());
                    poses[0] = new Vector3f(fxs, curStart.y, fzs);
                    poses1[0] = new Vector3f(fxs, curStart.y, fz);
                    poses[1] = new Vector3f(fx, curEnd.y, fzs);
                    poses1[1] = new Vector3f(fx, curEnd.y, fz);
                    break;
                case Z:
                    fx = rotator.fixBorderX(fixedEnd.getX());
                    fz = rotator.fixBorderZ((int) curEnd.z);
                    poses[0] = new Vector3f(fxs, curStart.y, fzs);
                    poses1[0] = new Vector3f(fx, curStart.y, fzs);
                    poses[1] = new Vector3f(fxs, curEnd.y, fz);
                    poses1[1] = new Vector3f(fx, curEnd.y, fz);
                    break;
                default:
                    break;
            }
            list.add(poses);
            list1.add(poses1);
        }
        return new ArrayList[]{list, list1};
    }

    /**
     * Helper: read the collision shape bounds for the block at {@code pos}. Returns null if empty
     * (mirroring the legacy {@code Block.NULL_AABB} contract).
     */
    private static AABB aabbOf(Level world, BlockPos pos) {
        try {
            var shape = world.getBlockState(pos).getCollisionShape(world, pos);
            if (shape.isEmpty())
                return null;
            return shape.bounds();
        } catch (Exception e) {
            return null;
        }
    }
}
