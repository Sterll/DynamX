package fr.dynamx.common.items;

import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.common.items.tools.ItemWrench;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Central broker that the legacy code uses to gather every DynamX item before registering it with Forge.
 *
 * TODO port:1.20.1 - Full migration to {@link net.minecraftforge.registries.DeferredRegister} will happen in
 * the entry-point wiring phase. For now we keep the legacy API surface (add / injectItems / registerItemBlock /
 * registerItemModels) so the rest of the codebase compiles, but the actual registry calls are stubbed:
 *  - {@link net.minecraft.world.item.CreativeModeTab} replaced {@code CreativeTabs}; instances are now built via
 *    {@code CreativeModeTab.builder(...)} and registered through {@code CreativeModeTabRegistryEvent}.
 *  - {@code IForgeRegistry<Item>} is gone — {@code DeferredRegister<Item>} drives registration.
 *  - {@code ModelLoader#setCustomModelResourceLocation} disappeared in 1.16+; baked-model loaders + ItemModelShaper
 *    are the new approach. Renderer wiring lives in DynamXContext.getDxModelRegistry() once the client phase lands.
 *
 * To avoid blocking compilation we replace the two static {@code CreativeTabs} singletons with {@code Object}
 * placeholders that match the {@link fr.dynamx.common.contentpack.type.objects.AbstractItemObject#getCreativeTab(Object)}
 * contract (which itself was relaxed to {@link Object} during Phase 3b).
 */
public class DynamXItemRegistry {
    private static final List<IResourcesOwner> ITEMS = new ArrayList<>();

    /**
     * Vehicle creative tab handle (kept as Object for the legacy
     * {@link fr.dynamx.common.contentpack.type.objects.AbstractItemObject#getCreativeTab(Object)} contract).
     * Backed by {@link fr.dynamx.common.core.DynamXCreativeTabs#VEHICLES}.
     */
    public static Object vehicleTab = new Object() {
        @Override
        public String toString() {
            return DynamXConstants.ID + "_vehicle";
        }
    };

    /**
     * Object creative tab handle. Backed by {@link fr.dynamx.common.core.DynamXCreativeTabs#OBJECTS}.
     */
    public static Object objectTab = new Object() {
        @Override
        public String toString() {
            return DynamXConstants.ID + "_object";
        }
    };

    public static final List<Object> creativeTabs = new ArrayList<>();

    /**
     * Legacy compat accessor for the wrench item. Backed by
     * {@link fr.dynamx.common.core.DynamXItems#WRENCH}. Returns {@code null} before
     * RegisterEvent fires; callers that compare references should still be safe (null != stack item).
     */
    public static Item getItemWrench() {
        try {
            return fr.dynamx.common.core.DynamXItems.WRENCH.get();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    public static void add(Item item) {
        ITEMS.add(IResourcesOwner.of(item));
    }

    /**
     * Registers every item collected through {@link #add(Item)} with the item registry.
     *
     * <p>Items already registered (e.g. tools wired via {@code DynamXItems} {@code DeferredRegister}) are
     * skipped to avoid duplicate registration. Content-pack items get a registry id of the form
     * {@code dynamxmod:<json-name>}.
     */
    public static void injectItems(RegisterEvent event) {
        if (!event.getRegistryKey().equals(ForgeRegistries.Keys.ITEMS)) {
            return;
        }
        for (IResourcesOwner owner : ITEMS) {
            Item item = owner.getItem();
            if (item == null) {
                continue;
            }
            ResourceLocation existing = ForgeRegistries.ITEMS.getKey(item);
            if (existing != null && ForgeRegistries.ITEMS.containsKey(existing)) {
                continue;
            }
            String name = owner.getJsonName(0).toLowerCase();
            ResourceLocation id = new ResourceLocation(DynamXConstants.ID, name);
            if (ForgeRegistries.ITEMS.containsKey(id)) {
                continue;
            }
            event.register(ForgeRegistries.Keys.ITEMS, id, () -> item);
        }
    }

    public static void registerItemBlock(Object block) {
        // TODO port:1.20.1 - DynamXBlock not yet ported (Phase 4); accepted as Object and downcast when available.
        if (block instanceof net.minecraft.world.level.block.Block) {
            add(new DynamXItemBlock((net.minecraft.world.level.block.Block) block));
        }
    }

    /**
     * TODO port:1.20.1 - {@code ModelRegistryEvent} + {@code ModelLoader#setCustomModelResourceLocation} are gone.
     *  Item models now flow through {@code ModelEvent.RegisterAdditional} + custom {@code BakedModel} loaders.
     *  Implementation deferred to the client phase.
     */
    public static void registerItemModels(Object event) {
        // TODO port:1.20.1 - See above.
    }

    /**
     * TODO port:1.20.1 - {@code ModelResourceLocation} + {@code ModelLoader#setCustomModelResourceLocation} removed.
     *  Implementation deferred to the client phase.
     */
    public static void registerModel(IResourcesOwner item, byte metadata) {
        // TODO port:1.20.1 - See above.
    }
}
