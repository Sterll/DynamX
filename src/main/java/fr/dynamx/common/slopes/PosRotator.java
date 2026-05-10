package fr.dynamx.common.slopes;

import com.jme3.math.Vector3f;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class PosRotator {
    private final Direction direction;

    public PosRotator(Direction direction) {
        this.direction = direction;
    }

    public BlockPos.MutableBlockPos mute(int i, BlockPos.MutableBlockPos from) {
        switch (direction) {
            case NORTH:
                from.set(from.getX(), from.getY(), from.getZ() - i);
                break;
            case SOUTH:
                from.set(from.getX(), from.getY(), from.getZ() + i);
                break;
            case EAST:
                from.set(from.getX() + i, from.getY(), from.getZ());
                break;
            case WEST:
                from.set(from.getX() - i, from.getY(), from.getZ());
                break;
        }
        return from;
    }

    public BlockPos mute(int i, int x, int y, int z) {
        switch (direction) {
            case NORTH:
                return new BlockPos(x, y, z - i);
            case SOUTH:
                return new BlockPos(x, y, z + i);
            case EAST:
                return new BlockPos(x + i, y, z);
            case WEST:
                return new BlockPos(x - i, y, z);
        }
        return new BlockPos(x, y, z);
    }

    public Vector3f mute(int i, int x, float y, int z) {
        switch (direction) {
            case NORTH:
                return Vector3fPool.get(x, y, z - i);
            case SOUTH:
                return Vector3fPool.get(x, y, z + i);
            case EAST:
                return Vector3fPool.get(x + i, y, z);
            case WEST:
                return Vector3fPool.get(x - i, y, z);
        }
        return Vector3fPool.get(x, y, z);
    }

    public int getLittleX(int xstart, int xend) {
        return direction.getAxis() == Direction.Axis.X ? (direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? Math.max(xstart, xend) : Math.min(xstart, xend)) : Math.min(xstart, xend);
    }

    public int getLittleZ(int zstart, int zend) {
        return direction.getAxis() == Direction.Axis.Z ? (direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? Math.max(zstart, zend) : Math.min(zstart, zend)) : Math.min(zstart, zend);
    }

    public int getBigX(int xstart, int xend) {
        return direction.getAxis() == Direction.Axis.X ? (direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? Math.min(xstart, xend) : Math.max(xstart, xend)) : Math.max(xstart, xend);
    }

    public int getBigZ(int zstart, int zend) {
        return direction.getAxis() == Direction.Axis.Z ? (direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? Math.min(zstart, zend) : Math.max(zstart, zend)) : Math.max(zstart, zend);
    }

    public int getTheDir(BlockPos from) {
        switch (direction) {
            case NORTH:
            case SOUTH:
                return from.getZ();
        }
        return from.getX();
    }

    public int getTheDir(Vector3f from) {
        switch (direction) {
            case NORTH:
            case SOUTH:
                return (int) from.z;
        }
        return (int) from.x;
    }

    public int getTheOtherDir(BlockPos from) {
        switch (direction) {
            case NORTH:
            case SOUTH:
                return from.getX();
        }
        return from.getZ();
    }

    public float getTheOtherDir(Vector3f from) {
        switch (direction) {
            case NORTH:
            case SOUTH:
                return from.x;
        }
        return from.z;
    }

    public BlockPos setTheOtherDir(BlockPos in, int value) {
        switch (direction) {
            case NORTH:
            case SOUTH:
                return new BlockPos(value, in.getY(), in.getZ());
        }
        return new BlockPos(in.getX(), in.getY(), value);
    }

    public int fixBorderX(int x) {
        return direction == Direction.WEST ? x + 1 : x;
    }

    public int fixBorderZ(int z) {
        return direction == Direction.NORTH ? z + 1 : z;
    }

    public int counterFixBorderX(int x) {
        return direction == Direction.WEST ? x - 1 : x;
    }

    public int counterFixBorderZ(int z) {
        return direction == Direction.NORTH ? z - 1 : z;
    }
}
