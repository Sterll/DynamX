package fr.dynamx.api.blocks;

import fr.dynamx.api.entities.modules.IBaseModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.LogicalSide;

/**
 * Base implementation of a {@code fr.dynamx.common.blocks.TEDynamXBlock} module
 */
// TODO port:1.20.1 - TEDynamXBlock (block entity) not yet ported (Phase 4 blocks).
public interface IBlockEntityModule extends IBaseModule {
    /**
     * Called when the tile entity was just loaded
     */
    default void initBlockEntityProperties() {
    }

    /**
     * Called when the block is break
     */
    default void onBlockBreak() {
    }

    /**
     * Fills the drops list with the block drops when the block is broken
     */
    // TODO port:1.20.1 - 1.12 IBlockAccess replaced by BlockGetter; signature kept for downstream compatibility.
    default void getBlockDrops(NonNullList<ItemStack> drops, BlockGetter world, BlockPos pos, BlockState state, int fortune) {
    }

    /**
     * Implement this on you module to listen tile entity updates
     */
    interface IBlockEntityUpdateListener {
        /**
         * @return True to listen this update on this side (default is true on all sides)
         */
        // TODO port:1.20.1 - Forge Side replaced by NeoForge LogicalSide.
        default boolean listenBlockEntityUpdates(LogicalSide side) {
            return true;
        }

        /**
         * Called when updating the tile entity
         */
        default void updateBlockEntity() {
        }
    }
}
