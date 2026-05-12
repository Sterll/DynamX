package fr.dynamx.common.core;

import fr.dynamx.common.entities.PropsEntity;
import fr.dynamx.common.entities.RagdollEntity;
import fr.dynamx.common.entities.SeatEntity;
import fr.dynamx.common.entities.vehicles.BoatEntity;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.common.entities.vehicles.DoorEntity;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.common.entities.vehicles.TrailerEntity;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DynamXEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DynamXConstants.ID);

    public static final RegistryObject<EntityType<CarEntity<?>>> CAR =
            ENTITIES.register("car", () -> build(CarEntity::new, "car", 3.0F, 2.5F));
    public static final RegistryObject<EntityType<BoatEntity<?>>> BOAT =
            ENTITIES.register("boat", () -> build(BoatEntity::new, "boat", 3.0F, 2.0F));
    public static final RegistryObject<EntityType<TrailerEntity<?>>> TRAILER =
            ENTITIES.register("trailer", () -> build(TrailerEntity::new, "trailer", 3.0F, 2.5F));
    public static final RegistryObject<EntityType<HelicopterEntity<?>>> HELICOPTER =
            ENTITIES.register("helicopter", () -> build(HelicopterEntity::new, "helicopter", 4.0F, 4.0F));
    public static final RegistryObject<EntityType<PropsEntity<?>>> PROP =
            ENTITIES.register("prop", () -> build(PropsEntity::new, "prop", 2.0F, 2.0F));
    public static final RegistryObject<EntityType<DoorEntity<?>>> DOOR =
            ENTITIES.register("door", () -> build(DoorEntity::new, "door", 1.5F, 1.5F));
    public static final RegistryObject<EntityType<RagdollEntity>> RAGDOLL =
            ENTITIES.register("ragdoll", () -> build(RagdollEntity::new, "ragdoll", 0.6F, 1.8F));
    public static final RegistryObject<EntityType<SeatEntity>> SEAT =
            ENTITIES.register("seat", () -> build(SeatEntity::new, "seat", 0.1F, 0.1F));

    private DynamXEntities() {
    }

    private static <T extends Entity> EntityType<T> build(EntityType.EntityFactory<T> factory, String id, float width, float height) {
        return EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(width, height)
                .clientTrackingRange(10)
                .updateInterval(3)
                .setShouldReceiveVelocityUpdates(true)
                .build(DynamXConstants.ID + ":" + id);
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}
