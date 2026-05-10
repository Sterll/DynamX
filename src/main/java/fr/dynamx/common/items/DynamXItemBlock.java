package fr.dynamx.common.items;

import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * TODO port:1.20.1 - Block + ItemBlock wiring changed:
 *  - {@code ItemBlock} -> {@link BlockItem}; constructor signature now takes a {@link Block} + {@link Properties}.
 *  - {@code onItemUse(EntityPlayer, World, BlockPos, EnumHand, EnumFacing, float, float, float)} ->
 *    {@link #useOn(UseOnContext)}.
 *  - {@link Block#getRegistryName()} no longer exists; ResourceLocation must be looked up from the registry.
 *  - {@code setHasSubtypes(true)} / {@code setMaxDamage(0)} removed. Variants live in data-components/NBT now.
 *  - {@code IBlockState} -> {@code BlockState}; {@code placeBlockAt(...)} bypasses superclass and is now handled
 *    by {@link BlockItem#place(net.minecraft.world.item.context.BlockPlaceContext)}.
 *  - {@code DynamXBlock} comes from Phase 4 (not yet ported); strongly-typed reference relaxed to {@link Block}
 *    + raw {@code Object} casts where needed.
 */
public class DynamXItemBlock extends BlockItem implements IResourcesOwner, IDynamXItem<BlockObject<?>> {

    // TODO port:1.20.1 - DynamXBlock<BlockObject<?>> not yet ported (Phase 4). Stored as the parent Block reference
    //  and queried generically via reflection-light helpers when needed. Strongly-typed accessors are stubbed.
    private final Block blockIn;

    public DynamXItemBlock(Block block) {
        super(block, new Properties());
        this.blockIn = block;
        // TODO port:1.20.1 - setRegistryName / setHasSubtypes / setMaxDamage no longer available.
        //  Variant support is data-component based; registry name comes from DeferredRegister in the entry-point phase.
    }

    // TODO port:1.20.1 - getSubItems removed; populate creative tabs via CreativeModeTabRegistryEvent.
    /*
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) { ... }
    */

    @Override
    public String getDescriptionId(ItemStack stack) {
        // TODO port:1.20.1 - stack.getMetadata() removed; variant byte is 0 until data-components migration lands.
        return super.getDescriptionId(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // TODO port:1.20.1 - Legacy onItemUse(...) reimplemented natively by BlockItem#useOn through
        //  BlockItem#place(BlockPlaceContext). The custom placement logic (criteria trigger, sound, shrink)
        //  is already handled by the superclass; legacy override removed and behavior delegated upstream.
        return super.useOn(context);
    }

    // TODO port:1.20.1 - placeBlockAt was a 1.12 helper; BlockItem#place(BlockPlaceContext) replaces it natively
    //  and triggers CriteriaTriggers.PLACED_BLOCK internally.

    @Override
    public String getJsonName(int meta) {
        // TODO port:1.20.1 - DynamXBlock not yet ported; the block is expected to implement IResourcesOwner once Phase 4 lands.
        if (blockIn instanceof IResourcesOwner) {
            return ((IResourcesOwner) blockIn).getJsonName(meta);
        }
        return blockIn.getDescriptionId();
    }

    @Override
    public IModelPackObject getDxModel() {
        if (blockIn instanceof IResourcesOwner) {
            return ((IResourcesOwner) blockIn).getDxModel();
        }
        return null;
    }

    @Override
    public int getMaxMeta() {
        // TODO port:1.20.1 - DynamXBlock not yet ported; default to 1 until Phase 4 wires its getMaxMeta().
        return 1;
    }

    @Override
    public boolean createJson() {
        // TODO port:1.20.1 - DynamXBlock not yet ported; default to false.
        return false;
    }

    @Override
    public boolean createTranslation() {
        // TODO port:1.20.1 - DynamXBlock not yet ported; default to true.
        return true;
    }

    @Override
    public BlockObject<?> getInfo() {
        // TODO port:1.20.1 - DynamXBlock<BlockObject<?>> not yet ported (Phase 4); returning null until then.
        return null;
    }

    @Override
    public void setInfo(BlockObject<?> info) {
        // TODO port:1.20.1 - DynamXBlock not yet ported; no-op stub.
    }

    @Override
    public String toString() {
        return "DynamXItemBlock{" +
                "dynamxMainBlock=" + blockIn +
                '}';
    }
}
