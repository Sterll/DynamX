package fr.dynamx.client.renders;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

import javax.annotation.Nullable;

/**
 * Thread-local "current render frame" — the active PoseStack, MultiBufferSource and packedLight
 * for the entity / item / block currently being rendered. Populated by render entry points
 * (e.g. {@code RenderPhysicsEntity.render}) and read by model renderers (e.g. {@code ObjModelRenderer})
 * whose legacy signatures don't take a {@code PoseStack}.
 */
public final class RenderFrame {
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

    private RenderFrame() {
    }

    public static void push(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        CURRENT.set(new Frame(poseStack, bufferSource, packedLight));
    }

    public static void clear() {
        CURRENT.remove();
    }

    @Nullable
    public static Frame current() {
        return CURRENT.get();
    }

    public record Frame(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
    }
}
