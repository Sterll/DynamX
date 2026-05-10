package fr.dynamx.common.physics.terrain.computing;

import fr.dynamx.api.physics.terrain.IBlockCollisionBehavior;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.physics.terrain.element.DynamXBlockTerrainElement;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * All {@link IBlockCollisionBehavior} add by DynamX <br>
 * The behaviors added in the {@link fr.dynamx.api.physics.terrain.ITerrainManager} have the priority over them
 */
public class BlockCollisionBehaviors {
    /**
     * Default fallback behavior
     */
    static class None implements IBlockCollisionBehavior {
        private final List<AABB> boxes = new ArrayList<>();

        @Override
        public boolean applies(BlockGetter world, BlockPos pos, BlockState toBlock) {
            return false;
        }

        @Override
        public boolean stacks(BlockGetter world, BlockPos pos, Direction.Axis axis, BlockState onBlock, BlockState stackingBlock, BlockState lastStacked) {
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, Level world, BlockPos at, BlockState ofBlock, Direction.Axis axis) {
            // TODO port:1.20.1 - Material removed; using BlockState.blocksMotion() approximation for blocksMovement
            if (ofBlock.blocksMotion()) //Si le block a une collision spéciale, on l'ajoute
            {
                // TODO port:1.20.1 - addCollisionBoxToList replaced by VoxelShape.toAabbs() offset by pos
                ofBlock.getCollisionShape(world, at).toAabbs().forEach(b -> boxes.add(b.move(at.getX(), at.getY(), at.getZ())));
                if (boxes.size() <= DynamXConfig.maxComplexBlockBoxes) {
                    terrainBoxConstructor.injectBlockCollisions(at, ofBlock, boxes);
                    boxes.clear();
                } else {
                    boxes.clear();
                    // TODO port:1.20.1 - getBoundingBox replaced by getShape().bounds()
                    AABB box = ofBlock.getShape(world, at).bounds();
                    terrainBoxConstructor.addMutable(new MutableBoundingBox(box).offset(at.getX(), at.getY(), at.getZ()));
                }
            }
        }

        @Override
        public boolean isStackableBlock(BlockGetter world, BlockPos pos, BlockState blockState) {
            return false;
        }
    }

    static class Leaves implements IBlockCollisionBehavior {
        @Override
        public boolean applies(BlockGetter world, BlockPos pos, BlockState toBlock) {
            return toBlock.getBlock() instanceof LeavesBlock;
        }

        @Override
        public boolean stacks(BlockGetter world, BlockPos pos, Direction.Axis axis, BlockState onBlock, BlockState stackingBlock, BlockState lastStacked) {
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, Level world, BlockPos at, BlockState ofBlock, Direction.Axis axis) {
        }

        @Override
        public boolean isStackableBlock(BlockGetter world, BlockPos pos, BlockState blockState) {
            return false;
        }
    }

    static class FullCube implements IBlockCollisionBehavior {
        @Override
        public boolean applies(BlockGetter world, BlockPos pos, BlockState toBlock) {
            // TODO port:1.20.1 - isFullCube replaced by isCollisionShapeFullBlock approximation
            return toBlock.isCollisionShapeFullBlock(world, pos) && !(toBlock.getBlock() instanceof LeavesBlock);
        }

        @Override
        public boolean stacks(BlockGetter world, BlockPos pos, Direction.Axis axis, BlockState onBlock, BlockState stackingBlock, BlockState lastStacked) {
            // TODO port:1.20.1 - isFullCube approximated with isCollisionShapeFullBlock
            if (axis == Direction.Axis.Y && lastStacked != null && !lastStacked.isCollisionShapeFullBlock(world, pos)) {
                return false;
            }
            return onBlock.isCollisionShapeFullBlock(world, pos) || (axis == Direction.Axis.Y && onBlock.getBlock() instanceof SlabBlock && onBlock.getValue(SlabBlock.TYPE) == SlabType.TOP);
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, Level world, BlockPos at, BlockState ofBlock, Direction.Axis axis) {
            if (axis == null) {
                terrainBoxConstructor.addBlockCollisions(world, at, ofBlock);
            } else {
                switch (axis) {
                    case X:
                        boxBuilder.expandX(1);
                        break;
                    case Y:
                        boxBuilder.expandY(1);
                        break;
                    case Z:
                        boxBuilder.expandZ(1);
                        break;
                }
            }
        }
    }

