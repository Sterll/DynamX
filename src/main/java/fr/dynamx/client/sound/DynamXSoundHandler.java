package fr.dynamx.client.sound;

import com.jme3.math.Vector3f;
import fr.dynamx.api.audio.IDynamXSound;
import fr.dynamx.utils.DynamXConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all DynamX sounds (engines, skid, reversing...).
 *
 * <p>TODO port:1.20.1 - HEAVY rewrite required:</p>
 * <ul>
 *   <li>The 1.12 implementation drove sound playback through Forge-exposed {@code paulscode.sound.SoundSystem}
 *       and the {@code SoundManager.sndSystem} private field, reached via {@code ObfuscationReflectionHelper}.
 *       Both {@code paulscode.sound.*} and the {@code field_148620_e} reflection are gone in 1.20.1.</li>
 *   <li>{@code SoundSetupEvent} no longer exists; sound system setup goes through Forge's
 *       {@code SoundEngineLoadEvent} (after {@code SoundEngine#load}). {@code SoundLoadEvent} stays.</li>
 *   <li>The custom URL+OGG streaming bypassing {@code sounds.json} is no longer supported - instead, register
 *       a {@code SimpleSoundInstance} / {@code AbstractTickableSoundInstance} and submit it to
 *       {@code Minecraft.getInstance().getSoundManager()}.</li>
 *   <li>{@code SoundSystemConfig.ATTENUATION_LINEAR/_ROLLOFF} -> {@code SoundInstance.Attenuation.LINEAR/NONE}.</li>
 *   <li>{@code MathHelper.clamp} -> {@code Mth.clamp}.</li>
 *   <li>{@code Vec3d} -> {@code Vec3}.</li>
 *   <li>{@code Minecraft.getMinecraft()} -> {@code Minecraft.getInstance()}; {@code world.isRemote} -> {@code level.isClientSide()}.</li>
 *   <li>{@code Minecraft.isGamePaused()} -> {@code Minecraft.isPaused()}.</li>
 * </ul>
 * <p>This stub keeps the public method signatures used by the rest of the codebase
 * ({@code playSingleSound}, {@code playStreamingSound}, {@code stopSound}, {@code setSoundVolume}, etc.)
 * so the rest of the mod compiles, but their bodies are intentionally inert. Phase 10 will rewrite this
 * class against the new SoundEngine.</p>
 */
@OnlyIn(Dist.CLIENT)
public class DynamXSoundHandler {

    /** All currently playing (or paused) sounds. */
    private final List<IDynamXSound> playingSounds = new ArrayList<>();
    /** Sounds waiting to be removed from the playingSounds list. */
    private final List<IDynamXSound> stoppingSounds = new ArrayList<>();

    /** Called on mc sound system setup (was SoundSetupEvent in 1.12). */
    public void setup(/* SoundSetupEvent / SoundEngineLoadEvent */ Object event) {
        // TODO port:1.20.1 - stubbed.
    }

    /** Called on mc sound system load (was SoundLoadEvent). */
    public void load(/* SoundLoadEvent */ Object event) {
        // TODO port:1.20.1 - stubbed.
        playingSounds.clear();
    }

    /** Called on world unload. */
    public void unload() {
        // TODO port:1.20.1 - was: for each sound, mcSoundSystem.stop(uniqueName).
        playingSounds.clear();
    }

    /** Called each tick to update the sounds. */
    public void tick() {
        // TODO port:1.20.1 - body stubbed pending SoundEngine integration.
        if (!stoppingSounds.isEmpty()) {
            playingSounds.removeAll(stoppingSounds);
            stoppingSounds.clear();
        }
    }

    public List<IDynamXSound> getPlayingSounds() {
        return playingSounds;
    }

    /**
     * Plays a single sound. <br>
     * <p>TODO port:1.20.1 - delegate to {@code Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance)}.</p>
     */
    public void playSingleSound(Vector3f soundPosition, String soundName, float volume, float pitch) {
        playSingleSound(soundPosition, soundName, volume, pitch, 0, 48);
    }

    public void playSingleSound(Vector3f soundPosition, String soundName, float volume, float pitch, int attenuationType, float distOrRoll) {
        if (DynamXConfig.getMasterSoundVolume() <= 0) return;
        // TODO port:1.20.1 - stubbed.
    }

    /**
     * Plays a streaming sound, used for engines sound, for example.
     * <p>TODO port:1.20.1 - register an AbstractTickableSoundInstance subclass that wraps the IDynamXSound
     * and routes update() through SoundManager.</p>
     */
    public void playStreamingSound(Vector3f soundPosition, IDynamXSound sound) {
        playStreamingSound(soundPosition, sound, 0, 48);
    }

    public void playStreamingSound(Vector3f soundPosition, IDynamXSound sound, int attenuationType, float distOrRoll) {
        if (DynamXConfig.getMasterSoundVolume() <= 0) return;
        if (playingSounds.contains(sound))
            throw new IllegalStateException("Sound " + sound + " is already playing !");
        // TODO port:1.20.1 - stubbed; just track the sound so getPlayingSounds() reflects callers' state.
        playingSounds.add(sound);
        sound.onStarted();
    }

    public void stopSound(IDynamXSound sound) {
        if (playingSounds.contains(sound)) {
            if (sound.tryStop()) {
                stoppingSounds.add(sound);
            }
        }
    }

    public void setSoundVolume(IDynamXSound sound, float volume) {
        // TODO port:1.20.1 - stubbed; in 1.20 the SoundInstance carries its own volume and is re-evaluated each tick.
    }

    public void setPitch(IDynamXSound sound, float pitch) {
        // TODO port:1.20.1 - stubbed.
    }

    public void setAttenuationType(IDynamXSound sound, int attenuationType) {
        // TODO port:1.20.1 - stubbed.
    }

    public void setSoundDistance(IDynamXSound sound, float radius) {
        // TODO port:1.20.1 - stubbed.
    }

    public void setPosition(IDynamXSound sound, float x, float y, float z) {
        // TODO port:1.20.1 - stubbed.
    }

    public void pause(IDynamXSound sound) {
        // TODO port:1.20.1 - stubbed.
    }

    public void resume(IDynamXSound sound) {
        // TODO port:1.20.1 - stubbed.
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
}
