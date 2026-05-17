package fr.dynamx.common.contentpack.type.objects;

import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.common.contentpack.loader.InfoList;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

/**
 * TODO port:1.20.1 - Original referenced (not yet ported):
 *   - fr.dynamx.api.events.CreatePackItemEvent.SimpleItem (Phase 5 events)
 *   - fr.dynamx.api.events.client.BuildSceneGraphEvent.BuildItemScene (Phase 7)
 *   - fr.dynamx.client.renders.scene.node.{ItemNode,SceneNode} (Phase 7)
 *   - fr.dynamx.common.items.DynamXItem (Phase 6)
 *   - net.minecraftforge.common.MinecraftForge.EVENT_BUS - replaced by MinecraftForge.EVENT_BUS
 *   The createItem() / getSceneGraph() bodies are stubbed and return null until those Phases land.
 */
public class ItemObject<T extends ItemObject<T>> extends AbstractItemObject<T, T> {
    @Getter
    @Setter
    @PackFileProperty(configNames = "MaxItemStackSize", required = false, defaultValue = "1")
    protected int maxItemStackSize = 1;

    /**
     * TODO port:1.20.1 - Was SceneNode&lt;?, ?&gt;; relaxed to Object pending Phase 7 port.
     */
    protected fr.dynamx.client.renders.scene.node.SceneNode<?, ?> sceneNode;

    public ItemObject(String packName, String fileName) {
        super(packName, fileName);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected IDynamXItem<T> createItem(InfoList<T> loader) {
        return new fr.dynamx.common.items.DynamXItem(this);
    }

    @Override
    public String toString() {
        return "ItemObject named " + getFullName();
    }

    /**
     * TODO port:1.20.1 - Original signature was:
     *   IModelTextureVariants getTextureVariantsFor(ObjObjectRenderer objObjectRenderer)
     *   Phase 7 dependency. Returns null since items don't carry variants.
     */
    @Nullable
    public Object getTextureVariantsFor(Object objObjectRenderer) {
        return null;
    }

    public byte getMaxVariantId() {
        return 0;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public fr.dynamx.client.renders.scene.node.SceneNode<?, ?> getSceneGraph() {
        if (sceneNode == null) {
            fr.dynamx.client.renders.scene.SceneBuilder<
                    fr.dynamx.client.renders.scene.BaseRenderContext.ItemRenderContext,
                    ItemObject<T>> builder = new fr.dynamx.client.renders.scene.SceneBuilder<>();
            sceneNode = builder.buildItemSceneGraph((ItemObject<T>) this,
                    (java.util.List) getDrawableParts(),
                    new com.jme3.math.Vector3f(1, 1, 1));
        }
        return sceneNode;
    }
}