  /*  static class MaybeFullBlock implements IBlockCollisionBehavior {
        @Override
        public boolean applies(BlockGetter world, BlockPos pos, BlockState toBlock) {
            AABB bb = toBlock.getBoundingBox(world, pos);
            return bb.maxY - bb.minY == 1 && bb.maxX - bb.minX == 1 && bb.maxZ - bb.minZ == 1;
        }

        @Override
        public boolean stacks(BlockGetter world, BlockPos pos, Direction.Axis axis, BlockState onBlock, BlockState stackingBlock, BlockState lastStacked) {
            return applies(world, pos, onBlock) || (axis == Direction.Axis.Y && onBlock.getBlock() instanceof SlabBlock && onBlock.getValue(SlabBlock.TYPE) == SlabType.TOP);
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, Level world, BlockPos at, BlockState ofBlock, Direction.Axis axis) {
            if (axis == null) {
                ofBlock.addCollisionBoxToList(world, at, terrainBoxConstructor.getSearchZone(), terrainBoxConstructor.getOutListVanilla(), null, false);
            } else {
                switch (axis) {
                    case X:
                        boxBuilder.expandX(1);
                        break;
                    case Y:
                        boxBuilder.expandY(1);
                        break;
                    case Z:
                        boxBuilder.expandZ(1);
                        break;
                }
            }
        }
    }*/

    static class Slab implements IBlockCollisionBehavior {
        @Override
        public boolean applies(BlockGetter world, BlockPos pos, BlockState toBlock) {
            return toBlock.getBlock() instanceof SlabBlock;
        }

        @Override
        public boolean stacks(BlockGetter world, BlockPos pos, Direction.Axis axis, BlockState onBlock, BlockState stackingBlock, BlockState lastStacked) {
            if (axis == Direction.Axis.Y) {
                // TODO port:1.20.1 - isFullCube approximated with isCollisionShapeFullBlock
                if (lastStacked != null && lastStacked.isCollisionShapeFullBlock(world, pos))
                    return false; //cannot continue the plane
                return (onBlock.isCollisionShapeFullBlock(world, pos) || (onBlock.getBlock() instanceof SlabBlock && onBlock.getValue(SlabBlock.TYPE) == SlabType.TOP)) && stackingBlock.getValue(SlabBlock.TYPE) == SlabType.BOTTOM;
            }
            // TODO port:1.20.1 - SlabBlock.isDouble() removed; check via SlabType.DOUBLE
            return onBlock.getBlock() instanceof SlabBlock && onBlock.getValue(SlabBlock.TYPE) != SlabType.DOUBLE && onBlock.getValue(SlabBlock.TYPE) == stackingBlock.getValue(SlabBlock.TYPE);
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, Level world, BlockPos at, BlockState ofBlock, Direction.Axis axis) {
            if (axis == null) {
                terrainBoxConstructor.addBlockCollisions(world, at, ofBlock);
            } else {
                switch (axis) {
                    case X:
                        boxBuilder.expandX(1);
                        break;
                    case Y:
                        boxBuilder.expandY(0.5f); //Can't be up
                        break;
                    case Z:
                        boxBuilder.expandZ(1);
                        break;
                }
            }
        }
    }

    static class DynamXBlockBehavior implements IBlockCollisionBehavior {
        @Override
        public boolean applies(BlockGetter world, BlockPos pos, BlockState toBlock) {
            return toBlock.getBlock() instanceof DynamXBlock;
        }

