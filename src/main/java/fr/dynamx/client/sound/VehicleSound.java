package fr.dynamx.client.sound;

import com.jme3.math.Vector3f;
import fr.dynamx.api.audio.EnumSoundState;
import fr.dynamx.api.audio.IDynamXSound;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public abstract class VehicleSound implements IDynamXSound {
    protected final BaseVehicleEntity<?> vehicleEntity;
    protected final Player player;
    private final String soundName;

    protected Vector3f playerPos;
    protected Vector3f sourcePos;

    private EnumSoundState state = EnumSoundState.STOPPED;

    public VehicleSound(String soundName, BaseVehicleEntity<?> vehicle) {
        this.soundName = soundName;

        this.vehicleEntity = vehicle;
        this.player = Minecraft.getInstance().player;

        this.playerPos = new Vector3f((float) player.getX(), (float) player.getY(), (float) player.getZ());
        this.sourcePos = vehicle.physicsPosition;
    }

    public void setState(EnumSoundState state) {
        this.state = state;
    }

    public EnumSoundState getState() {
        return state;
    }

    @Override
    public void onStarted() {
        setState(EnumSoundState.PLAYING);
    }

    @Override
    public boolean tryStop() {
        setState(EnumSoundState.STOPPED);
        return true;
    }

    @Override
    public void update(DynamXSoundHandler handler) {
        if (isSoundActive() && vehicleEntity.isAlive()) {
            this.playerPos.set((float) player.getX(), (float) player.getY(), (float) player.getZ());
            this.sourcePos.set(vehicleEntity.physicsPosition);

            handler.setSoundVolume(this, getVolume());
            handler.setPitch(this, getPitch());
            Vec3 soundNormalizedPosition = vehicleEntity.position();
            handler.setPosition(this, (float) soundNormalizedPosition.x, (float) soundNormalizedPosition.y, (float) soundNormalizedPosition.z);
            if (Minecraft.getInstance().isPaused()) {
                handler.pause(this);
            } else {
                handler.resume(this);
            }
        } else {
            if (!vehicleEntity.isAlive()) {
                handler.stopSound(this);
            } else if (getState() != EnumSoundState.STOPPED) {
                setState(EnumSoundState.STOPPING);
            }
        }
    }

    public float getPosX() {
        return sourcePos.x;
    }

    public float getPosY() {
        return sourcePos.y;
    }

    public float getPosZ() {
        return sourcePos.z;
    }

    private float volumeFactor = 1;

    public void setVolumeFactor(float factor) {
        volumeFactor = factor;
    }

    public float getVolumeFactor() {
        return volumeFactor;
    }

    @Override
    public float getVolume() {
        if (vehicleEntity.equals(player.getVehicle())) {
            return 1.0F * volumeFactor;
        }
        return getCurrentVolume() * volumeFactor;
    }

    public float getPitch() {
        if (vehicleEntity.equals(player.getVehicle())) {
            return getCurrentPitch();
        } else {
            Vec3 playerMot = player.getDeltaMovement();
            Vec3 vehMot = vehicleEntity.getDeltaMovement();
            Vector3f temp = Vector3fPool.get(playerPos);
            Vector3f temp2 = Vector3fPool.get(sourcePos);
            double soundVelocity = Vector3fPool.get(playerPos).subtractLocal(sourcePos.x, sourcePos.y, sourcePos.z).length() - temp.addLocal((float) playerMot.x, (float) playerMot.y, (float) playerMot.z)
                    .addLocal(temp2.addLocal((float) vehMot.x, (float) vehMot.y, (float) vehMot.z).multLocal(-1)).length();
            return (float) (getCurrentPitch() * (1 + soundVelocity / 10F));
        }
    }

    public String getSoundName() {
        return soundName;
    }

    @Override
    public String getSoundUniqueName() {
        return vehicleEntity.getId() + "_" + getSoundName();
    }

    public abstract boolean isSoundActive();

    protected float getCurrentVolume() {
        return 1.0F;
    }

    protected float getCurrentPitch() {
        return 1.0F;
    }

    @Override
    public float getDistanceToPlayer() {
        return playerPos.distance(sourcePos);
    }

    @Override
    public void onMuted() {

    }
}
