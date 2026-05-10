package fr.dynamx.api.contentpack.object.part;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * A part that can be rendered in the world with the 3D model of an object (entity, block, item, etc).
 *
 * @param <A> The type of the owner of this part
 *
 * TODO port:1.20.1 - This interface originally referenced fr.dynamx.client.renders.scene.* types
 *   (SceneBuilder, SceneNode, IRenderContext) which live in Phase 7. We keep the API surface
 *   by typing those parameters as Object until that package is ported.
 */
public interface IDrawablePart<A extends IModelPackObject> {
    /**
     * Prevents the added parts from being rendered with the main obj model of the vehicle.
     *
     * @return The parts to hide when rendering the main obj model
     */
    default String[] getRenderedParts() {
        String objectName = getObjectName();
        return objectName == null ? new String[0] : new String[]{objectName};
    }

    /**
     * Adds this part to the scene graph.
     *
     * @param packInfo     The pack info of the entity (owner of the part)
     * @param sceneBuilder The scene builder. TODO port:1.20.1 - typed as Object until SceneBuilder is ported.
     */
    @OnlyIn(Dist.CLIENT)
    default void addToSceneGraph(A packInfo, Object sceneBuilder) {
        // TODO port:1.20.1 - sceneBuilder.addNode(packInfo, this) once SceneBuilder is ported.
    }

    /**
     * @return Whether this part should be linked to the entity or not.
     */
    @OnlyIn(Dist.CLIENT)
    default boolean isLinkedToEntity() {
        return true;
    }

    /**
     * Creates the scene node of this part.
     *
     * @param modelScale The scale of the model (usually the scaleModifier of the packInfo)
     * @param childGraph The child scene graph (parts that are linked to this part). Typed as List&lt;Object&gt; until SceneNode is ported.
     * @return The scene node of this part. Typed as Object until SceneNode is ported.
     */
    @OnlyIn(Dist.CLIENT)
    Object createSceneGraph(Vector3f modelScale, List<Object> childGraph);

    /**
     * @return The node name in the scene graph.
     */
    @OnlyIn(Dist.CLIENT)
    String getNodeName();

    /**
     * @return The name of the object in the 3D model. This is NOT the name of the part and this is NOT the node name.
     */
    String getObjectName();
}
