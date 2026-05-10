package fr.dynamx.common.items.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.items.ItemModularEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * TODO port:1.20.1 - {@code CarEntity} / {@code BaseVehicleEntity} are Phase 6 ports; {@link #getSpawnEntity}
 *  return type relaxed to {@link Object} until then.
 */
public class ItemCar extends ItemModularEntity {
    public ItemCar(ModularVehicleInfo info) {
        super(info);
    }

    @Override
    public Object getSpawnEntity(Level worldIn, Player playerIn, Vector3f pos, float spawnRotation, int metadata) {
        // TODO port:1.20.1 - CarEntity not yet ported (Phase 6); returning null stub.
        // return new CarEntity<>(getInfo().getFullName(), worldIn, pos, spawnRotation, metadata);
        return null;
    }

    public static ItemCar getItemForCar(ModularVehicleInfo info) {
        return new ItemCar(info);
    }
}
