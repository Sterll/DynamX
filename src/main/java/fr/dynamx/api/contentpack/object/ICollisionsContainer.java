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

    // TODO port:1.20.1 - ObjectCollisionsHelper is in fr.dynamx.common.contentpack.type (Phase 3b);
    //   typed as Object until that package is ported.
    Object getCollisionsHelper();
}
