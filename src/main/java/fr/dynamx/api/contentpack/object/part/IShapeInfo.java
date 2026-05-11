package fr.dynamx.api.contentpack.object.part;

import com.jme3.math.Vector3f;
import fr.dynamx.utils.optimization.MutableBoundingBox;

/**
 * A simple cuboid collision shape
 * <p>
 * todo outdated doc
 */
public interface IShapeInfo {
    /**
     * @return Center of the shape
     */
    Vector3f getPosition();

    /**
     * @return Half of the total size on each side
     */
    Vector3f getSize();

    MutableBoundingBox getBoundingBox();

    /**
     * @return The shape type.
     */
    default fr.dynamx.common.contentpack.parts.PartShape.EnumPartType getShapeType() {
        return fr.dynamx.common.contentpack.parts.PartShape.EnumPartType.BOX;
    }
}
