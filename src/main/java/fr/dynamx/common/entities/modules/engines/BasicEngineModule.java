package fr.dynamx.common.entities.modules.engines;

import fr.dynamx.api.audio.EnumSoundState;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.events.EventPhase;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.client.sound.EngineSound;
import fr.dynamx.common.contentpack.type.vehicle.BaseEngineInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.EnginePhysicsHandler;
import fr.dynamx.common.physics.entities.parts.engine.AutomaticGearboxHandler;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static fr.dynamx.client.ClientProxy.SOUND_HANDLER;

/**
 * Basic engine module for vehicles with control flags + RPM-driven sounds.
 *
 * @see VehicleEntityProperties.EnumEngineProperties
 * @see EnginePhysicsHandler
 * @see AutomaticGearboxHandler
 * @see WheelsModule
 */
// TODO port:1.20.1 - EngineSound/SOUND_HANDLER forward references (Phase 7). isRidingOrBeingRiddenBy ->
// hasPassenger(p) || getVehicle() == p. gameSettings.thirdPersonView -> options.getCameraType().
@SynchronizedEntityVariable.SynchronizedPhysicsModule(modid = DynamXConstants.ID)
public abstract class BasicEngineModule implements IPhysicsModule<BaseVehiclePhysicsHandler<?>>, IPhysicsModule.IPhysicsUpdateListener, IPhysicsModule.IEntityUpdateListener, IPackInfoReloadListener {

    protected final BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity;

    @SynchronizedEntityVariable(name = "controls")
    private final EntityVariable<Integer> controls = new EntityVariable<>((var, value) -> {
        setControls(value);
    }, SynchronizationRules.CONTROLS_TO_SPECTATORS, 32);

    @SynchronizedEntityVariable(name = "engine_props")
    private final EntityVariable<float[]> engineProperties = new EntityVariable<>(SynchronizationRules.PHYSICS_TO_SPECTATORS, new float[VehicleEntityProperties.EnumEngineProperties.values().length]);

    protected final Map<Integer, EngineSound> engineSounds = new HashMap<>();
    @Getter
    protected EngineSound currentEngineSound;
    protected EngineSound lastEngineSound;

