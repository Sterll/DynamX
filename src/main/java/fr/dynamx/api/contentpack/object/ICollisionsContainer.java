package fr.dynamx.api.contentpack.object;

import com.jme3.math.Vector3f;

/**
 * todo doc
 */
public interface ICollisionsContainer extends INamedObject {
    /**
     * @return The object's scale modifier
     */
    Vector3f getScaleModifier();

    fr.dynamx.common.contentpack.type.ObjectCollisionsHelper getCollisionsHelper();
}
