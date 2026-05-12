package fr.dynamx.common.core;

import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DynamXBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DynamXConstants.ID);

    public static final RegistryObject<BlockEntityType<TEDynamXBlock>> DYNAMX_BLOCK = BLOCK_ENTITIES.register(
            "dynamx_block",
            () -> BlockEntityType.Builder.of(TEDynamXBlock::new).build(null)
    );

    private DynamXBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent ev) ->
                TEDynamXBlock.TYPE = DYNAMX_BLOCK.get());
    }
}
