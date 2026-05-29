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
    private static final List<fr.dynamx.common.blocks.DynamXBlock<?>> BLOCKS = new ArrayList<>();

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
     * Live, unmodifiable view of every pack item collected via {@link #add(Item)}.
     * Used by {@link fr.dynamx.common.core.DynamXCreativeTabs} to populate creative tabs after
     * {@link RegisterEvent} has flushed each item into the Forge registry.
     */
    public static List<IResourcesOwner> getItems() {
        return java.util.Collections.unmodifiableList(ITEMS);
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
        org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger("DynamX");
        int registered = 0;
        for (IResourcesOwner owner : ITEMS) {
            Item item = owner.getItem();
            if (item == null) {
                continue;
            }
            // ForgeRegistries.ITEMS.getKey(item) falls back to "minecraft:air" for unregistered
            // items in 1.20.1, so compare the round-tripped value to know whether this exact item
            // already owns a slot in the registry.
            ResourceLocation existing = ForgeRegistries.ITEMS.getKey(item);
            if (existing != null && ForgeRegistries.ITEMS.getValue(existing) == item) {
                continue;
            }
            String rawName;
            try {
                rawName = owner.getJsonName(0);
            } catch (Throwable t) {
                log.error("Failed to derive json name for {}", item, t);
                continue;
            }
            // Keep the json name verbatim (just lower-cased): the '.' separator is a VALID character
            // in a 1.20.1 ResourceLocation path ([a-z0-9/._-]) and the content packs ship their item
            // models under that exact name (e.g. "<pack>.vehicle_<name>_default.json"). Replacing '.'
            // with '_' here desynced the registry id from the shipped model file, which is what produced
            // the "Unable to load model ...#inventory / FileNotFoundException models/item/<name>.json" spam.
            String name = rawName.toLowerCase();
            ResourceLocation id;
            try {
                id = new ResourceLocation(DynamXConstants.ID, name);
            } catch (net.minecraft.ResourceLocationException e) {
                log.error("Invalid registry id derived from '{}' for item {}", name, item, e);
                continue;
            }
            if (ForgeRegistries.ITEMS.containsKey(id)) {
                log.warn("Duplicate registry id {} for item {} - skipping", id, item);
                continue;
            }
            event.register(ForgeRegistries.Keys.ITEMS, id, () -> item);
            registered++;
        }
        log.info("injectItems: registered {} pack item(s) out of {} owners", registered, ITEMS.size());
    }

    public static void registerItemBlock(Object block) {
        if (block instanceof fr.dynamx.common.blocks.DynamXBlock<?> dxBlock) {
            BLOCKS.add(dxBlock);
            add(new DynamXItemBlock(dxBlock));
        } else if (block instanceof net.minecraft.world.level.block.Block vanillaBlock) {
            add(new DynamXItemBlock(vanillaBlock));
        }
    }

    /**
     * Registers every {@link fr.dynamx.common.blocks.DynamXBlock} collected through
     * {@link #registerItemBlock(Object)} with the block registry.
     *
     * <p>Each {@code new DynamXBlock(...)} grabs an intrusive holder slot in
     * {@link ForgeRegistries#BLOCKS}; if the holder isn't consumed by a registry insertion before
     * {@code freezeData()} runs, the game crashes with
     * {@code IllegalStateException: Some intrusive holders were not registered}.
     * Pack-driven blocks get a registry id of the form {@code dynamxmod:<json-name>}.
     */
    public static void injectBlocks(RegisterEvent event) {
        if (!event.getRegistryKey().equals(ForgeRegistries.Keys.BLOCKS)) {
            return;
        }
        org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger("DynamX");
        int registered = 0;
        for (fr.dynamx.common.blocks.DynamXBlock<?> block : BLOCKS) {
            ResourceLocation existing = ForgeRegistries.BLOCKS.getKey(block);
            if (existing != null && ForgeRegistries.BLOCKS.getValue(existing) == block) {
                continue;
            }
            String rawName;
            try {
                rawName = block.getJsonName(0);
            } catch (Throwable t) {
                log.error("Failed to derive json name for block {}", block, t);
                continue;
            }
            // Same rule as items: keep the json name verbatim (lower-cased only). '.' is a valid
            // ResourceLocation path char in 1.20.1 and the generated blockstate is keyed off this id.
            String name = rawName.toLowerCase();
            ResourceLocation id;
            try {
                id = new ResourceLocation(DynamXConstants.ID, name);
            } catch (net.minecraft.ResourceLocationException e) {
                log.error("Invalid registry id derived from '{}' for block {}", name, block, e);
                continue;
            }
            if (ForgeRegistries.BLOCKS.containsKey(id)) {
                log.warn("Duplicate registry id {} for block {} - skipping", id, block);
                continue;
            }
            event.register(ForgeRegistries.Keys.BLOCKS, id, () -> block);
            registered++;
        }
        log.info("injectBlocks: registered {} pack block(s) out of {} collected", registered, BLOCKS.size());
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
