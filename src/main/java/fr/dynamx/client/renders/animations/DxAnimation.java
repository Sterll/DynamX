package fr.dynamx.client.renders.animations;

import fr.dynamx.client.renders.model.renderer.GltfModelRenderer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Glb/gltf animation playback wrapper.
 * <p>
 * TODO port:1.20.1 - the mcgltf/InterpolatedChannel pipeline has not been ported yet;
 * channel updates and world-time queries are stubbed. Once the GLTF runtime is brought back
 * online, restore {@link #playAnimation(GltfModelRenderer, DxAnimator, float)} logic.
 */
@RequiredArgsConstructor
@ToString
public class DxAnimation {

    @Getter
    private final String name;
    @ToString.Exclude
    private final List<Object> channels; // TODO port:1.20.1 - was List<InterpolatedChannel> from mcgltf
    @Getter
    private final EnumAnimType animType;
    @Getter
    private boolean isPlaying;
    @Getter
    private boolean hasEnded;
    @Setter
    @Accessors(fluent = true)
    private boolean shouldLoop;

    public float finalTime = 100;

    boolean allTimerEnder;

    public void playAnimation(GltfModelRenderer modelRenderer, DxAnimator animator, float partialTicks) {
        // TODO port:1.20.1 - reimplement against ported InterpolatedChannel / Animation.getWorldTime
        if (animator != null) {
            animator.isAnimationPlaying = false;
        }
        this.isPlaying = false;
        this.hasEnded = true;
    }

    public void resetAnimation() {
        // TODO port:1.20.1 - reset each InterpolatedChannel to t=0 once mcgltf is ported
    }

    public void resetModel(GltfModelRenderer gltfModelRenderer, float partialTicks) {
        gltfModelRenderer.resetModel(partialTicks);
    }

    public float getStartTime(Object channel) {
        // TODO port:1.20.1 - return channel.getKeys()[0] once InterpolatedChannel is ported
        return 0f;
    }

    public float getEndTime(Object channel) {
        // TODO port:1.20.1 - return channel.getKeys()[length-1] once InterpolatedChannel is ported
        return 0f;
    }

    public float getTotalEndTime() {
        // TODO port:1.20.1 - sum channel end-times once InterpolatedChannel is ported
        return 0f;
    }

    public float getDuration(Object channel) {
        return getEndTime(channel) - getStartTime(channel);
    }

    public enum EnumAnimType {
        NORMAL, LOOP, START, END, START_END
    }

    public static class Timer {
        public float currentTime = 0;
        public boolean timerEnded = false;
        public float endTime = 0;
        public boolean shouldPlayFinalTransition = false;

        public void updateTimer(DxAnimator animator, DxAnimation animation, float endTime, float deltaTime) {
            if (!shouldPlayFinalTransition && currentTime < endTime - (animation.finalTime / 20)) {
                currentTime += deltaTime;
            } else if (currentTime >= endTime - (animation.finalTime / 20)) {
                shouldPlayFinalTransition = true;
            }
            if (shouldPlayFinalTransition && currentTime < endTime) {
                currentTime += deltaTime;
            } else if (currentTime >= endTime) {
                timerEnded = true;
            }
        }
    }
}
