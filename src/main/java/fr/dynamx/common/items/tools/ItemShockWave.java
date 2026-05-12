package fr.dynamx.common.items.tools;

import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.server.command.CmdShockWave;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class ItemShockWave extends Item {
    public ItemShockWave() {
        super(new Properties());
        DynamXItemRegistry.add(this);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack stack = playerIn.getItemInHand(handIn);
        if (!worldIn.isClientSide) {
            List<PhysicsEntity> entities = worldIn.getEntitiesOfClass(PhysicsEntity.class,
                    playerIn.getBoundingBox().inflate(20));
            entities.forEach(physicsEntity -> DynamXPhysicsHelper.createExplosion(physicsEntity,
                    Vector3fPool.get((float) playerIn.getX(), (float) playerIn.getY(), (float) playerIn.getZ()),
                    CmdShockWave.explosionForce));
        }
        return InteractionResultHolder.sidedSuccess(stack, worldIn.isClientSide);
    }
}
