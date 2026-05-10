package fr.dynamx.api.events.client;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.neoforged.bus.api.Event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Fired when creating the scene node of a {@link IDrawablePart}.
 *
 * TODO port:1.20.1 - SceneNode and SceneNode.SceneRenderListener / SceneNode.SceneContainer
 *   live in fr.dynamx.client.renders.scene.node (Phase 7); typed as Object until then.
 */
@Getter
@RequiredArgsConstructor
public class CreatePartSceneEvent extends Event {
    /**
     * The pack info containing the part
     */
    private final IModelPackObject packInfo;
    /**
     * The part that is being rendered
     */
    private final IDrawablePart<?> part;
    /**
     * The scale of the model (of the pack info)
     */
    private final Vector3f modelScale;
    /**
     * The children of the part.
     */
    @Nullable
    private final List<Object> childGraph;
    /**
     * The scene graph that will be used to render the part. Can be overridden.
     */
    @Setter
    private Object overrideSceneNode;

    /**
     * @return The scene graph that will be used to render the part. If overrideSceneGraph is null, it will be created by the part.
     */
    @Nonnull
    public Object getSceneGraphResult() {
        if (overrideSceneNode == null) {
            overrideSceneNode = part.createSceneGraph(modelScale, childGraph);
        }
        return overrideSceneNode;
    }

    /**
     * Adds a listener to the scene graph that will be used to render the part.
     *
     * TODO port:1.20.1 - SceneNode.SceneRenderListener / SceneNode.SceneContainer are Phase 7.
     */
    public void listenPartScene(Object listener) {
        // TODO port:1.20.1 - overrideSceneNode = new SceneNode.SceneContainer(listener, part, getSceneGraphResult());
    }
}
