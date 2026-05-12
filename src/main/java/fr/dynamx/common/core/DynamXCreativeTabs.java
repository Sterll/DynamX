package fr.dynamx.common.core;

import fr.dynamx.utils.DynamXConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
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
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> OBJECTS = CREATIVE_TABS.register("objects",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + DynamXConstants.ID + ".objects"))
                    .icon(() -> new ItemStack(DynamXItems.SLOPES.get()))
                    .displayItems((params, output) -> {
                    })
                    .build());

    private DynamXCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        CREATIVE_TABS.register(modBus);
    }
}
