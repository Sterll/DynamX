package fr.dynamx.api.events.client;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.node.ArmorNode;
import fr.dynamx.client.renders.scene.node.BlockNode;
import fr.dynamx.client.renders.scene.node.EntityNode;
import fr.dynamx.client.renders.scene.node.ItemNode;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.contentpack.type.objects.ItemObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Build scene graph events.
 */
@Getter
@RequiredArgsConstructor
public abstract class BuildSceneGraphEvent<A extends IModelPackObject> extends Event {
    /**
     * The scene builder.
     */
    protected final SceneBuilder<?, A> sceneBuilder = new SceneBuilder<>();
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
    protected SceneNode<?, A> overrideSceneNode;

    /**
     * @return The scene graph that will be used to render the pack info.
     */
    @Nonnull
    public abstract SceneNode<?, A> getSceneGraphResult();

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
        @SuppressWarnings({"unchecked", "rawtypes"})
        public SceneNode<?, IPhysicsPackInfo> getSceneGraphResult() {
            if (overrideSceneNode == null) {
                overrideSceneNode = (SceneNode) new SceneBuilder().buildEntitySceneGraph(packInfo, drawableParts, modelScale);
            }
            return (SceneNode<?, IPhysicsPackInfo>) (SceneNode) overrideSceneNode;
        }
    }

    /**
     * Fired when creating the scene of a block pack info.
     */
    public static class BuildBlockScene extends BuildSceneGraphEvent<BlockObject<?>> {
        public BuildBlockScene(BlockObject<?> packInfo, List<IDrawablePart<BlockObject<?>>> drawableParts, Vector3f modelScale) {
            super(packInfo, drawableParts, modelScale);
        }

        @Override
        @Nonnull
        @SuppressWarnings({"unchecked", "rawtypes"})
        public SceneNode<?, BlockObject<?>> getSceneGraphResult() {
            if (overrideSceneNode == null) {
                overrideSceneNode = (SceneNode) new SceneBuilder().buildBlockSceneGraph(packInfo, drawableParts, modelScale);
            }
            return (SceneNode<?, BlockObject<?>>) (SceneNode) overrideSceneNode;
        }
    }

    /**
     * Fired when creating the scene of an armor pack info.
     */
    public static class BuildArmorScene extends BuildSceneGraphEvent<ArmorObject<?>> {
        public BuildArmorScene(ArmorObject<?> packInfo, List<IDrawablePart<ArmorObject<?>>> drawableParts) {
            super(packInfo, drawableParts, new Vector3f(1, 1, 1));
        }

        @Override
        @Nonnull
        @SuppressWarnings({"unchecked", "rawtypes"})
        public SceneNode<?, ArmorObject<?>> getSceneGraphResult() {
            if (overrideSceneNode == null) {
                overrideSceneNode = (SceneNode) new SceneBuilder().buildArmorSceneGraph(packInfo, drawableParts, modelScale);
            }
            return (SceneNode<?, ArmorObject<?>>) (SceneNode) overrideSceneNode;
        }
    }

    /**
     * Fired when creating the scene of an item pack info.
     */
    public static class BuildItemScene extends BuildSceneGraphEvent<ItemObject<?>> {
        public BuildItemScene(ItemObject<?> packInfo, List<IDrawablePart<ItemObject<?>>> drawableParts) {
            super(packInfo, drawableParts, new Vector3f(1, 1, 1));
        }

        @Override
        @Nonnull
        @SuppressWarnings({"unchecked", "rawtypes"})
        public SceneNode<?, ItemObject<?>> getSceneGraphResult() {
            if (overrideSceneNode == null) {
                overrideSceneNode = (SceneNode) new SceneBuilder().buildItemSceneGraph(packInfo, drawableParts, modelScale);
            }
            return (SceneNode<?, ItemObject<?>>) (SceneNode) overrideSceneNode;
        }
    }
}
