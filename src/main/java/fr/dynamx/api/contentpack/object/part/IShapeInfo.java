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
     *
     * TODO port:1.20.1 - PartShape.EnumPartType lives in fr.dynamx.common.contentpack.parts
     *   (Phase 3b). Returns null until that enum is ported. Callers must null-check.
     */
    default Object getShapeType() {
        return null;
    }
}
