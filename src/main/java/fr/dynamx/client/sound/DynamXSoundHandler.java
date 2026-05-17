package fr.dynamx.client.sound;

import com.jme3.math.Vector3f;
import fr.dynamx.api.audio.IDynamXSound;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.optimization.SubClassPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.sound.SoundEngineLoadEvent;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all DynamX sounds (engines, skid, reversing...).
 * Port 1.20.1: uses {@link SoundManager} and a custom {@link AbstractTickableSoundInstance}
 * wrapper instead of the legacy paulscode SoundSystem reflection.
 */
@OnlyIn(Dist.CLIENT)
public class DynamXSoundHandler {

    public static final int ATTENUATION_NONE = 0;
    public static final int ATTENUATION_ROLLOFF = 1;
    public static final int ATTENUATION_LINEAR = 2;

    private final List<IDynamXSound> playingSounds = new ArrayList<>();
    private final List<IDynamXSound> stoppingSounds = new ArrayList<>();
    private final Map<IDynamXSound, DynamXTickableSoundInstance> instances = new IdentityHashMap<>();

    public void setup(SoundEngineLoadEvent event) {
        // Rien a faire au setup en 1.20.1, le SoundManager est accessible via Minecraft.getInstance().
    }

    public void load(SoundEngineLoadEvent event) {
        for (DynamXTickableSoundInstance inst : instances.values()) {
            inst.markStopped();
        }
        instances.clear();
        playingSounds.clear();
        stoppingSounds.clear();
    }

    public void unload() {
        SoundManager mgr = getSoundManager();
        if (mgr != null) {
            for (DynamXTickableSoundInstance inst : instances.values()) {
                inst.markStopped();
                mgr.stop(inst);
            }
        }
        instances.clear();
        playingSounds.clear();
        stoppingSounds.clear();
    }

    public void tick() {
        if (!ready()) return;
        Vector3fPool.openPool(SubClassPool.DX_SOUND_HANDLER);
        for (IDynamXSound sound : playingSounds) {
            sound.update(this);
        }
        int maxSounds = DynamXConfig.getMaxSounds();
        if (maxSounds > 0 && playingSounds.size() > maxSounds) {
            playingSounds.sort((o1, o2) -> Float.compare(o1.getDistanceToPlayer(), o2.getDistanceToPlayer()));
            for (int i = maxSounds; i < playingSounds.size(); i++) {
                setSoundVolume(playingSounds.get(i), 0);
                playingSounds.get(i).onMuted();
            }
        }
        Vector3fPool.closePool();
        if (!stoppingSounds.isEmpty()) {
            for (IDynamXSound sound : stoppingSounds) {
                DynamXTickableSoundInstance inst = instances.remove(sound);
                if (inst != null) {
                    inst.markStopped();
                }
                playingSounds.remove(sound);
            }
            stoppingSounds.clear();
        }
    }

    public List<IDynamXSound> getPlayingSounds() {
        return playingSounds;
    }

    public void playSingleSound(Vector3f soundPosition, String soundName, float volume, float pitch) {
        playSingleSound(soundPosition, soundName, volume, pitch, ATTENUATION_LINEAR, 48);
    }

    public void playSingleSound(Vector3f soundPosition, String soundName, float volume, float pitch, int attenuationType, float distOrRoll) {
        if (!ready() || DynamXConfig.getMasterSoundVolume() <= 0) return;
        SoundManager mgr = getSoundManager();
        if (mgr == null) return;
        ResourceLocation loc = parseSoundLocation(soundName);
        float effVolume = Mth.clamp(volume * DynamXConfig.getMasterSoundVolume(), 0.0F, 1.0F);
        SimpleSoundInstance instance = new SimpleSoundInstance(
                loc,
                SoundSource.NEUTRAL,
                effVolume,
                pitch,
                RandomSource.create(),
                false,
                0,
                mapAttenuation(attenuationType),
                soundPosition.x,
                soundPosition.y,
                soundPosition.z,
                false);
        mgr.play(instance);
    }

    public void playStreamingSound(Vector3f soundPosition, IDynamXSound sound) {
        playStreamingSound(soundPosition, sound, ATTENUATION_LINEAR, 48);
    }

    public void playStreamingSound(Vector3f soundPosition, IDynamXSound sound, int attenuationType, float distOrRoll) {
        if (!ready() || DynamXConfig.getMasterSoundVolume() <= 0) return;
        if (playingSounds.contains(sound))
            throw new IllegalStateException("Sound " + sound + " is already playing !");
        SoundManager mgr = getSoundManager();
        if (mgr == null || Minecraft.getInstance().isPaused()) return;

        String soundID = sound.getSoundUniqueName();
        String soundName = soundID.substring(soundID.indexOf('_') + 1);
        ResourceLocation loc = parseSoundLocation(soundName);

        DynamXTickableSoundInstance inst = new DynamXTickableSoundInstance(loc, mapAttenuation(attenuationType), soundPosition);
        inst.setVolume(Mth.clamp(sound.getVolume() * DynamXConfig.getMasterSoundVolume(), 0.0F, 1.0F));
        instances.put(sound, inst);
        playingSounds.add(sound);
        mgr.play(inst);
        sound.onStarted();
    }

