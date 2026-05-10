package fr.dynamx.api.contentpack.registry;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import lombok.Getter;

/**
 * Sub-info type registries, one per high-level pack object kind.
 *
 * TODO port:1.20.1 - The real registries reference DynamXObjectLoaders + concrete pack
 *   objects (PackInfo, ModularVehicleInfo, ItemObject, ArmorObject, BlockObject,
 *   PropObject, PartWheelInfo, CarEngineInfo, BaseEngineInfo) which all live in
 *   fr.dynamx.common.contentpack (Phase 3b). For now we keep the enum constants so
 *   annotations referring to them still compile, but the InfoList / infoOwnerType
 *   fields are stubbed with Object.
 */
public enum SubInfoTypeRegistries {
    PACKS,
    WHEELED_VEHICLES,
    TRAILERS,
    BOATS,
    HELICOPTER,
    ITEMS,
    ARMORS,
    BLOCKS,
    PROPS,
    WHEELS,
    CAR_ENGINES,
    HELICOPTER_ENGINES;

    /**
     * @return The info list for this registry. Stubbed (null) until Phase 3b.
     */
    @Getter
    private final Object infoList = null;

    /**
     * @return The class of the {@link ISubInfoTypeOwner} owning the sub info types of this registry. Stubbed (null) until Phase 3b.
     */
    @Getter
    private final Class<? extends ISubInfoTypeOwner<?>> infoOwnerType = null;
}
