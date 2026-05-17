package fr.dynamx.client.renders.animations;

import com.modularmods.mcgltf.dynamx.animation.InterpolatedChannel;
import fr.dynamx.client.renders.model.renderer.GltfModelRenderer;
import fr.dynamx.utils.optimization.QuaternionPool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Glb/gltf animation playback wrapper.
 * <p>
 * TODO port:1.20.1 - {@link InterpolatedChannel} is stubbed: its {@code update(timeS)} returns null
 * and there is no world-time hook (was {@code Animation.getWorldTime} on the legacy Forge client).
 * The timer / queue plumbing still runs so callers can drive the animator without NPEs; once the
 * GLTF runtime is ported, restore the per-channel transform application in
 * {@link #playAnimation(GltfModelRenderer, DxAnimator, float)}.
 */
@RequiredArgsConstructor
@ToString
public class DxAnimation {

    @Getter
    private final String name;
    @ToString.Exclude
    private final List<InterpolatedChannel> channels;
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
    InterpolatedChannel.TransformType update = null;

    public void playAnimation(GltfModelRenderer modelRenderer, DxAnimator animator, float partialTicks) {
        // Fast-path: nothing to drive. Mark the animation as ended and drain the queue
        // so callers do not get stuck waiting for a never-completing animation.
        if (channels == null || channels.isEmpty()) {
            if (animator != null) {
                animator.isAnimationPlaying = false;
                animator.getAnimationQueue().poll();
            }
            isPlaying = false;
            hasEnded = true;
            return;
        }

        QuaternionPool.openPool();
        try {
            // TODO port:1.20.1 - worldTime was Animation.getWorldTime(world, partialTicks); the
            // jgltf-dynamx port has no equivalent yet, so loops driven on world time stay frozen.
            float worldTime = 0f;
            float tmpDeltaTime = 0f;

            if (allTimerEnder) {
                for (InterpolatedChannel channel : channels) {
                    if (channel != null && channel.timer != null) {
                        channel.timer.timerEnded = false;
                        channel.timer.shouldPlayFinalTransition = false;
                        channel.timer.currentTime = 0;
                    }
                }
                if (animator != null) {
                    animator.isAnimationPlaying = false;
                    animator.getAnimationQueue().poll();
                }
                isPlaying = false;
                hasEnded = true;
                return;
            }

            for (InterpolatedChannel channel : channels) {
                if (channel == null) continue;
                Timer timer = channel.timer;
                if (timer == null) continue;

                float endTime = getEndTime(channel) + (finalTime / 20);
                if (timer.timerEnded) {
                    allTimerEnder = true;
                    continue;
                }
                allTimerEnder = false;
                isPlaying = true;
                timer.updateTimer(animator, this, endTime, tmpDeltaTime);

                if (shouldLoop) {
                    channel.update(endTime > 0 ? worldTime % endTime : 0f);
                    continue;
                }

                if (!timer.shouldPlayFinalTransition) {
                    update = channel.update(Math.min(timer.currentTime, endTime - (finalTime / 20)));
                }
                if (animator == null) continue;
                DxAnimator.EnumBlendPose blendPose = animator.getBlendPose();
                boolean blendActive = blendPose == DxAnimator.EnumBlendPose.START
                        || blendPose == DxAnimator.EnumBlendPose.END
                        || blendPose == DxAnimator.EnumBlendPose.START_END;
                if (!blendActive) continue;

                if (!timer.shouldPlayFinalTransition
                        && (animType == EnumAnimType.START || animType == EnumAnimType.START_END)) {
                    if (update != null) {
                        modelRenderer.blendInitialPose(channel.nodeModel, update.copiedValues, update.type,
                                timer.currentTime / 100);
                    }
                }
                if (timer.shouldPlayFinalTransition
                        && (animType == EnumAnimType.END || animType == EnumAnimType.START_END)) {
                    GltfModelRenderer.Transform transform = modelRenderer.initialNodeTransforms.get(channel.nodeModel);
                    if (transform != null) {
                        float delta = (timer.currentTime - (endTime - (finalTime / 20))) / (finalTime / 20);
                        modelRenderer.resetNodeModel(channel.nodeModel, transform, delta);
                    }
                }
            }
        } finally {
            QuaternionPool.closePool();
        }
    }

    public void resetAnimation() {
        if (channels == null) return;
        for (InterpolatedChannel channel : channels) {
            if (channel != null) channel.update(0);
        }
    }

    public void resetModel(GltfModelRenderer gltfModelRenderer, float partialTicks) {
        gltfModelRenderer.resetModel(partialTicks);
    }

    public float getStartTime(InterpolatedChannel channel) {
        if (channel == null) return 0f;
        float[] keys = channel.getKeys();
        return keys != null && keys.length > 0 ? keys[0] : 0f;
    }

    public float getEndTime(InterpolatedChannel channel) {
        if (channel == null) return 0f;
        float[] keys = channel.getKeys();
        return keys != null && keys.length > 0 ? keys[keys.length - 1] : 0f;
    }

    public float getTotalEndTime() {
        if (channels == null) return 0f;
        float total = 0f;
        for (InterpolatedChannel channel : channels) {
            total += getEndTime(channel);
        }
        return total;
    }

    public float getDuration(InterpolatedChannel channel) {
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