        @Override
        public boolean stacks(BlockGetter world, BlockPos pos, Direction.Axis axis, BlockState onBlock, BlockState stackingBlock, BlockState lastStacked) {
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, Level world, BlockPos at, BlockState ofBlock, Direction.Axis axis) {
            // TODO port:1.20.1 - getTileEntity renamed to getBlockEntity
            BlockEntity te = world.getBlockEntity(at);
            if (te instanceof TEDynamXBlock)
                terrainBoxConstructor.addCustomShapedElement(new DynamXBlockTerrainElement(cursor.dx, cursor.dy, cursor.dz, at));
        }

        @Override
        public boolean isStackableBlock(BlockGetter world, BlockPos pos, BlockState blockState) {
            return false;
        }
    }

    static class Stairs implements IBlockCollisionBehavior {
        @Override
        public boolean applies(BlockGetter world, BlockPos pos, BlockState toBlock) {
            return toBlock.getBlock() instanceof StairBlock && toBlock.getValue(StairBlock.SHAPE) == StairsShape.STRAIGHT;
        }

        @Override
        public boolean stacks(BlockGetter world, BlockPos pos, Direction.Axis axis, BlockState onBlock, BlockState stackingBlock, BlockState lastStacked) {
            if (axis == Direction.Axis.Y) {
                return false;
            } else if (onBlock.getBlock() instanceof StairBlock && (stackingBlock.getValue(StairBlock.HALF) == onBlock.getValue(StairBlock.HALF))) {
                Direction facing = stackingBlock.getValue(StairBlock.FACING);
                // TODO port:1.20.1 - axis.negate().test(facing) replaced; check axis is perpendicular to facing
                if (facing.getAxis() != axis) {
                    return facing == onBlock.getValue(StairBlock.FACING);
                }
            }
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, Level world, BlockPos at, BlockState ofBlock, Direction.Axis axis) {
            if (axis == null) {
                terrainBoxConstructor.addBlockCollisions(world, at, ofBlock);
            } else {
                switch (axis) {
                    case X:
                        boxBuilder.expandX(1);
                        break;
                    case Y:
                        boxBuilder.expandY(0.5f); //Can't be up
                        break;
                    case Z:
                        boxBuilder.expandZ(1);
                        break;
                }
            }
        }

        @Override
        public TerrainBoxBuilder initBoxBuilder(TerrainBoxConstructor terrainBoxConstructor, Level world, BlockPos mutable, BlockState boxStart, double ox, double oy, double oz) {
            // TODO port:1.20.1 - getBoundingBox replaced by getShape().bounds()
            AABB box = boxStart.getShape(world, mutable).bounds();
            // TODO port:1.20.1 - getActualState removed; the BlockState already reflects connections in 1.20.1
            Direction facing = boxStart.getValue(StairBlock.FACING);
            if (boxStart.getValue(StairBlock.HALF) == Half.BOTTOM) {
                return new TerrainBoxBuilder.StairsTerrainBoxBuilder(ox + box.minX, oy + box.minY, oz + box.minZ, 0.5f, facing, false);
            }
            return new TerrainBoxBuilder.StairsTerrainBoxBuilder(ox + box.minX, oy + box.minY, oz + box.minZ, 0.5f, facing, true);
        }

        @Override
        public boolean isStackableBlock(BlockGetter world, BlockPos pos, BlockState blockState) {
            return true;
        }
    }

    public static class Panes implements IBlockCollisionBehavior {
        private final List<MutableBoundingBox> waitingXBoxes = new ArrayList<>();
        private final List<MutableBoundingBox> waitingZBoxes = new ArrayList<>();
        private boolean hasXZLegs;

        @Override
        public boolean applies(BlockGetter world, BlockPos pos, BlockState toBlock) {
            return toBlock.getBlock() instanceof IronBarsBlock;
        }

