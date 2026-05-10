package fr.dynamx.common.items;

import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.common.items.tools.ItemWrench;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.world.item.Item;

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
     * TODO port:1.20.1 - was {@code CreativeTabs}; relaxed to {@link Object} until CreativeModeTab wiring lands in
     *  the entry-point phase. The icon-supplier and tab-name carry over once a real CreativeModeTab is built.
     */
    public static Object vehicleTab = new Object() {
        @Override
        public String toString() {
            return DynamXConstants.ID + "_vehicle";
        }
    };

    /**
     * TODO port:1.20.1 - was {@code CreativeTabs}; relaxed to {@link Object} until CreativeModeTab wiring lands.
     */
    public static Object objectTab = new Object() {
        @Override
        public String toString() {
            return DynamXConstants.ID + "_object";
        }
    };

    /**
     * TODO port:1.20.1 - was {@code List<CreativeTabs>}; relaxed to {@code List<Object>} for the same reason.
     */
    public static final List<Object> creativeTabs = new ArrayList<>();

    /**
     * TODO port:1.20.1 - Static instantiation of {@link ItemWrench} used to register the wrench at class-load time.
     *  Under DeferredRegister, this must move to a {@code RegistryObject<Item>} initialized in the entry-point phase.
     *  Kept as an eager static for now so legacy call sites resolve.
     */
    public static final Item ITEM_WRENCH = new ItemWrench();

    public static void add(Item item) {
        ITEMS.add(IResourcesOwner.of(item));
    }

    /**
     * TODO port:1.20.1 - {@code RegistryEvent.Register<Item>} no longer exists. The DeferredRegister model registers
     *  items eagerly when their suppliers run. This method is kept as a no-op stub so legacy call sites compile.
     */
    public static void injectItems(Object event) {
        // TODO port:1.20.1 - Implement via DeferredRegister in the entry-point phase.
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
