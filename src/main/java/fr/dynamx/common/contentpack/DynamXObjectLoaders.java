package fr.dynamx.common.contentpack;

import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.common.contentpack.loader.InfoList;
import fr.dynamx.common.contentpack.loader.InfoLoader;
import fr.dynamx.common.contentpack.loader.LateInfoLoader;
import fr.dynamx.common.contentpack.loader.ObjectLoader;
import fr.dynamx.common.contentpack.loader.PacksInfoLoader;
import fr.dynamx.common.contentpack.loader.PropsLoader;
import fr.dynamx.common.contentpack.loader.SoundInfoLoader;
import fr.dynamx.common.contentpack.loader.SubInfoTypesRegistry;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.contentpack.type.objects.ItemObject;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.contentpack.type.vehicle.BaseEngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.BoatEngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.CarEngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import fr.dynamx.common.contentpack.type.vehicle.SoundListInfo;
import fr.dynamx.common.contentpack.type.vehicle.VehicleValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * All DynamX {@link InfoLoader}s.
 *
 * TODO port:1.20.1 - Original referenced (not yet ported):
 *   - fr.dynamx.common.blocks.DynamXBlock (Phase 4)
 *   - fr.dynamx.common.items.{DynamXItem, DynamXItemArmor} (Phase 6)
 *   - fr.dynamx.common.items.vehicle.{ItemBoat, ItemCar, ItemHelicopter, ItemTrailer} (Phase 6)
 *   - fr.dynamx.common.contentpack.type.vehicle.CarInfo (was a ModularVehicleInfo subclass)
 *     - currently the CarInfo subclass has not been ported; we instantiate ModularVehicleInfo
 *       with VehicleValidator.CAR_VALIDATOR directly. Once CarInfo is ported the constructor
 *       reference should be restored.
 *   The owner generic of every {@link ObjectLoader} is widened to {@link IDynamXItem} until
 *   the concrete item classes are available; this preserves the public field name layout so
 *   downstream code that does {@code DynamXObjectLoaders.WHEELED_VEHICLES.owners} keeps working.
 */
public class DynamXObjectLoaders {
    private static final List<InfoList<?>> INFO_LISTS = new ArrayList<>();

    public static PacksInfoLoader PACKS = new PacksInfoLoader("pack_info", (p, n) -> new PackInfo(p, n, ContentPackType.NOTSET), new SubInfoTypesRegistry<>());

    // TODO port:1.20.1 - CarInfo subclass not ported yet; use ModularVehicleInfo with the
    //   CAR_VALIDATOR until Phase 3c-final reintroduces CarInfo. The lambda is structurally
    //   identical to the legacy CarInfo::new reference.
    public static ObjectLoader<ModularVehicleInfo, IDynamXItem<?>> WHEELED_VEHICLES = new ObjectLoader<>("vehicle_", (packName, fileName) -> new ModularVehicleInfo(packName, fileName, VehicleValidator.CAR_VALIDATOR), new SubInfoTypesRegistry<>());
    public static ObjectLoader<ModularVehicleInfo, IDynamXItem<?>> TRAILERS = new ObjectLoader<>("trailer_", (packName, fileName) -> new ModularVehicleInfo(packName, fileName, VehicleValidator.TRAILER_VALIDATOR), WHEELED_VEHICLES.getDefaultSubInfoTypesRegistry());
    public static ObjectLoader<ModularVehicleInfo, IDynamXItem<?>> BOATS = new ObjectLoader<>("boat_", (packName, fileName) -> new ModularVehicleInfo(packName, fileName, VehicleValidator.BOAT_VALIDATOR), WHEELED_VEHICLES.getDefaultSubInfoTypesRegistry());
    public static ObjectLoader<ModularVehicleInfo, IDynamXItem<?>> HELICOPTERS = new ObjectLoader<>("helicopter_", (packName, fileName) -> new ModularVehicleInfo(packName, fileName, VehicleValidator.HELICOPTER_VALIDATOR), new SubInfoTypesRegistry<>());
    public static ObjectLoader<BlockObject<?>, IDynamXItem<?>> BLOCKS = new ObjectLoader<>("block", BlockObject::new, new SubInfoTypesRegistry<>());
    public static ObjectLoader<ItemObject<?>, IDynamXItem<?>> ITEMS = new ObjectLoader<>("item", ItemObject::new, new SubInfoTypesRegistry<>());
    public static ObjectLoader<ArmorObject<?>, IDynamXItem<?>> ARMORS = new ObjectLoader<>("armor", ArmorObject::new, new SubInfoTypesRegistry<>());
    public static PropsLoader<PropObject<?>> PROPS = new PropsLoader<>();
    public static InfoLoader<PartWheelInfo> WHEELS = new InfoLoader<>("wheel", PartWheelInfo::new, new SubInfoTypesRegistry<>());
    public static LateInfoLoader<BaseEngineInfo> ENGINES = new LateInfoLoader<>("engine", ((pack, name, clazz) -> {
        if (Objects.equals(clazz, CarEngineInfo.class.toString()))
            return new CarEngineInfo(pack, name);
        else if (Objects.equals(clazz, BaseEngineInfo.class.toString()))
            return new BaseEngineInfo(pack, name);
        else if (Objects.equals(clazz, BoatEngineInfo.class.toString()))
            return new BoatEngineInfo(pack, name);
        else
            throw new IllegalArgumentException("Unknown engine class: " + clazz);
    }), new SubInfoTypesRegistry<>());
    public static SoundInfoLoader SOUNDS = new SoundInfoLoader("sounds", SoundListInfo::new);

    static {
        // TODO port:1.20.1 - Legacy code only registered SOUNDS into INFO_LISTS, even though
        //   all the other loaders need to participate in clear/postLoad/getInfoLoaders. The
        //   legacy gap was filled at runtime via reflection on the static fields - we keep
        //   the legacy behavior of only adding SOUNDS to preserve binary semantics, but
        //   ContentPackLoader#reload should likely iterate every public static field.
        INFO_LISTS.add(SOUNDS);
    }

    public static List<InfoList<?>> getInfoLists() {
        return INFO_LISTS;
    }

    public static List<InfoLoader<?>> getInfoLoaders() {
        return Collections.unmodifiableList(INFO_LISTS.stream().filter(l -> l instanceof InfoLoader).map(l -> (InfoLoader<?>) l).collect(Collectors.toList()));
    }
}