        @Override
        public boolean stacks(BlockGetter world, BlockPos pos, Direction.Axis axis, BlockState onBlock, BlockState stackingBlock, BlockState lastStacked) {
            if (!(onBlock.getBlock() instanceof IronBarsBlock))
                return false;
            return onBlock.equals(stackingBlock);
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, Level world, BlockPos at, BlockState ofBlock, Direction.Axis axis) {
            if (axis == null) {
                terrainBoxConstructor.addBlockCollisions(world, at, ofBlock);
                return;
            }
            if (axis == Direction.Axis.Y) {
                boxBuilder.expandY(1);
                return;
            }
            // TODO port:1.20.1 - getActualState removed; BlockState already has connection properties
            BlockState realStacking = ofBlock;
            // if (onBlock.getValue(BlockPane.SOUTH) || onBlock.getValue(BlockPane.NORTH))
            // return !onBlock.getValue(BlockPane.EAST) && !onBlock.getValue(BlockPane.WEST);
            switch (axis) {
                case X:
                    boxBuilder.expandX((realStacking.getValue(IronBarsBlock.EAST) ? 1 : 0.5625f));
                    if (realStacking.getValue(IronBarsBlock.SOUTH) && !hasXZLegs)
                        waitingZBoxes.add(new MutableBoundingBox(0.4375D, 0.0D, 0.4375D, 0.5625D, 1.0D, 1.0D).offset(at.getX(), at.getY(), at.getZ()));
                    break;
                case Z:
                    boxBuilder.expandZ((realStacking.getValue(IronBarsBlock.SOUTH) ? 1 : 0.5625f));
                    if (realStacking.getValue(IronBarsBlock.EAST) && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0.4375D, 0.0D, 0.4375D, 1.0D, 1.0D, 0.5625D).offset(at.getX(), at.getY(), at.getZ()));
                    if (realStacking.getValue(IronBarsBlock.WEST) && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0, 0.0D, 0.4375D, 0.5625f, 1.0D, 0.5625D).offset(at.getX(), at.getY(), at.getZ()));
                    break;
            }
        }

        @Override
        public TerrainBoxBuilder initBoxBuilder(TerrainBoxConstructor terrainBoxConstructor, Level world, BlockPos mutable, BlockState boxStart, double ox, double oy, double oz) {
            hasXZLegs = false;
            // TODO port:1.20.1 - getBoundingBox replaced by getShape().bounds()
            AABB box = boxStart.getShape(world, mutable).bounds();
            double maxZ = box.maxZ;
            if (maxZ == 1 && box.maxX == 1) { //if x-stacking, but z+ leg
                maxZ = 0.5625f;
                hasXZLegs = true;
                waitingZBoxes.add(new MutableBoundingBox(0.4375D, 0.0D, 0.4375D, 0.5625D, 1.0D, 1.0D).offset(mutable.getX(), mutable.getY(), mutable.getZ()));
            }

            return new TerrainBoxBuilder.MutableTerrainBoxBuilder(ox + box.minX, oy + box.minY, oz + box.minZ, box.maxX - box.minX, box.maxY - box.minY, maxZ - box.minZ) {
                @Override
                public void expandZ(float by) {
                    super.expandZ(by);
                    if (hasXZLegs) {
                        startZSize = box.maxZ - box.minZ;
                    }
                }
            };
        }

        @Override
        public void onBoxBuildEnd(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder) {
            if (boxBuilder.getXSize() == 0) {
                waitingXBoxes.forEach(terrainBoxConstructor::addMutable);
            }
            waitingXBoxes.clear();
            if (boxBuilder.getZSize() == 0) {
                waitingZBoxes.forEach(terrainBoxConstructor::addMutable);
            }
            waitingZBoxes.clear();
        }
    }

    static class PathBlock implements IBlockCollisionBehavior {
        @Override
        public boolean applies(BlockGetter world, BlockPos pos, BlockState toBlock) {
            //   System.out.println("Test "+toBlock+" "+pos);
            // TODO port:1.20.1 - GRASS_PATH renamed to DIRT_PATH
            return toBlock.getBlock() == Blocks.DIRT_PATH || toBlock.getBlock() == Blocks.FARMLAND;
        }

        @Override
        public boolean stacks(BlockGetter world, BlockPos pos, Direction.Axis axis, BlockState onBlock, BlockState stackingBlock, BlockState lastStacked) {
            //System.out.println("Stacks ? "+onBlock+" "+stackingBlock+" "+lastStacked);
            //System.out.println("RESULT "+(axis != Direction.Axis.Y && applies(world, pos, onBlock)));
            return axis != Direction.Axis.Y && applies(world, pos, onBlock);
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, Level world, BlockPos at, BlockState ofBlock, Direction.Axis axis) {
            //System.out.println("Add farm "+ofBlock+" "+at);
            if (axis == null) {
                terrainBoxConstructor.addBlockCollisions(world, at, ofBlock);
            } else {
                switch (axis) {
                    case X:
                        boxBuilder.expandX(1);
                        break;
                    case Z:
                        boxBuilder.expandZ(1);
                        break;
                }
            }
        }
    }

    static class Fences implements IBlockCollisionBehavior {
        private final List<MutableBoundingBox> waitingXBoxes = new ArrayList<>();
        private final List<MutableBoundingBox> waitingZBoxes = new ArrayList<>();
        private boolean hasXZLegs;

        @Override
        public boolean applies(BlockGetter world, BlockPos pos, BlockState toBlock) {
            return toBlock.getBlock() instanceof FenceBlock;
        }

        @Override
        public boolean stacks(BlockGetter world, BlockPos pos, Direction.Axis axis, BlockState onBlock, BlockState stackingBlock, BlockState lastStacked) {
            if (!(onBlock.getBlock() instanceof FenceBlock))
                return false;
            BlockState realStacking;
            switch (axis) {
                case Y:
                    return onBlock.equals(stackingBlock);
                case X:
                    // TODO port:1.20.1 - getActualState removed; BlockState already has connection properties
                    realStacking = stackingBlock;
                    return onBlock.equals(stackingBlock) && (realStacking.getValue(CrossCollisionBlock.EAST) || realStacking.getValue(CrossCollisionBlock.WEST));
                case Z:
                    realStacking = stackingBlock;
                    return onBlock.equals(stackingBlock) && (realStacking.getValue(CrossCollisionBlock.NORTH) || realStacking.getValue(CrossCollisionBlock.SOUTH));
            }
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, Level world, BlockPos at, BlockState ofBlock, Direction.Axis axis) {
            if (axis == null) {
                terrainBoxConstructor.addBlockCollisions(world, at, ofBlock);
                return;
            }
            if (axis == Direction.Axis.Y) {
                boxBuilder.expandY(1);
                return;
            }
            // TODO port:1.20.1 - getActualState removed; BlockState already has connection properties
            BlockState realStacking = ofBlock;
            // if (onBlock.getValue(BlockPane.SOUTH) || onBlock.getValue(BlockPane.NORTH))
            // return !onBlock.getValue(BlockPane.EAST) && !onBlock.getValue(BlockPane.WEST);
            switch (axis) {
                case X:
                    boxBuilder.expandX((realStacking.getValue(CrossCollisionBlock.EAST) ? 1 : 0.625f));
                    if (realStacking.getValue(CrossCollisionBlock.SOUTH) && !hasXZLegs)
                        waitingZBoxes.add(new MutableBoundingBox(0.375D, 0.0D, 0.625D, 0.625D, 1D, 1.0D).offset(at.getX(), at.getY(), at.getZ()));
                    break;
                case Z:
                    boxBuilder.expandZ((realStacking.getValue(CrossCollisionBlock.SOUTH) ? 1 : 0.625f));
                    if (realStacking.getValue(CrossCollisionBlock.EAST) && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0.625D, 0.0D, 0.375D, 1.0D, 1D, 0.625D).offset(at.getX(), at.getY(), at.getZ()));
                    if (realStacking.getValue(CrossCollisionBlock.WEST) && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0.0D, 0.0D, 0.375D, 0.375D, 1D, 0.625D).offset(at.getX(), at.getY(), at.getZ()));
                    break;
            }
        }

        @Override
        public TerrainBoxBuilder initBoxBuilder(TerrainBoxConstructor terrainBoxConstructor, Level world, BlockPos mutable, BlockState boxStart, double ox, double oy, double oz) {
            hasXZLegs = false;
            // TODO port:1.20.1 - getBoundingBox replaced by getShape().bounds()
            AABB box = boxStart.getShape(world, mutable).bounds();
            double maxZ = box.maxZ;
            if (maxZ == 1 && box.maxX == 1) { //if x-stacking, but z+ leg
                maxZ = 0.625D;
                hasXZLegs = true;
                waitingZBoxes.add(new MutableBoundingBox(0.375D, 0.0D, 0.625D, 0.625D, 1D, 1.0D).offset(mutable.getX(), mutable.getY(), mutable.getZ()));
            }

            return new TerrainBoxBuilder.MutableTerrainBoxBuilder(ox + box.minX, oy + box.minY, oz + box.minZ, box.maxX - box.minX, box.maxY - box.minY, maxZ - box.minZ) {
                @Override
                public void expandZ(float by) {
                    super.expandZ(by);
                    if (hasXZLegs) {
                        startZSize = box.maxZ - box.minZ;
                    }
                }
            };
        }

        @Override
        public void onBoxBuildEnd(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder) {
            if (boxBuilder.getXSize() == 0) {
                waitingXBoxes.forEach(terrainBoxConstructor::addMutable);
            }
            waitingXBoxes.clear();
            if (boxBuilder.getZSize() == 0) {
                waitingZBoxes.forEach(terrainBoxConstructor::addMutable);
            }
            waitingZBoxes.clear();
        }
    }

    static class Walls implements IBlockCollisionBehavior {
        private final List<MutableBoundingBox> waitingXBoxes = new ArrayList<>();
        private final List<MutableBoundingBox> waitingZBoxes = new ArrayList<>();
        private boolean hasXZLegs;

        @Override
        public boolean applies(BlockGetter world, BlockPos pos, BlockState toBlock) {
            return toBlock.getBlock() instanceof WallBlock;
        }

        @Override
        public boolean stacks(BlockGetter world, BlockPos pos, Direction.Axis axis, BlockState onBlock, BlockState stackingBlock, BlockState lastStacked) {
            if (!(onBlock.getBlock() instanceof WallBlock))
                return false;
            BlockState realStacking;
            // TODO port:1.20.1 - WallBlock NORTH/SOUTH/EAST/WEST changed type from boolean to WallSide enum
            switch (axis) {
                case Y:
                    return onBlock.equals(stackingBlock);
                case X:
                    realStacking = stackingBlock;
                    return onBlock.equals(stackingBlock) && (realStacking.getValue(BlockStateProperties.WEST_WALL) != WallSide.NONE || realStacking.getValue(BlockStateProperties.EAST_WALL) != WallSide.NONE);
                case Z:
                    realStacking = stackingBlock;
                    return onBlock.equals(stackingBlock) && (realStacking.getValue(BlockStateProperties.NORTH_WALL) != WallSide.NONE || realStacking.getValue(BlockStateProperties.SOUTH_WALL) != WallSide.NONE);
            }
            return false;
        }

        @Override
        public void addBlockCollision(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder, TerrainCollisionsCalculator.TerrainCursor cursor, Level world, BlockPos at, BlockState ofBlock, Direction.Axis axis) {
            if (axis == null) {
                terrainBoxConstructor.addBlockCollisions(world, at, ofBlock);
                return;
            }
            if (axis == Direction.Axis.Y) {
                boxBuilder.expandY(1);
                return;
            }
            // TODO port:1.20.1 - getActualState removed; BlockState already has connection properties
            BlockState realStacking = ofBlock;
            // TODO port:1.20.1 - WallBlock NORTH/SOUTH/EAST/WEST changed type from boolean to WallSide enum
            // if (onBlock.getValue(BlockPane.SOUTH) || onBlock.getValue(BlockPane.NORTH))
            // return !onBlock.getValue(BlockPane.EAST) && !onBlock.getValue(BlockPane.WEST);
            switch (axis) {
                case X:
                    boxBuilder.expandX((realStacking.getValue(BlockStateProperties.EAST_WALL) != WallSide.NONE ? 1 : 0.75f));
                    if (realStacking.getValue(BlockStateProperties.SOUTH_WALL) != WallSide.NONE && !hasXZLegs)
                        waitingZBoxes.add(new MutableBoundingBox(0.25D, 0.0D, 0.25D, 0.75D, 1.0D, 1.0D).offset(at.getX(), at.getY(), at.getZ()));
                    break;
                case Z:
                    boxBuilder.expandZ((realStacking.getValue(BlockStateProperties.SOUTH_WALL) != WallSide.NONE ? 1 : 0.75f));
                    if (realStacking.getValue(BlockStateProperties.EAST_WALL) != WallSide.NONE && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0.25D, 0.0D, 0.25D, 1.0D, 1.0D, 0.75D).offset(at.getX(), at.getY(), at.getZ()));
                    if (realStacking.getValue(BlockStateProperties.WEST_WALL) != WallSide.NONE && !hasXZLegs)
                        waitingXBoxes.add(new MutableBoundingBox(0.0D, 0.0D, 0.0D, 0.75D, 1.0D, 0.75D).offset(at.getX(), at.getY(), at.getZ()));
                    break;
            }
        }

        @Override
        public TerrainBoxBuilder initBoxBuilder(TerrainBoxConstructor terrainBoxConstructor, Level world, BlockPos mutable, BlockState boxStart, double ox, double oy, double oz) {
            hasXZLegs = false;
            // TODO port:1.20.1 - getBoundingBox replaced by getShape().bounds()
            AABB box = boxStart.getShape(world, mutable).bounds();
            double maxZ = box.maxZ;
            if (maxZ == 1 && box.maxX == 1) { //if x-stacking, but z+ leg
                maxZ = 0.75f;
                hasXZLegs = true;
                waitingZBoxes.add(new MutableBoundingBox(0.25D, 0.0D, 0.25D, 0.75D, 1.0D, 1.0D).offset(mutable.getX(), mutable.getY(), mutable.getZ()));
            }

            return new TerrainBoxBuilder.MutableTerrainBoxBuilder(ox + box.minX, oy + box.minY, oz + box.minZ, box.maxX - box.minX, box.maxY - box.minY, maxZ - box.minZ) {
                @Override
                public void expandZ(float by) {
                    super.expandZ(by);
                    if (hasXZLegs) {
                        startZSize = box.maxZ - box.minZ;
                    }
                }
            };
        }

        @Override
        public void onBoxBuildEnd(TerrainBoxConstructor terrainBoxConstructor, TerrainBoxBuilder boxBuilder) {
            if (boxBuilder.getXSize() == 0) {
                waitingXBoxes.forEach(terrainBoxConstructor::addMutable);
            }
            waitingXBoxes.clear();
            if (boxBuilder.getZSize() == 0) {
                waitingZBoxes.forEach(terrainBoxConstructor::addMutable);
            }
            waitingZBoxes.clear();
        }
    }
}
