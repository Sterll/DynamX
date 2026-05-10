package fr.dynamx.api.events.client;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.neoforged.bus.api.Event;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Build scene graph events.
 *
 * TODO port:1.20.1 - SceneBuilder / SceneNode / IRenderContext / BaseRenderContext.*RenderContext
 *   and ArmorObject / BlockObject / ItemObject live in not-yet-ported packages (Phase 3b / 7).
 *   The originally-typed scene generic (C extends IRenderContext) is dropped here and scene types
 *   are typed as Object. Subclasses keep their distinct context shape for documentation; tighten
 *   when those packages are ported.
 */
@Getter
@RequiredArgsConstructor
public abstract class BuildSceneGraphEvent<A extends IModelPackObject> extends Event {
    /**
     * The scene builder. TODO port:1.20.1 - SceneBuilder is Phase 7.
     */
    protected final Object sceneBuilder = null;
    /**
     * The pack info that is being compiled into a scene graph
     */
    protected final A packInfo;
    /**
     * The drawable parts of the pack info
     */
    protected final List<IDrawablePart<A>> drawableParts;
    /**
     * The scale of the model (of the pack info)
     */
    protected final Vector3f modelScale;
    /**
     * The scene graph that will be used to render the pack info. Can be overridden.
     */
    @Setter
    protected Object overrideSceneNode;

    /**
     * @return The scene graph that will be used to render the pack info.
     */
    @Nonnull
    public abstract Object getSceneGraphResult();

    /**
     * Adds an isolated scene node to the pack info.
     */
    public void addSceneNode(String nodeName, BiFunction<Vector3f, List<Object>, Object> sceneNode) {
        drawableParts.add(new IDrawablePart<A>() {
            @Override
            public Object createSceneGraph(Vector3f modelScale, List<Object> childGraph) {
                return sceneNode.apply(modelScale, childGraph);
            }

            @Override
            public String getNodeName() {
                return nodeName;
            }

            @Override
            public String getObjectName() {
                return null;
            }
        });
    }

    /**
     * Fired when creating the scene of an entity pack info.
     */
    public static class BuildEntityScene extends BuildSceneGraphEvent<IPhysicsPackInfo> {
        public BuildEntityScene(IPhysicsPackInfo packInfo, List<IDrawablePart<IPhysicsPackInfo>> iDrawableParts, Vector3f modelScale) {
            super(packInfo, iDrawableParts, modelScale);
        }

        @Override
        @Nonnull
        public Object getSceneGraphResult() {
            // TODO port:1.20.1 - SceneBuilder.buildEntitySceneGraph(packInfo, drawableParts, modelScale); Phase 7.
            return overrideSceneNode == null ? (overrideSceneNode = new Object()) : overrideSceneNode;
        }
    }

    /**
     * Fired when creating the scene of a block pack info.
     *
     * TODO port:1.20.1 - BlockObject lives in fr.dynamx.common.contentpack.type.objects (Phase 3b);
     *   the generic parameter is dropped to keep this event compilable.
     */
    public static class BuildBlockScene extends BuildSceneGraphEvent<IModelPackObject> {
        public BuildBlockScene(IModelPackObject packInfo, List<IDrawablePart<IModelPackObject>> drawableParts, Vector3f modelScale) {
            super(packInfo, drawableParts, modelScale);
        }

        @Override
        @Nonnull
        public Object getSceneGraphResult() {
            return overrideSceneNode == null ? (overrideSceneNode = new Object()) : overrideSceneNode;
        }
    }

    /**
     * Fired when creating the scene of an armor pack info.
     *
     * TODO port:1.20.1 - ArmorObject lives in fr.dynamx.common.contentpack.type.objects (Phase 3b).
     */
    public static class BuildArmorScene extends BuildSceneGraphEvent<IModelPackObject> {
        public BuildArmorScene(IModelPackObject packInfo, List<IDrawablePart<IModelPackObject>> drawableParts) {
            super(packInfo, drawableParts, new Vector3f(1, 1, 1));
        }

        @Override
        @Nonnull
        public Object getSceneGraphResult() {
            return overrideSceneNode == null ? (overrideSceneNode = new Object()) : overrideSceneNode;
        }
    }

    /**
     * Fired when creating the scene of an item pack info.
     *
     * TODO port:1.20.1 - ItemObject lives in fr.dynamx.common.contentpack.type.objects (Phase 3b).
     */
    public static class BuildItemScene extends BuildSceneGraphEvent<IModelPackObject> {
        public BuildItemScene(IModelPackObject packInfo, List<IDrawablePart<IModelPackObject>> drawableParts) {
            super(packInfo, drawableParts, new Vector3f(1, 1, 1));
        }

        @Override
        @Nonnull
        public Object getSceneGraphResult() {
            return overrideSceneNode == null ? (overrideSceneNode = new Object()) : overrideSceneNode;
        }
    }
}
