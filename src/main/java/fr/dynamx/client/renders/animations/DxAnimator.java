package fr.dynamx.client.renders.animations;

import fr.dynamx.client.renders.model.renderer.GltfModelRenderer;
import fr.dynamx.common.blocks.TEDynamXBlock;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Class that manages animations for a model.
 *
 * @see TEDynamXBlock
 */
public class DxAnimator {
    public static final float TIME_STEP = 1.0f / 60f;

    protected boolean isAnimationPlaying;

    @Getter
    @Setter
    @Accessors(fluent = true)
    private boolean shouldPause;

    /**
     * Map of all the animations of a model (key: animation name, value: list of channels).
     * <p>
     * TODO port:1.20.1 - channel values were {@code List<InterpolatedChannel>} from mcgltf;
     * stored as raw Object lists until the GLTF runtime is ported.
     */
    @Nullable
    @Setter
    public HashMap<String, List<Object>> modelAnimations;

    @Getter
    protected final Queue<DxAnimation> animationQueue = new LinkedList<>();

    @Getter
    @Setter
    private EnumBlendPose blendPose = EnumBlendPose.NONE;

    public void update(GltfModelRenderer modelRenderer, float partialTicks) {
        if (shouldPause) return;

        if (animationQueue.isEmpty()) {
            isAnimationPlaying = false;
            return;
        }

        DxAnimation currentAnimation = animationQueue.peek();
        currentAnimation.playAnimation(modelRenderer, this, partialTicks);
    }

    public DxAnimation addAnimation(String animationName, DxAnimation.EnumAnimType type) {
        if (modelAnimations == null) throw new IllegalStateException("Model animations map is null,"
                + " you should call the fillModelAnimations method before playing any animation");
        List<Object> list = modelAnimations.containsKey(animationName)
                ? modelAnimations.get(animationName) : new ArrayList<>();
        DxAnimation animation = new DxAnimation(animationName, list, type);
        animationQueue.add(animation);
        return animation;
    }

    public boolean isAnyAnimationPlaying() {
        return isAnimationPlaying;
    }

    public void playNextAnimation() {
        animationQueue.poll();
    }

    @Nullable
    public DxAnimation getPlayingAnimation() {
        return animationQueue.peek();
    }

    public enum EnumBlendPose {
        NONE, START, END, START_END
    }
}
