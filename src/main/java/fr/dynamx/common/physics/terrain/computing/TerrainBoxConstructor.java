package fr.dynamx.common.physics.terrain.computing;

import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.common.physics.terrain.element.CompoundBoxTerrainElement;
import fr.dynamx.common.physics.terrain.element.EmptyTerrainElement;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Holds all boxes during the construction of one {@link fr.dynamx.common.physics.terrain.chunk.ChunkCollisions} by the {@link TerrainCollisionsCalculator}
 *
 * @see TerrainBoxBuilder
 */
public class TerrainBoxConstructor {
    /**
     * The maximum amount of axis aligned collision boxes in one terrain collision element
     */
    private static final int MAX_BOXES_PER_MESH = 60;

    private final AABB searchZone;
    private final List<ITerrainElement> otherTerrainElements = new ArrayList<>();
    //Optimized, grouped collisions of many blocks (full cubes, slabs and snow)
    private final List<MutableBoundingBox> outListMutable = new ArrayList<>();
    //Collisions of special blocks like flower pots with no optimization
    private final List<AABB> outListVanilla = new ArrayList<>();
    private final int x, y, z;
    private final boolean debug;

    public TerrainBoxConstructor(AABB searchZone, int x, int y, int z, boolean debug) {
        this.searchZone = searchZone;
        this.x = x;
        this.y = y;
        this.z = z;
        this.debug = debug;
    }

    public AABB getSearchZone() {
        return searchZone;
    }

    public void addMutable(MutableBoundingBox boundingBox) {
        if (boundingBox == null)
            throw new NullPointerException("You can't add a null boundingBox.... " + x + " " + y + " " + z);
        outListMutable.add(boundingBox);
    }

    public void addBlockCollisions(Level world, BlockPos at, BlockState ofBlock) {
        // TODO port:1.20.1 - addCollisionBoxToList replaced by VoxelShape.toAabbs() offset by pos
        if (isDebug()) {
            List<AABB> boxes = new ArrayList<>();
            ofBlock.getCollisionShape(world, at).toAabbs().forEach(b -> boxes.add(b.move(at.getX(), at.getY(), at.getZ())));
            injectBlockCollisions(at, ofBlock, boxes);
        } else {
            ofBlock.getCollisionShape(world, at).toAabbs().forEach(b -> getOutListVanilla().add(b.move(at.getX(), at.getY(), at.getZ())));
        }
    }

    public void injectBlockCollisions(BlockPos at, BlockState ofBlock, List<AABB> boxes) {
        if (isDebug())
            System.out.println("Injecting " + boxes.size() + " boxes at " + at + " for " + ofBlock);
        outListVanilla.addAll(boxes);
    }

    public List<AABB> getOutListVanilla() {
        return outListVanilla;
    }

    public void addCustomShapedElement(ITerrainElement element) {
        otherTerrainElements.add(element);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public boolean isDebug() {
        return debug;
    }

    /**
     * Builds all collected terrain elements in the search zone, and returns them
     */
    public List<ITerrainElement> getTerrainElements() {
        //The result
        List<ITerrainElement> result = new ArrayList<>(otherTerrainElements);

        //Create complex collisions meshes, keeping maximum MAX_BOXES_PER_MESH in each
        List<MutableBoundingBox> vanillaBoxes = new ArrayList<>();
        //Sort them by y pos
        outListVanilla.sort(Comparator.comparingDouble(a -> {
            if (a != null) {
                return a.minY;
            }
            return 0;
        }));
        int count = 0;
        for (AABB box : outListVanilla) {
            if (box != null) {
                vanillaBoxes.add(new MutableBoundingBox(box));
                count++;
                if (count >= MAX_BOXES_PER_MESH) {
                    result.add(new CompoundBoxTerrainElement(-x, -y, -z, vanillaBoxes));
                    vanillaBoxes = new ArrayList<>();
                    count = 0;
                }
            }
        }
        if (!vanillaBoxes.isEmpty()) {
            result.add(new CompoundBoxTerrainElement(-x, -y, -z, vanillaBoxes));
        }
        //And add all standard TerrainElements
        if (!outListMutable.isEmpty()) {
            result.add(new CompoundBoxTerrainElement(-x, -y, -z, outListMutable));
        }
        if (result.isEmpty()) {
            // Helps to know that chunk collisions had been successfully loaded, but for an empty chunk
            result.add(new EmptyTerrainElement());
        }
        return result;
    }
}
