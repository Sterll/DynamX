package fr.dynamx.common.items.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.items.ItemModularEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * TODO port:1.20.1 - {@code TrailerEntity} / {@code BaseVehicleEntity} are Phase 6 ports;
 *  {@link #getSpawnEntity} return type relaxed to {@link Object} until then.
 */
public class ItemTrailer extends ItemModularEntity {
    public ItemTrailer(ModularVehicleInfo modularVehicleInfo) {
        super(modularVehicleInfo);
    }

    @Override
    public Object getSpawnEntity(Level worldIn, Player playerIn, Vector3f pos, float spawnRotation, int metadata) {
        // TODO port:1.20.1 - TrailerEntity not yet ported (Phase 6); returning null stub.
        // return new TrailerEntity<>(getInfo().getFullName(), worldIn, pos, spawnRotation, metadata);
        return null;
    }
}
