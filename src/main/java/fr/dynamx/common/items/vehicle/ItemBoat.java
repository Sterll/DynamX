package fr.dynamx.common.items.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.items.ItemModularEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * TODO port:1.20.1 - {@code BoatEntity} / {@code BaseVehicleEntity} are Phase 6 ports; the {@link #getSpawnEntity}
 *  return type is relaxed to {@link Object} until then.
 */
public class ItemBoat extends ItemModularEntity {
    public ItemBoat(ModularVehicleInfo modularVehicleInfo) {
        super(modularVehicleInfo);
    }

    @Override
    public Object getSpawnEntity(Level worldIn, Player playerIn, Vector3f pos, float spawnRotation, int metadata) {
        return new fr.dynamx.common.entities.vehicles.BoatEntity<>(
                fr.dynamx.common.core.DynamXEntities.BOAT.get(),
                getInfo().getFullName(), worldIn, pos.subtractLocal(0, 1, 0), spawnRotation, metadata);
    }
}
