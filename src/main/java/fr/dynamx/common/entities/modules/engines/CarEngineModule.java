package fr.dynamx.common.entities.modules.engines;

import fr.dynamx.api.audio.EnumSoundState;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.client.handlers.hud.CarController;
import fr.dynamx.client.sound.ReversingSound;
import fr.dynamx.common.contentpack.type.vehicle.CarEngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.CarInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.EnginePhysicsHandler;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

import static fr.dynamx.client.ClientProxy.SOUND_HANDLER;

/**
 * Car-specific engine: physics gearbox + reversing/handbrake sounds.
 */
// TODO port:1.20.1 - CarController/ReversingSound forward references (Phase 7).
@SynchronizedEntityVariable.SynchronizedPhysicsModule(modid = DynamXConstants.ID)
public class CarEngineModule extends BasicEngineModule implements IPackInfoReloadListener {
    @Getter
    protected CarEngineInfo engineInfo;
    @Getter
    protected EnginePhysicsHandler physicsHandler;

    protected ReversingSound reversingSound;

    @SynchronizedEntityVariable(name = "speed_limit")
    private final EntityVariable<Float> speedLimit = new EntityVariable<>(SynchronizationRules.CONTROLS_TO_SPECTATORS, Float.MAX_VALUE);

    public CarEngineModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity, CarEngineInfo engineInfo) {
        super(entity);
        this.engineInfo = engineInfo;
    }

    @Override
    public void onPackInfosReloaded() {
        this.engineInfo = entity.getPackInfo().getSubPropertyByType(CarEngineInfo.class);
        if (physicsHandler != null)
            physicsHandler.onPackInfosReloaded();
        super.onPackInfosReloaded();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public IVehicleController createNewController() {
        return new CarController(entity, this);
    }

    @Override
    public void initPhysicsEntity(@Nullable BaseVehiclePhysicsHandler<?> handler) {
        if (handler != null) {
            physicsHandler = new EnginePhysicsHandler(this, handler, handler.getWheels());
        }
    }

    @Override
    public void preUpdatePhysics(boolean simulatePhysics) {
        if (simulatePhysics) {
            physicsHandler.update();
        }
    }

    @Override
    public void postUpdatePhysics(boolean simulatingPhysics) {
        super.postUpdatePhysics(simulatingPhysics);
        if (simulatingPhysics) {
            this.getEngineProperties()[VehicleEntityProperties.EnumEngineProperties.REVS.ordinal()] = physicsHandler.getEngine().getRevs();
            this.getEngineProperties()[VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR.ordinal()] = physicsHandler.getGearBox().getActiveGearNum();
        }
    }

    public float getRealSpeedLimit() {
        return speedLimit.get() == Float.MAX_VALUE ? entity.getPackInfo().getVehicleMaxSpeed() : speedLimit.get();
    }

    public float getSpeedLimit() {
        return speedLimit.get();
    }

    public void setSpeedLimit(float speedLimit) {
        this.speedLimit.set(speedLimit);
    }

    @Override
    public void setControls(int controls) {
        if (entity.level().isClientSide && entity.tickCount > 60 && entity.getPackInfo() instanceof CarInfo) {
            if (!this.isHandBraking() && (controls & 32) == 32)
                playHandbrakeSound(true);
            else if (this.isHandBraking() && (controls & 32) != 32)
                playHandbrakeSound(false);
        }
        super.setControls(controls);
    }

    @Override
    public void updateSounds() {
        super.updateSounds();
        if (isReversing() && getEngineProperty(VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR) == -1) {
            playReversingSound();
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void playHandbrakeSound(boolean on) {
        // Handbrake/reversing sounds are CarInfo-only properties; the pack may be a plain
        // ModularVehicleInfo (no CarInfo section), so guard the cast to avoid a ClassCastException.
        if (!(entity.getPackInfo() instanceof CarInfo carInfo))
            return;
        String sound = on ? carInfo.getHandbrakeSoundOn() : carInfo.getHandbrakeSoundOff();
        if (sound != null)
            SOUND_HANDLER.playSingleSound(entity.physicsPosition, sound, 1, 1);
    }

    @OnlyIn(Dist.CLIENT)
    protected void playReversingSound() {
        if (getEngineInfo() == null || !(entity.getPackInfo() instanceof CarInfo carInfo))
            return;
        String sound = carInfo.getReversingSound();
        if (sound == null)
            return;
        boolean forInterior = Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON && (entity.hasPassenger(Minecraft.getInstance().player) || entity.getVehicle() == Minecraft.getInstance().player);
        if (reversingSound != null && reversingSound.getState() == EnumSoundState.PLAYING) {
            if (forInterior == reversingSound.isInterior())
                return;
            SOUND_HANDLER.stopSound(reversingSound);
        }
        reversingSound = new ReversingSound(sound, entity, this, forInterior);
        SOUND_HANDLER.playStreamingSound(Vector3fPool.get(reversingSound.getPosX(), reversingSound.getPosY(), reversingSound.getPosZ()), reversingSound);
    }
}
