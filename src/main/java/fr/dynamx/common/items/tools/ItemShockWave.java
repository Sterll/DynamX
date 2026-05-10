package fr.dynamx.common.items.tools;

import fr.dynamx.common.items.DynamXItemRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * TODO port:1.20.1 - Shockwave item:
 *  - {@code onItemRightClick(...)} -> {@link #use(Level, Player, InteractionHand)} returning
 *    {@link InteractionResultHolder}.
 *  - {@code World#getEntitiesWithinAABB} -> {@code Level#getEntitiesOfClass(Class, AABB)}.
 *  - {@code player.getEntityBoundingBox()} -> {@code player.getBoundingBox()}.
 *  - {@code AxisAlignedBB#grow} -> {@code AABB#inflate}.
 *  - {@code player.posX/Y/Z} -> {@code player.getX/Y/Z}.
 *  - {@code PhysicsEntity}, {@code DynamXPhysicsHelper.createExplosion}, {@code CmdShockWave.explosionForce} all
 *    live in not-yet-ported packages (Phase 4/5/6); the right-click body is stubbed.
 */
public class ItemShockWave extends Item {
    public ItemShockWave() {
        super(new Properties());
        // TODO port:1.20.1 - setRegistryName / setTranslationKey / setCreativeTab removed; DeferredRegister handles it.
        DynamXItemRegistry.add(this);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        // TODO port:1.20.1 - PhysicsEntity / DynamXPhysicsHelper.createExplosion / CmdShockWave are not yet ported.
        //  Once available:
        //   List<PhysicsEntity> entities = worldIn.getEntitiesOfClass(PhysicsEntity.class, playerIn.getBoundingBox().inflate(20));
        //   entities.forEach(physicsEntity -> DynamXPhysicsHelper.createExplosion(physicsEntity,
        //       Vector3fPool.get((float) playerIn.getX(), (float) playerIn.getY(), (float) playerIn.getZ()),
        //       CmdShockWave.explosionForce));
        return InteractionResultHolder.pass(playerIn.getItemInHand(handIn));
    }
}
