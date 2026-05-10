package fr.dynamx.common.items.tools;

import com.jme3.math.Vector3f;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * TODO port:1.20.1 - Ragdoll item:
 *  - {@code onItemRightClick(...)} -> {@link #use(Level, Player, InteractionHand)} returning
 *    {@link InteractionResultHolder}.
 *  - {@code RayTraceResult} -> {@code HitResult}; {@code BlockHitResult.getBlockPos()} replaces {@code getBlockPos()}.
 *  - {@code Blocks.SNOW_LAYER} -> {@code Blocks.SNOW}.
 *  - {@code BlockPos#down} -> {@code BlockPos#below}.
 *  - {@code player.capabilities.isCreativeMode} -> {@code player.getAbilities().instabuild}.
 *  - {@code player.addStat(StatList.getObjectUseStats(this))} -> {@code player.awardStat(Stats.ITEM_USED.get(this))}.
 *  - {@code World#spawnEntity} -> {@link Level#addFreshEntity(net.minecraft.world.entity.Entity)}.
 *  - {@code fr.dynamx.common.entities.RagdollEntity} not yet ported (Phase 6); {@link #getSpawnEntity} returns Object.
 *  - {@code DynamXUtils.rayTraceEntitySpawn(...)} not yet present in the ported utils.
 */
public class ItemRagdoll extends Item {
    public ItemRagdoll() {
        super(new Properties());
        // TODO port:1.20.1 - setRegistryName / setTranslationKey / setCreativeTab removed; handled by DeferredRegister.
        DynamXItemRegistry.add(this);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand hand) {
        ItemStack itemstack = playerIn.getItemInHand(hand);
        // TODO port:1.20.1 - DynamXUtils.rayTraceEntitySpawn(...) not yet ported. Once available, replicate the
        //  raycast (block hit, snow-layer drop, spawn, creative stack-decrement, stat-award).
        return InteractionResultHolder.pass(itemstack);
    }

    public boolean spawnEntity(ItemStack itemStackIn, Level worldIn, Player playerIn, BlockPos blockpos) {
        if (!worldIn.isClientSide) {
            if (playerIn.isShiftKeyDown()) {
                for (int i = -10; i < 10; i += 4) {
                    for (int j = -10; j < 10; j += 4) {
                        Vector3f pos = new Vector3f(blockpos.getX() + i, blockpos.getY(), blockpos.getZ() + j);
                        Object entity = getSpawnEntity(worldIn, playerIn, Vector3fPool.get(pos.x, pos.y + 2.3F, pos.z), playerIn.getYRot() % 360.0F, 0);
                        if (entity instanceof net.minecraft.world.entity.Entity)
                            worldIn.addFreshEntity((net.minecraft.world.entity.Entity) entity);
                    }
                }
            } else {
                Object entity = getSpawnEntity(worldIn, playerIn,
                        Vector3fPool.get(blockpos.getX(), blockpos.getY() + 2.19F, blockpos.getZ()).add(new Vector3f(0.5f, 0, 0.5f)),
                        playerIn.getYRot() % 360.0F, 0);
                if (entity instanceof net.minecraft.world.entity.Entity)
                    worldIn.addFreshEntity((net.minecraft.world.entity.Entity) entity);
            }
        }
        return true;
    }

    /**
     * TODO port:1.20.1 - {@code RagdollEntity} not yet ported (Phase 6); return type relaxed to {@link Object}.
     */
    public Object getSpawnEntity(Level worldIn, Player playerIn, Vector3f pos, float spawnRotation, int metadata) {
        // return new RagdollEntity(worldIn, pos, spawnRotation, playerIn.getName().getString());
        return null;
    }
}
