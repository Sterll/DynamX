package fr.dynamx.api.events;

import fr.dynamx.api.contentpack.object.part.InteractivePart;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Vehicle entity events.
 *
 * TODO port:1.20.1 - PackPhysicsEntity / BaseVehicleEntity / SeatsModule / WheelsModule /
 *   BasicEngineModule / BasePartSeat / PartWheelInfo / IVehicleController / VehicleHud /
 *   CarController all live in fr.dynamx.common.entities / fr.dynamx.client.* (Phases 5/6/7).
 *   They are typed as Object here to keep the API surface compilable.
 */
public class VehicleEntityEvent extends Event {
    @Getter
    private final Dist side;

    @Getter
    private final Object entity; // TODO port:1.20.1 - PackPhysicsEntity<?, ?>

    public VehicleEntityEvent(Dist side, Object vehicleEntity) {
        this.entity = vehicleEntity;
        this.side = side;
    }

    /**
     * Called on server side when a player entity interacts with a vehicle
     */
    public static class PlayerInteract extends VehicleEntityEvent implements ICancellableEvent {
        @Getter
        private final Player player;
        @Nullable
        @Getter
        private final InteractivePart<?, ?> part;
        @Getter
        private final InteractionType interactionType;

        public PlayerInteract(Player player, Object vehicleEntity, @Nullable InteractivePart<?, ?> part) {
            super(Dist.DEDICATED_SERVER, vehicleEntity);
            this.player = player;
            this.part = part;
            this.interactionType = part == null ? InteractionType.VEHICLE : InteractionType.PART;
        }

        public boolean withPart() {
            return interactionType == InteractionType.PART;
        }

        public boolean withVehicle() {
            return interactionType == InteractionType.VEHICLE;
        }

        public enum InteractionType {
            VEHICLE, PART
        }
    }

    /**
     * Called on client and server sides when an entity has mounted on a vehicle.
     */
    public static class EntityMount extends VehicleEntityEvent {
        @Getter
        private final Entity entityMounted;
        @Getter
        private final Object module; // TODO port:1.20.1 - SeatsModule
        @Getter
        private final Object seat;   // TODO port:1.20.1 - BasePartSeat

        public EntityMount(Dist side, Entity entityMounted, Object vehicleEntity, Object module, Object seat) {
            super(side, vehicleEntity);
            this.entityMounted = entityMounted;
            this.module = module;
            this.seat = seat;
        }
    }

    /**
     * Called on client and server sides when an entity has dismounted a vehicle.
     */
    public static class EntityDismount extends VehicleEntityEvent {
        @Getter
        private final Entity entityDismounted;
        @Getter
        private final Object module; // TODO port:1.20.1 - SeatsModule
        @Getter
        private final Object seat;   // TODO port:1.20.1 - BasePartSeat

        public EntityDismount(Dist side, Entity entityDismounted, Object vehicleEntity, Object module, Object seat) {
            super(side, vehicleEntity);
            this.entityDismounted = entityDismounted;
            this.module = module;
            this.seat = seat;
        }
    }

    /**
     * Fired when loading a vehicle from NBT.
     *
     * TODO port:1.20.1 - Originally inspected vehicleEntity.world.isRemote to pick a Side;
     *   in 1.20.1 use entity.level().isClientSide. Defaults to CLIENT pending the entity port.
     */
    public static class LoadFromNBT extends VehicleEntityEvent {
        @Getter
        private final CompoundTag nbtTagCompound;

        public LoadFromNBT(CompoundTag nbtTagCompound, Object vehicleEntity) {
            super(Dist.CLIENT, vehicleEntity);
            this.nbtTagCompound = nbtTagCompound;
        }
    }

    /**
     * Fired when saving a vehicle to NBT.
     */
    public static class SaveToNBT extends VehicleEntityEvent {
        @Getter
        private final CompoundTag nbtTagCompound;

        public SaveToNBT(CompoundTag nbtTagCompound, Object vehicleEntity) {
            super(Dist.CLIENT, vehicleEntity);
            this.nbtTagCompound = nbtTagCompound;
        }
    }

    /**
     * Fired when creating a vehicle HUD.
     */
    public static class CreateHud extends VehicleEntityEvent implements ICancellableEvent {
        @Getter
        private final Object vehicleHud; // TODO port:1.20.1 - VehicleHud
        @Getter
        private final List<ResourceLocation> styleSheets;
        @Getter
        private final boolean isPlayerDriving;
        @Getter
        private final List<Object> controllers; // TODO port:1.20.1 - List<IVehicleController>

        public CreateHud(Object vehicleHUD, List<ResourceLocation> styleSheets, boolean isPlayerDriving, Object vehicleEntity, List<Object> controllers) {
            super(Dist.CLIENT, vehicleEntity);
            this.vehicleHud = vehicleHUD;
            this.styleSheets = styleSheets;
            this.isPlayerDriving = isPlayerDriving;
            this.controllers = controllers;
        }
    }

    /**
     * Called on client side when the engine sounds of the entity are updated.
     */
    public static class UpdateSounds extends VehicleEntityEvent implements ICancellableEvent {
        @Getter
        private final EventPhase eventPhase;
        @Getter
        private final Object module; // TODO port:1.20.1 - BasicEngineModule

        public UpdateSounds(Object vehicleEntity, Object module, EventPhase phase) {
            super(Dist.CLIENT, vehicleEntity);
            this.eventPhase = phase;
            this.module = module;
        }
    }

    /**
     * Called when a vehicle's wheel is changed.
     */
    public static class ChangeWheel extends VehicleEntityEvent implements ICancellableEvent {
        @Getter
        private final byte wheelPartId;
        @Getter
        private final Object oldWheel;       // TODO port:1.20.1 - PartWheelInfo
        @Getter
        @Setter
        private Object newWheel;             // TODO port:1.20.1 - PartWheelInfo
        @Getter
        private final Object wheelsModule;   // TODO port:1.20.1 - WheelsModule

        public ChangeWheel(Dist side, Object vehicleEntity, Object wheelsModule, Object oldWheel, Object newWheel, byte wheelPartId) {
            super(side, vehicleEntity);
            this.wheelsModule = wheelsModule;
            this.wheelPartId = wheelPartId;
            this.oldWheel = oldWheel;
            this.newWheel = newWheel;
        }
    }

    /**
     * Called on CarController post update.
     */
    public static class ControllerUpdate<T> extends VehicleEntityEvent {
        @Getter
        private final T controller;

        public ControllerUpdate(Object vehicleEntity, T controller) {
            super(Dist.CLIENT, vehicleEntity);
            this.controller = controller;
        }
    }
}
