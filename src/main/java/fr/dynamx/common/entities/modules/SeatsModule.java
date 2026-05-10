package fr.dynamx.common.entities.modules;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.client.camera.CameraMode;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.parts.BasePartSeat;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.network.sync.MessageSeatsSync;
import fr.dynamx.common.network.sync.PhysicsEntitySynchronizer;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static fr.dynamx.common.DynamXMain.log;

/**
 * Tracks who's sitting in which seat, dispatches mount/dismount events.
 */
// TODO port:1.20.1 - MessageSeatsSync/PhysicsEntitySynchronizer/CameraMode/ClientEventHandler are forward
// references (Phase 5/7). MinecraftForge.EVENT_BUS -> NeoForge.EVENT_BUS; Side -> LogicalSide for the API
// listener calls; MathHelper -> Mth; EntityPlayer -> Player. positionRider/onPassengerTurned forwards from
// ModularPhysicsEntity invoke updatePassenger/applyOrientationToEntity here via reflection.
public class SeatsModule implements IPhysicsModule<AbstractEntityPhysicsHandler<?, ?>> {
    protected final PackPhysicsEntity<?, ? extends IPartContainer<?>> entity;
    protected BiMap<BasePartSeat, Entity> seatToPassenger = HashBiMap.create();
    protected Map<Byte, Boolean> doorsStatus;
    @Getter
    protected final CameraMode preferredCameraMode;
    @Getter
    private BasePartSeat lastRiddenSeat;

    public SeatsModule(PackPhysicsEntity<?, ? extends IPartContainer<?>> entity) {
        this(entity, CameraMode.AUTO);
    }

    public SeatsModule(PackPhysicsEntity<?, ? extends IPartContainer<?>> entity, CameraMode preferredCameraMode) {
        this.entity = entity;
        this.preferredCameraMode = preferredCameraMode;
    }

    @Override
    public void initEntityProperties() {
        for (BasePartSeat s : (List<BasePartSeat>) entity.getPackInfo().getPartsByType(BasePartSeat.class)) {
            if (s.hasDoor()) {
                if (doorsStatus == null)
                    doorsStatus = new HashMap<>();
                doorsStatus.put(s.getId(), false);
            }
        }
    }

    public boolean isEntitySitting(Entity entity) {
        return seatToPassenger.containsValue(entity);
    }

