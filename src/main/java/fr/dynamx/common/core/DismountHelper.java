package fr.dynamx.common.core;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Teleports the player to a valid position near the door when dismounting from a vehicle.
 *
 * TODO port:1.20.1 - Body stubbed. Depends on:
 *  - fr.dynamx.api.entities.IModuleContainer (Phase 6 - entities)
 *  - fr.dynamx.common.contentpack.parts.BasePartSeat (Phase 3b - contentpack)
 *  - many 1.12 World/Entity methods that have been renamed in 1.20.1
 *    (collidesWithAnyBlock, getEntityBoundingBox, posX/Y/Z, etc.)
 *  - net.minecraft.block.material.Material was removed in 1.20 (use FluidState)
 * Will be re-implemented once entities + contentpack are ported.
 */
public class DismountHelper {
    public static void preDismount(LivingEntity dismounter, Entity entityIn) {
        // TODO port:1.20.1 - re-implement once dependencies are ported
    }
}
