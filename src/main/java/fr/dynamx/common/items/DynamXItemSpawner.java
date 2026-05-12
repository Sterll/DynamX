package fr.dynamx.common.items;

import com.jme3.math.Vector3f;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * TODO port:1.20.1 - Spawner item base class:
 *  - {@code onItemRightClick(World, EntityPlayer, EnumHand)} -> {@link #use(Level, Player, InteractionHand)}
 *    returning {@link InteractionResultHolder}.
 *  - {@code ActionResult<ItemStack>} -> {@link InteractionResultHolder} of {@link ItemStack}.
 *  - {@code RayTraceResult} -> {@code HitResult}; {@code Type.BLOCK / Type.ENTITY} now live on {@code HitResult.Type}.
 *  - {@code player.capabilities.isCreativeMode} -> {@link Player#getAbilities()}{@code .instabuild}.
 *  - {@code player.addStat(StatList.getObjectUseStats(this))} -> {@link Player#awardStat(net.minecraft.stats.Stat)}
 *    with {@code Stats.ITEM_USED.get(this)}.
 *  - {@code World#spawnEntity} -> {@link Level#addFreshEntity(net.minecraft.world.entity.Entity)}.
 *  - {@code fr.dynamx.common.entities.PackPhysicsEntity} is from Phase 6 (not yet ported); the
 *    {@link #getSpawnEntity(Level, Player, Vector3f, float, int)} return type is relaxed to {@link Object} for now.
 *  - {@code PhysicsEntityEvent.Spawn} + {@code MinecraftForge.EVENT_BUS.post(...)} relies on
 *    {@code fr.dynamx.common.entities.PackPhysicsEntity}; event posting is stubbed pending Phase 6.
 *  - {@code DynamXUtils.rayTraceEntitySpawn(...)} not yet present in the ported utils; ray-trace lookup is stubbed.
 */
public abstract class DynamXItemSpawner<T extends AbstractItemObject<T, ?>> extends DynamXItem<T> {
    public DynamXItemSpawner(T itemInfo) {
        super(itemInfo);
    }

    public DynamXItemSpawner(String modid, String itemName, ResourceLocation model) {
        super(modid, itemName, model);
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level worldIn, Player playerIn, @Nonnull InteractionHand hand) {
        ItemStack itemstack = playerIn.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.fail(itemstack);
        }
        HitResult raytraceresult = DynamXUtils.rayTraceEntitySpawn(worldIn, playerIn, hand);
        if (raytraceresult == null || raytraceresult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(itemstack);
        }
        BlockPos blockpos;
        Vec3 hitVec = raytraceresult.getLocation();
        if (raytraceresult instanceof EntityHitResult entityHit) {
            blockpos = entityHit.getEntity().blockPosition();
        } else if (raytraceresult instanceof BlockHitResult blockHit) {
            blockpos = blockHit.getBlockPos();
        } else {
            return InteractionResultHolder.pass(itemstack);
        }
        if (worldIn.getBlockState(blockpos).getBlock() == Blocks.SNOW) {
            blockpos = blockpos.below();
        }
        if (!spawnEntity(itemstack, worldIn, playerIn, hitVec)) {
            return InteractionResultHolder.fail(itemstack);
        }
        if (!playerIn.getAbilities().instabuild) {
            itemstack.shrink(1);
        }
        playerIn.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.success(itemstack);
    }

    public boolean spawnEntity(ItemStack itemStackIn, Level worldIn, Player playerIn, Vec3 blockPos) {
        if (!worldIn.isClientSide) {
            // TODO port:1.20.1 - PhysicsEntityEvent.Spawn / PackPhysicsEntity are Phase 6 dependencies.
            //  Once ported:
            //   Object entity = getSpawnEntity(worldIn, playerIn, Vector3fPool.get(...), playerIn.getYRot() % 360.0F, 0);
            //   if (!MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.Spawn(worldIn, entity, playerIn, this, blockPos))) {
            //       worldIn.addFreshEntity((net.minecraft.world.entity.Entity) entity);
            //   }
            Object entity = getSpawnEntity(worldIn, playerIn,
                    Vector3fPool.get((float) blockPos.x, (float) blockPos.y + 1F, (float) blockPos.z),
                    playerIn.getYRot() % 360.0F, 0); // TODO port:1.20.1 - itemStack metadata is byte 0 placeholder.
            if (entity instanceof net.minecraft.world.entity.Entity) {
                worldIn.addFreshEntity((net.minecraft.world.entity.Entity) entity);
            }
        }
        return true;
    }

    /**
     * TODO port:1.20.1 - Return type was {@code PackPhysicsEntity<?, ?>}; relaxed to {@link Object} until Phase 6
     *  (common/entities) is ported.
     */
    public abstract Object getSpawnEntity(Level worldIn, @Nullable Player playerIn, Vector3f pos, float spawnRotation, int metadata);
}
