package fr.dynamx.api.events;

import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.client.gui.VehicleHud;
import fr.dynamx.client.handlers.hud.CarController;
import fr.dynamx.common.contentpack.parts.BasePartSeat;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.entities.modules.engines.BasicEngineModule;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Vehicle entity events.
 */
public class VehicleEntityEvent extends Event {
    @Getter
    private final Dist side;

    @Getter
    private final PackPhysicsEntity<?, ?> entity;

    public VehicleEntityEvent(Dist side, PackPhysicsEntity<?, ?> vehicleEntity) {
        this.entity = vehicleEntity;
        this.side = side;
    }

    // TODO port:1.20.1 - LogicalSide variant kept for legacy call-sites; mapped to Dist.
    public VehicleEntityEvent(LogicalSide side, PackPhysicsEntity<?, ?> vehicleEntity) {
        this(side == LogicalSide.CLIENT ? Dist.CLIENT : Dist.DEDICATED_SERVER, vehicleEntity);
    }

    /**
     * Called on server side when a player entity interacts with a vehicle
     */
    @Cancelable
    public static class PlayerInteract extends VehicleEntityEvent {
        @Getter
        private final Player player;
        @Nullable
        @Getter
        private final InteractivePart<?, ?> part;
        @Getter
        private final InteractionType interactionType;

        public PlayerInteract(Player player, PackPhysicsEntity<?, ?> vehicleEntity, @Nullable InteractivePart<?, ?> part) {
            super(Dist.DEDICATED_SERVER, vehicleEntity);
            this.player = player;
            this.part = part;
            this.interactionType = part == null ? InteractionType.VEHICLE : InteractionType.PART;
        }

        // TODO port:1.20.1 - Object overload for callers that still hand a raw entity object.
        public PlayerInteract(Player player, Object vehicleEntity, @Nullable InteractivePart<?, ?> part) {
            this(player, (PackPhysicsEntity<?, ?>) vehicleEntity, part);
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
        private final SeatsModule module;
        @Getter
        private final BasePartSeat seat;

        public EntityMount(Dist side, Entity entityMounted, PackPhysicsEntity<?, ?> vehicleEntity, SeatsModule module, BasePartSeat seat) {
            super(side, vehicleEntity);
            this.entityMounted = entityMounted;
            this.module = module;
            this.seat = seat;
        }

        // TODO port:1.20.1 - LogicalSide variant kept for callers using EffectiveSide / SeatsModule path.
        public EntityMount(LogicalSide side, Entity entityMounted, PackPhysicsEntity<?, ?> vehicleEntity, SeatsModule module, BasePartSeat seat) {
            this(side == LogicalSide.CLIENT ? Dist.CLIENT : Dist.DEDICATED_SERVER, entityMounted, vehicleEntity, module, seat);
        }
    }

    /**
     * Called on client and server sides when an entity has dismounted a vehicle.
     */
    public static class EntityDismount extends VehicleEntityEvent {
        @Getter
        private final Entity entityDismounted;
        @Getter
        private final SeatsModule module;
        @Getter
        private final BasePartSeat seat;

        public EntityDismount(Dist side, Entity entityDismounted, PackPhysicsEntity<?, ?> vehicleEntity, SeatsModule module, BasePartSeat seat) {
            super(side, vehicleEntity);
            this.entityDismounted = entityDismounted;
            this.module = module;
            this.seat = seat;
        }

        public EntityDismount(LogicalSide side, Entity entityDismounted, PackPhysicsEntity<?, ?> vehicleEntity, SeatsModule module, BasePartSeat seat) {
            this(side == LogicalSide.CLIENT ? Dist.CLIENT : Dist.DEDICATED_SERVER, entityDismounted, vehicleEntity, module, seat);
        }
    }

    /**
     * Fired when loading a vehicle from NBT.
     */
    public static class LoadFromNBT extends VehicleEntityEvent {
        @Getter
        private final CompoundTag nbtTagCompound;

        public LoadFromNBT(CompoundTag nbtTagCompound, BaseVehicleEntity<?> vehicleEntity) {
            super(vehicleEntity.level().isClientSide ? Dist.CLIENT : Dist.DEDICATED_SERVER, vehicleEntity);
            this.nbtTagCompound = nbtTagCompound;
        }
    }

    /**
     * Fired when saving a vehicle to NBT.
     */
    public static class SaveToNBT extends VehicleEntityEvent {
        @Getter
        private final CompoundTag nbtTagCompound;

        public SaveToNBT(CompoundTag nbtTagCompound, BaseVehicleEntity<?> vehicleEntity) {
            super(vehicleEntity.level().isClientSide ? Dist.CLIENT : Dist.DEDICATED_SERVER, vehicleEntity);
            this.nbtTagCompound = nbtTagCompound;
        }
    }

    /**
     * Fired when creating a vehicle HUD (it's a gui displayed as an HUD). <br>
     * Cancelling the event will remove DynamX components from the HUD.
     */
    @Cancelable
    public static class CreateHud extends VehicleEntityEvent {
        @Getter
        private final VehicleHud vehicleHud;
        @Getter
        private final List<ResourceLocation> styleSheets;
        @Getter
        private final boolean isPlayerDriving;
        @Getter
        private final List<IVehicleController> controllers;

        public CreateHud(VehicleHud vehicleHUD, List<ResourceLocation> styleSheets, boolean isPlayerDriving, PackPhysicsEntity<?, ?> vehicleEntity, List<IVehicleController> controllers) {
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
    @Cancelable
    public static class UpdateSounds extends VehicleEntityEvent {
        @Getter
        private final EventPhase eventPhase;
        @Getter
        private final BasicEngineModule module;

        public UpdateSounds(BaseVehicleEntity<?> vehicleEntity, BasicEngineModule module, EventPhase phase) {
            super(Dist.CLIENT, vehicleEntity);
            this.eventPhase = phase;
            this.module = module;
        }
    }

    /**
     * Called when a vehicle's wheel is changed.
     */
    @Cancelable
    public static class ChangeWheel extends VehicleEntityEvent {
        @Getter
        private final byte wheelPartId;
        @Getter
        private final PartWheelInfo oldWheel;
        @Getter
        @Setter
        private PartWheelInfo newWheel;
        @Getter
        private final WheelsModule wheelsModule;

        public ChangeWheel(Dist side, BaseVehicleEntity<?> vehicleEntity, WheelsModule wheelsModule, PartWheelInfo oldWheel, PartWheelInfo newWheel, byte wheelPartId) {
            super(side, vehicleEntity);
            this.wheelsModule = wheelsModule;
            this.wheelPartId = wheelPartId;
            this.oldWheel = oldWheel;
            this.newWheel = newWheel;
        }

        // TODO port:1.20.1 - LogicalSide variant kept for callers using EffectiveSide.get().
        public ChangeWheel(LogicalSide side, BaseVehicleEntity<?> vehicleEntity, WheelsModule wheelsModule, PartWheelInfo oldWheel, PartWheelInfo newWheel, byte wheelPartId) {
            this(side == LogicalSide.CLIENT ? Dist.CLIENT : Dist.DEDICATED_SERVER, vehicleEntity, wheelsModule, oldWheel, newWheel, wheelPartId);
        }
    }

    /**
     * Called on {@link CarController} post update.
     */
    public static class ControllerUpdate<T extends IVehicleController> extends VehicleEntityEvent {
        @Getter
        private final T controller;

        public ControllerUpdate(BaseVehicleEntity<?> vehicleEntity, T controller) {
            super(Dist.CLIENT, vehicleEntity);
            this.controller = controller;
        }
    }
}
