package fr.dynamx.common.items.tools;

import com.jme3.math.Vector3f;
import fr.dynamx.common.core.DynamXEntities;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ItemRagdoll extends Item {
    public ItemRagdoll() {
        super(new Properties());
        DynamXItemRegistry.add(this);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand hand) {
        ItemStack itemstack = playerIn.getItemInHand(hand);

        BlockHitResult hit = playerLookRay(worldIn, playerIn);
        BlockPos target;
        if (hit.getType() == HitResult.Type.BLOCK) {
            target = hit.getBlockPos().above();
        } else {
            Vec3 eye = playerIn.getEyePosition(1.0F);
            target = BlockPos.containing(eye.x, eye.y, eye.z);
        }

        if (!worldIn.isClientSide) {
            spawnEntity(itemstack, worldIn, playerIn, target);
            playerIn.awardStat(Stats.ITEM_USED.get(this));
            if (!playerIn.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(itemstack, worldIn.isClientSide);
    }

    private static BlockHitResult playerLookRay(Level world, Player player) {
        float reach = 5.0F;
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.x * reach, look.y * reach, look.z * reach);
        return world.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    public boolean spawnEntity(ItemStack itemStackIn, Level worldIn, Player playerIn, BlockPos blockpos) {
        if (!worldIn.isClientSide) {
            if (playerIn.isShiftKeyDown()) {
                for (int i = -10; i < 10; i += 4) {
                    for (int j = -10; j < 10; j += 4) {
                        Vector3f pos = new Vector3f(blockpos.getX() + i, blockpos.getY(), blockpos.getZ() + j);
                        RagdollEntity entity = getSpawnEntity(worldIn, playerIn, Vector3fPool.get(pos.x, pos.y + 2.3F, pos.z), playerIn.getYRot() % 360.0F, 0);
                        if (entity != null) {
                            worldIn.addFreshEntity(entity);
                        }
                    }
                }
            } else {
                RagdollEntity entity = getSpawnEntity(worldIn, playerIn,
                        Vector3fPool.get(blockpos.getX(), blockpos.getY() + 2.19F, blockpos.getZ()).add(new Vector3f(0.5f, 0, 0.5f)),
                        playerIn.getYRot() % 360.0F, 0);
                if (entity != null) {
                    worldIn.addFreshEntity(entity);
                }
            }
        }
        return true;
    }

    public RagdollEntity getSpawnEntity(Level worldIn, Player playerIn, Vector3f pos, float spawnRotation, int metadata) {
        return new RagdollEntity(DynamXEntities.RAGDOLL.get(), worldIn, pos, spawnRotation, playerIn.getName().getString());
    }
}