    public void stopSound(IDynamXSound sound) {
        if (!playingSounds.contains(sound)) return;
        if (!sound.tryStop()) return;
        SoundManager mgr = getSoundManager();
        DynamXTickableSoundInstance inst = instances.get(sound);
        if (inst != null) {
            inst.markStopped();
            if (mgr != null) mgr.stop(inst);
        }
        stoppingSounds.add(sound);
    }

    public void setSoundVolume(IDynamXSound sound, float volume) {
        DynamXTickableSoundInstance inst = instances.get(sound);
        if (inst != null) {
            inst.setVolume(Mth.clamp(volume * DynamXConfig.getMasterSoundVolume(), 0.0F, 1.0F));
        }
    }

    public void setPitch(IDynamXSound sound, float pitch) {
        DynamXTickableSoundInstance inst = instances.get(sound);
        if (inst != null) {
            inst.setPitch(pitch);
        }
    }

    public void setAttenuationType(IDynamXSound sound, int attenuationType) {
        DynamXTickableSoundInstance inst = instances.get(sound);
        if (inst != null) {
            inst.setAttenuation(mapAttenuation(attenuationType));
        }
    }

    public void setSoundDistance(IDynamXSound sound, float radius) {
        // En 1.20.1 la distance est derivee du volume + attenuation, pas de setter direct. Stub volontaire.
    }

    public void setPosition(IDynamXSound sound, float x, float y, float z) {
        DynamXTickableSoundInstance inst = instances.get(sound);
        if (inst != null) {
            inst.setPosition(x, y, z);
        }
    }

    public void pause(IDynamXSound sound) {
        DynamXTickableSoundInstance inst = instances.get(sound);
        if (inst != null) {
            inst.setPaused(true);
        }
    }

    public void resume(IDynamXSound sound) {
        DynamXTickableSoundInstance inst = instances.get(sound);
        if (inst != null) {
            inst.setPaused(false);
        }
    }

    public void setMasterVolume(float masterVolume) {
        DynamXConfig.setMasterSoundVolume(masterVolume);
        for (IDynamXSound sound : playingSounds) {
            setSoundVolume(sound, sound.getVolume());
        }
    }

    public float getMasterVolume() {
        return DynamXConfig.getMasterSoundVolume();
    }

    public SoundManager getMcSoundManager() {
        return getSoundManager();
    }

    private SoundManager getSoundManager() {
        Minecraft mc = Minecraft.getInstance();
        return mc == null ? null : mc.getSoundManager();
    }

    private boolean ready() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.player != null && mc.level != null && mc.level.isClientSide();
    }

    private static ResourceLocation parseSoundLocation(String soundName) {
        int sep = soundName.indexOf(':');
        if (sep > 0) {
            return new ResourceLocation(soundName.substring(0, sep), soundName.substring(sep + 1));
        }
        return new ResourceLocation(DynamXConstants.ID, soundName);
    }

    private static SoundInstance.Attenuation mapAttenuation(int attenuationType) {
        return attenuationType == ATTENUATION_NONE ? SoundInstance.Attenuation.NONE : SoundInstance.Attenuation.LINEAR;
    }

    /**
     * Instance de son tickable qui expose des setters mutables pour volume, pitch, position,
     * attenuation et pause. Necessaire car {@link AbstractTickableSoundInstance} ne fournit
     * pas de setters publics.
     */
    public static class DynamXTickableSoundInstance extends AbstractTickableSoundInstance {
        private boolean externalStop;
        private boolean paused;
        private float savedVolume;

        public DynamXTickableSoundInstance(ResourceLocation loc, SoundInstance.Attenuation attenuation, Vector3f position) {
            super(SoundEvent.createVariableRangeEvent(loc), SoundSource.NEUTRAL, RandomSource.create());
            this.looping = true;
            this.delay = 0;
            this.attenuation = attenuation;
            this.relative = false;
            this.volume = 1.0F;
            this.pitch = 1.0F;
            this.x = position.x;
            this.y = position.y;
            this.z = position.z;
        }

        public void setVolume(float volume) {
            if (paused) {
                this.savedVolume = volume;
            } else {
                this.volume = volume;
            }
        }

        public void setPitch(float pitch) {
            this.pitch = pitch;
        }

        public void setPosition(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void setAttenuation(SoundInstance.Attenuation attenuation) {
            this.attenuation = attenuation;
        }

        public void setPaused(boolean paused) {
            if (paused == this.paused) return;
            this.paused = paused;
            if (paused) {
                this.savedVolume = this.volume;
                this.volume = 0.0F;
            } else {
                this.volume = this.savedVolume;
            }
        }

        public void markStopped() {
            this.externalStop = true;
            stop();
        }

        @Override
        public void tick() {
            if (externalStop && !isStopped()) {
                stop();
            }
        }
    }
}
