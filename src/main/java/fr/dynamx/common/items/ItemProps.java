package fr.dynamx.common.items;

import com.jme3.math.Vector3f;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * TODO port:1.20.1 - Prop spawner item:
 *  - {@code setCreativeTab(...)} removed; creative tab via events.
 *  - {@code setHasSubtypes(true)} / {@code setMaxDamage(0)} removed.
 *  - {@code playerIn.capabilities.isCreativeMode} -> {@link Player#getAbilities()}{@code .instabuild}.
 *  - {@code playerIn.isSneaking()} -> {@link Player#isShiftKeyDown()}.
 *  - {@code playerIn.rotationYaw} -> {@link Player#getYRot()}.
 *  - {@code World#spawnEntity} -> {@link Level#addFreshEntity(net.minecraft.world.entity.Entity)}.
 *  - {@code PropsEntity} is Phase 6 (not yet ported); {@link #getSpawnEntity(Level, Player, Vector3f, float, int)}
 *    returns {@link Object} until then.
 *  - {@code getSubItems} removed.
 *  - {@code ItemStack#getMetadata()} removed; placeholder 0.
 */
public class ItemProps<T extends PropObject<T>> extends DynamXItemSpawner<T> {

    protected final int textureNum;

    public ItemProps(T itemInfo) {
        super(itemInfo);
        // TODO port:1.20.1 - setCreativeTab removed; CreativeModeTab membership flows through events.

        textureNum = itemInfo.getMaxVariantId();
        // TODO port:1.20.1 - setHasSubtypes / setMaxDamage removed; variants live in data-components.
    }

    @Override
    public boolean spawnEntity(ItemStack itemStackIn, Level worldIn, Player playerIn, Vec3 blockPos) {
        Vector3f pos;
        if (!worldIn.isClientSide) {
            if (playerIn.isShiftKeyDown()) {
                if (!playerIn.getAbilities().instabuild)
                    return true;
                for (float i = 0; i < 5; i += 1) {
                    for (float j = 0; j < 5; j += 1) {
                        for (float k = 0; k < 5; k += 1) {
                            pos = new Vector3f((float) blockPos.x + i, (float) blockPos.y + 4f + (j), (float) blockPos.z + k);
                            // TODO port:1.20.1 - PropsEntity not yet ported (Phase 6); spawn skipped if entity is null.
                            Object entity = getSpawnEntity(worldIn, playerIn, pos, playerIn.getYRot() % 360.0F, 0);
                            if (entity instanceof net.minecraft.world.entity.Entity)
                                worldIn.addFreshEntity((net.minecraft.world.entity.Entity) entity);
                        }
                    }
                }
            } else {
                pos = new Vector3f((float) blockPos.x + getInfo().getSpawnOffset().x, (float) blockPos.y + getInfo().getSpawnOffset().y, (float) blockPos.z + getInfo().getSpawnOffset().z);
                Object entity = getSpawnEntity(worldIn, playerIn, pos, playerIn.getYRot() % 360.0F, 0);
                if (entity instanceof net.minecraft.world.entity.Entity)
                    worldIn.addFreshEntity((net.minecraft.world.entity.Entity) entity);
            }
        }
        return true;
    }

    @Override
    public Object getSpawnEntity(Level worldIn, Player playerIn, Vector3f pos, float spawnRotation, int metadata) {
        // TODO port:1.20.1 - PropsEntity not yet ported (Phase 6); returning null stub.
        return null;
        // return new PropsEntity<>(getInfo().getFullName(), worldIn, pos, spawnRotation, metadata);
    }

    // TODO port:1.20.1 - getSubItems removed; populate creative tabs via CreativeModeTabRegistryEvent.

    @Override
    public String getDescriptionId(ItemStack stack) {
        // TODO port:1.20.1 - stack.getMetadata() removed; variant byte 0 placeholder.
        return super.getDescriptionId(stack);
    }

    @Override
    public int getMaxMeta() {
        return textureNum;
    }
}
