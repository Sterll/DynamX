package fr.dynamx.common.core;

import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.items.DynamXItemArmor;
import fr.dynamx.common.items.DynamXItemBlock;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.common.items.ItemModularEntity;
import fr.dynamx.common.items.ItemProps;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class DynamXCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, DynamXConstants.ID);

    public static final RegistryObject<CreativeModeTab> VEHICLES = CREATIVE_TABS.register("vehicles",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + DynamXConstants.ID + ".vehicles"))
                    .icon(() -> new ItemStack(DynamXItems.WRENCH.get()))
                    .displayItems((params, output) -> {
                        output.accept(DynamXItems.WRENCH.get());
                        output.accept(DynamXItems.SHOCKWAVE.get());
                        output.accept(DynamXItems.SLOPES.get());
                        output.accept(DynamXItems.RAGDOLL.get());
                        forEachPackItem(item -> {
                            if (belongsInVehicleTab(item)) {
                                output.accept(item);
                            }
                        });
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> OBJECTS = CREATIVE_TABS.register("objects",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + DynamXConstants.ID + ".objects"))
                    .icon(() -> new ItemStack(DynamXItems.SLOPES.get()))
                    .displayItems((params, output) -> {
                        forEachPackItem(item -> {
                            if (belongsInObjectTab(item)) {
                                output.accept(item);
                            }
                        });
                    })
                    .build());

    private DynamXCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        CREATIVE_TABS.register(modBus);
    }

    private static void forEachPackItem(java.util.function.Consumer<Item> consumer) {
        for (IResourcesOwner owner : DynamXItemRegistry.getItems()) {
            Item item = owner.getItem();
            if (item == null) continue;
            if (item == DynamXItems.WRENCH.get() || item == DynamXItems.SHOCKWAVE.get()
                    || item == DynamXItems.SLOPES.get() || item == DynamXItems.RAGDOLL.get()) {
                continue;
            }
            if (isExcluded(item)) continue;
            consumer.accept(item);
        }
    }

    private static boolean isExcluded(Item item) {
        if (!(item instanceof IDynamXItem)) return false;
        Object info = ((IDynamXItem<?>) item).getInfo();
        if (info instanceof AbstractItemObject) {
            String tabName = ((AbstractItemObject<?, ?>) info).getCreativeTabName();
            return tabName != null && tabName.equalsIgnoreCase("None");
        }
        return false;
    }

    private static boolean belongsInVehicleTab(Item item) {
        return item instanceof ItemModularEntity;
    }

    private static boolean belongsInObjectTab(Item item) {
        return item instanceof DynamXItemBlock
                || item instanceof ItemProps
                || item instanceof DynamXItemArmor;
    }
}
