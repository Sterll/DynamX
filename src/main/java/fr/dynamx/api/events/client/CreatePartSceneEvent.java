package fr.dynamx.api.events.client;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.client.renders.scene.node.SceneNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Fired when creating the scene node of a {@link IDrawablePart}.
 *
 * <p>TODO port:1.20.1 - SceneNode.SceneRenderListener / SceneNode.SceneContainer are not yet implemented;
 *   {@link #listenPartScene(Object)} is a no-op stub until they land.
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
    private final List<SceneNode<?, ?>> childGraph;
    /**
     * The scene graph that will be used to render the part. Can be overridden.
     */
    @Setter
    private SceneNode<?, ?> overrideSceneNode;

    /**
     * @return The scene graph that will be used to render the part. If overrideSceneGraph is null, it will be created by the part.
     */
    @Nonnull
    @SuppressWarnings({"unchecked", "rawtypes"})
    public SceneNode<?, ?> getSceneGraphResult() {
        if (overrideSceneNode == null) {
            overrideSceneNode = (SceneNode<?, ?>) part.createSceneGraph(modelScale, (List) childGraph);
        }
        return overrideSceneNode;
    }

    /**
     * Adds a listener to the scene graph that will be used to render the part.
     *
     * <p>TODO port:1.20.1 - SceneNode.SceneRenderListener / SceneNode.SceneContainer not yet ported.
     */
    public void listenPartScene(Object listener) {
        // TODO port:1.20.1 - overrideSceneNode = new SceneNode.SceneContainer(listener, part, getSceneGraphResult());
    }
}