    public BasicEngineModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity) {
        this.entity = entity;
    }

    public float[] getEngineProperties() {
        return engineProperties.get();
    }

    public float getEngineProperty(VehicleEntityProperties.EnumEngineProperties engineProperty) {
        return engineProperties.get()[engineProperty.ordinal()];
    }

    public boolean isAccelerating() {
        return EnginePhysicsHandler.inTestFullGo || (controls.get() & 2) == 2;
    }

    public boolean isReversing() {
        return (controls.get() & 4) == 4;
    }

    public boolean isTurningLeft() {
        return (controls.get() & 8) == 8;
    }

    public boolean isTurningRight() {
        return (controls.get() & 16) == 16;
    }

    public boolean isHandBraking() {
        return (getControls() & 32) == 32;
    }

    public boolean isEngineStarted() {
        return (EnginePhysicsHandler.inTestFullGo) || ((controls.get() & 1) == 1);
    }

    public void setEngineStarted(boolean started) {
        setControls(started ? getControls() | 1 : getControls() & ~1);
    }

    public int getControls() {
        return controls.get();
    }

    public void setControls(int controls) {
        if (!this.isEngineStarted() && (controls & 1) == 1)
            onEngineSwitchedOn();
        else if (isEngineStarted() && (controls & 1) != 1)
            onEngineSwitchedOff();
        this.controls.set(controls);
    }

    public void onEngineSwitchedOn() {
        if (entity.level().isClientSide && entity.tickCount > 60) {
            playStartingSound();
        }
    }

    public void onEngineSwitchedOff() {
    }

    public void resetControls() {
        setControls(controls.get() & 1 | (controls.get() & 32));
    }

    @Nullable
    @Override
    public abstract IVehicleController createNewController();

    @Override
    public void readFromNBT(CompoundTag tag) {
        if (tag.getBoolean("isEngineStarted"))
            setControls(controls.get() | 1);
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        tag.putBoolean("isEngineStarted", isEngineStarted());
    }

    @Override
    public void postUpdatePhysics(boolean simulatingPhysics) {
        if (simulatingPhysics) {
            this.engineProperties.get()[VehicleEntityProperties.EnumEngineProperties.SPEED.ordinal()] = entity.physicsHandler.getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH);
        }
    }

    @Override
    public void onSetSimulationHolder(SimulationHolder simulationHolder, Player simulationPlayerHolder, SimulationHolder.UpdateContext changeContext) {
        if (simulationPlayerHolder == null) {
            resetControls();
        }
    }

    //Sounds

    @OnlyIn(Dist.CLIENT)
    protected void playStartingSound() {
        boolean forInterior = Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON && (entity.hasPassenger(Minecraft.getInstance().player) || entity.getVehicle() == Minecraft.getInstance().player);
        String sound = getStartingSound(forInterior);
        if (sound != null)
            SOUND_HANDLER.playSingleSound(entity.physicsPosition, sound, 1, 1);
    }

    @OnlyIn(Dist.CLIENT)
    public abstract BaseEngineInfo getEngineInfo();

    @Override
    public boolean listenEntityUpdates(LogicalSide side) {
        return side.isClient();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void updateEntity() {
        if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.UpdateSounds(entity, this, EventPhase.PRE))) {
            if (entity.getPackInfo() != null) {
                updateSounds();
            }
            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.UpdateSounds(entity, this, EventPhase.POST));
        }
    }

    @Override
    public void onPackInfosReloaded() {
        engineSounds.clear();
    }

    @OnlyIn(Dist.CLIENT)
    public String getStartingSound(boolean forInterior) {
        if (getEngineInfo() == null)
            return null;
        return forInterior ? getEngineInfo().startingSoundInterior : getEngineInfo().startingSoundExterior;
    }

    @OnlyIn(Dist.CLIENT)
    public void updateSounds() {
        BaseEngineInfo engineInfo = getEngineInfo();
        if (engineInfo == null || engineInfo.getEngineSounds() == null) {
            return;
        }

        if (engineSounds.isEmpty()) {
            engineInfo.getEngineSounds().forEach(engineSound -> engineSounds.put(engineSound.id, new EngineSound(engineSound, entity, this)));
        }

        if (!isEngineStarted()) {
            if (currentEngineSound != null) {
                SOUND_HANDLER.stopSound(currentEngineSound);
            }
            currentEngineSound = lastEngineSound = null;
            return;
        }

        boolean forInterior = Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON && (entity.hasPassenger(Minecraft.getInstance().player) || entity.getVehicle() == Minecraft.getInstance().player);
        float rpm = getEngineProperty(VehicleEntityProperties.EnumEngineProperties.REVS) * engineInfo.getMaxRevs();
        lastEngineSound = currentEngineSound;
        if (currentEngineSound == null || !currentEngineSound.shouldPlay(rpm, forInterior)) {
            for (EngineSound sound : engineSounds.values()) {
                if (sound.shouldPlay(rpm, forInterior)) {
                    this.currentEngineSound = sound;
                    break;
                }
            }
        }

        if (currentEngineSound == lastEngineSound) {
            return;
        }

        if (lastEngineSound != null) {
            SOUND_HANDLER.stopSound(lastEngineSound);
        }
        if (currentEngineSound != null) {
            if (currentEngineSound.getState() == EnumSoundState.STOPPING) {
                currentEngineSound.onStarted();
            } else {
                SOUND_HANDLER.playStreamingSound(Vector3fPool.get(currentEngineSound.getPosX(), currentEngineSound.getPosY(), currentEngineSound.getPosZ()), currentEngineSound);
            }
        }
    }

    public float getSoundPitch() {
        return getEngineProperty(VehicleEntityProperties.EnumEngineProperties.REVS);
    }
}