    public BasePartSeat getRidingSeat(Entity entity) {
        return seatToPassenger.inverse().get(entity);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isLocalPlayerDriving() {
        BasePartSeat seat = getRidingSeat(Minecraft.getInstance().player);
        return seat != null && seat.isDriver();
    }

    public BiMap<BasePartSeat, Entity> getSeatToPassengerMap() {
        return seatToPassenger;
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        seatToPassenger.forEach((s, p) -> tag.putString("Seat" + s.getId(), p.getUUID().toString()));
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        for (BasePartSeat seat : (List<BasePartSeat>) entity.getPackInfo().getPartsByType(BasePartSeat.class)) {
            if (tag.contains("Seat" + seat.getId(), Tag.TAG_STRING)) {
                Player player = entity.getServer().getPlayerList().getPlayer(UUID.fromString(tag.getString("Seat" + seat.getId())));
                if (player != null) {
                    seatToPassenger.put(seat, player);
                }
            }
        }
    }

    @Nullable
    public Entity getControllingPassenger() {
        return seatToPassenger.entrySet().stream().filter(e -> e.getKey().isDriver()).findFirst().map(Map.Entry::getValue).orElse(null);
    }

    public void updatePassenger(Entity passenger) {
        BasePartSeat seat = getRidingSeat(passenger);
        if (seat == null) {
            return;
        }
        com.jme3.math.Vector3f posVec;
        fr.dynamx.utils.optimization.Vector3fPool.openPool();
        try {
            posVec = fr.dynamx.utils.maths.DynamXGeometry.rotateVectorByQuaternion(seat.getPosition(), entity.renderRotation);
            passenger.setPos(entity.getX() + posVec.x, entity.getY() + posVec.y, entity.getZ() + posVec.z);
        } finally {
            fr.dynamx.utils.optimization.Vector3fPool.closePool();
        }

        // make player's yaw follow the entity yaw
        float deltaRotation = entity.getYRot() - entity.yRotO;
        passenger.setYRot(passenger.getYRot() + deltaRotation);
        passenger.setYHeadRot(passenger.getYHeadRot() + deltaRotation);
        applyOrientationToEntity(passenger);
    }

    /**
     * Rotates the passenger, limiting his field of view to avoid stiff necks
     */
    public void applyOrientationToEntity(Entity passenger) {
        passenger.setYBodyRot(0);
        BasePartSeat<?, ?> seat = getRidingSeat(passenger);
        if (seat != null && seat.shouldLimitFieldOfView()) {
            // Limit yaw
            float f = Mth.wrapDegrees(passenger.getYRot() - entity.getYRot());
            float f1 = Mth.clamp(f, seat.getMinYaw(), seat.getMaxYaw());
            passenger.yRotO += f1 - f;
            passenger.setYRot(passenger.getYRot() + f1 - f);

            // Limit pitch
            float f2 = Mth.wrapDegrees(passenger.getXRot());
            float f3 = Mth.clamp(f2, seat.getMinPitch(), seat.getMaxPitch());
            passenger.setXRot(f3);
            f2 = Mth.wrapDegrees(passenger.xRotO);
            f3 = Mth.clamp(f2, seat.getMinPitch(), seat.getMaxPitch());
            passenger.xRotO = f3;
        }
        passenger.setYHeadRot(passenger.getYRot() - entity.getYRot());
    }

    @Override
    public void addPassenger(Entity passenger) {
        if (entity.level().isClientSide) {
            return;
        }
        BasePartSeat hitPart = seatToPassenger.inverse().get(passenger);
        if (hitPart != null) {
            if (hitPart.isDriver() && passenger instanceof Player) {
                if (DynamXContext.usesPhysicsWorld(entity.level())) { //Fix: in single player, server has no physics world
                    DynamXContext.getPhysicsWorld(entity.level()).schedule(() -> entity.getSynchronizer().onPlayerStartControlling((Player) passenger, true));
                } else {
                    entity.getSynchronizer().onPlayerStartControlling((Player) passenger, true);
                }
            }
            NeoForge.EVENT_BUS.post(new VehicleEntityEvent.EntityMount(LogicalSide.SERVER, passenger, entity, this, hitPart));
            DynamXContext.getNetwork().sendToClient(new MessageSeatsSync((IModuleContainer.ISeatsContainer) entity), EnumPacketTarget.ALL_TRACKING_ENTITY, entity);
        } else {
            log.error("Cannot add passenger : " + passenger + " : seat not found !");
        }
        //Client side is managed by updateSeats
    }

    @Override
    public void removePassenger(Entity passenger) {
        BasePartSeat seat = getRidingSeat(passenger);
        if (entity.level().isClientSide || seat == null) {
            return;
        }
        lastRiddenSeat = seat;
        seatToPassenger.remove(seat);
        if (seat.isDriver() && passenger instanceof Player) {
            if (DynamXContext.usesPhysicsWorld(entity.level())) { //Fix: in single player, server has no physics world
                DynamXContext.getPhysicsWorld(entity.level()).schedule(() -> entity.getSynchronizer().onPlayerStopControlling((Player) passenger, true));
            } else {
                entity.getSynchronizer().onPlayerStopControlling((Player) passenger, true);
            }
        }
        DynamXContext.getNetwork().sendToClient(new MessageSeatsSync((IModuleContainer.ISeatsContainer) entity), EnumPacketTarget.ALL_TRACKING_ENTITY, entity);
        NeoForge.EVENT_BUS.post(new VehicleEntityEvent.EntityDismount(entity.level().isClientSide ? LogicalSide.CLIENT : LogicalSide.SERVER, passenger, entity, this, seat));
        //Client side is managed by updateSeats
    }

    /**
     * Receives seat synchronisation data from server <br>
     * Should be called in physics thread
     */
    public void updateSeats(MessageSeatsSync msg, PhysicsEntitySynchronizer<?> netHandler) {
        List<BasePartSeat> remove = new ArrayList<>(0);
        //Search for players who dismounted the entity
        for (Map.Entry<BasePartSeat, Entity> seatEntry : seatToPassenger.entrySet()) {
            if (msg.getSeatToEntity().containsValue(seatEntry.getValue().getId())) {
                continue;
            }
            remove.add(seatEntry.getKey());
            if (seatEntry.getKey().isDriver() && seatEntry.getValue() instanceof Player) {
                netHandler.onPlayerStopControlling((Player) seatEntry.getValue(), true);
            }
            ClientEventHandler.MC.tell(() -> NeoForge.EVENT_BUS.post(new VehicleEntityEvent.EntityDismount(LogicalSide.CLIENT, seatEntry.getValue(), entity, this, seatEntry.getKey())));
        }
        //And remove them
        if (!remove.isEmpty())
            remove.forEach(seatToPassenger::remove);
        //Search for players who mounted on the entity
        if (entity.getPackInfo() == null) //The seats may be sync before the entity's vehicle info is initialized
        {
            PackPhysicsEntity notGeneric = entity;
            notGeneric.setPackInfo(notGeneric.createInfo(entity.getInfoName()));
            if (entity.getPackInfo() == null)
                log.fatal("Failed to find info " + entity.getInfoName() + " for modular entity seats sync. Entity : " + entity);
        }
        for (Map.Entry<Byte, Integer> e : msg.getSeatToEntity().entrySet()) {
            BasePartSeat seat = entity.getPackInfo().getPartByTypeAndId(BasePartSeat.class, e.getKey());
            if (seat != null) {
                Entity passengerEntity = entity.level().getEntity(e.getValue());
                if (passengerEntity != null) {
                    if (seatToPassenger.get(seat) != passengerEntity) { //And add them
                        seatToPassenger.put(seat, passengerEntity);
                        if (seat.isDriver() && passengerEntity instanceof Player) {
                            netHandler.onPlayerStartControlling((Player) passengerEntity, true);
                        }
                        ClientEventHandler.MC.tell(() -> NeoForge.EVENT_BUS.post(new VehicleEntityEvent.EntityMount(LogicalSide.CLIENT, passengerEntity, entity, this, seat)));
                    }
                } else {
                    log.warn("Entity with id " + e.getValue() + " not found for seat in " + entity);
                    log.warn("Details " + msg.getSeatToEntity() + " " + entity.getPassengers());
                    log.warn("Players there " + entity.level().players());
                    log.warn("THE player id " + DynamXMain.proxy.getClientWorld());
                }
            } else {
                log.warn("Seat with id " + e.getKey() + " not found in " + entity);
            }
        }
    }
}
