package fr.dynamx.common.core;

import fr.dynamx.common.items.tools.ItemRagdoll;
import fr.dynamx.common.items.tools.ItemShockWave;
import fr.dynamx.common.items.tools.ItemSlopes;
import fr.dynamx.common.items.tools.ItemWrench;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DynamXItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DynamXConstants.ID);

    public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench", ItemWrench::new);
    public static final RegistryObject<Item> SHOCKWAVE = ITEMS.register("shockwave", ItemShockWave::new);
    public static final RegistryObject<Item> SLOPES = ITEMS.register("slopes", ItemSlopes::new);
    public static final RegistryObject<Item> RAGDOLL = ITEMS.register("ragdoll", ItemRagdoll::new);

    private DynamXItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
