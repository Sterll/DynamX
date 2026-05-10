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
    protected Object sceneNode;

    public ItemObject(String packName, String fileName) {
        super(packName, fileName);
    }

    @Override
    protected IDynamXItem<T> createItem(InfoList<T> loader) {
        // TODO port:1.20.1 - Original:
        //   CreatePackItemEvent.SimpleItem event = new CreatePackItemEvent.SimpleItem(loader, this);
        //   MinecraftForge.EVENT_BUS.post(event);
        //   if (event.isOverridden()) return event.getObjectItem();
        //   else return new DynamXItem(this);
        //   Both depend on Phase 5/6.
        return null;
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
    public Object getSceneGraph() {
        // TODO port:1.20.1 - Original:
        //   if (sceneNode == null) {
        //       if (isModelValid()) { post BuildItemScene event, sceneNode = event.getSceneGraphResult(); }
        //       else sceneNode = new ItemNode<>(Collections.EMPTY_LIST);
        //   }
        //   ItemNode/BuildSceneGraphEvent live in Phase 7.
        return sceneNode;
    }
}
