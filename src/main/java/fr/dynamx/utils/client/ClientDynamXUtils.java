package fr.dynamx.utils.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.network.packets.MessageAttachTrailer;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

/**
 * Some client interpolation utils.
 *
 * <p>TODO port:1.20.1 -
 * <ul>
 *   <li>{@code org.joml.Quaternionf} (lwjgl2 vector) is gone; everything is JOML now.
 *       The legacy {@code GlQuaternionPool} still returns a lwjgl-style class but we no longer
 *       pass it to GlStateManager. Once the pool is refactored to JOML this layer can go.</li>
 *   <li>{@code World} -> {@link Level}, {@code BlockPos.MutableBlockPos.setPos} -> {@code set}.</li>
 *   <li>{@code world.getBlockState(pos).isOpaqueCube()} -> {@code state.isSolidRender(level, pos)};
 *       {@code world.getCombinedLight(pos, ambient)} -> {@code LevelRenderer.getLightColor(level, pos)}
 *       (or {@code level.getBrightness(LightLayer, pos)}).</li>
 * </ul>
 *
 * @see DynamXRenderUtils
 */
public class ClientDynamXUtils {

    private static final FloatBuffer EMPTY_FLOAT_BUFFER = BufferUtils.createFloatBuffer(16);

    public static Quaternionf computeInterpolatedJomlQuaternion(com.jme3.math.Quaternion prevRotation, com.jme3.math.Quaternion rotation, float step) {
        return DynamXUtils.toQuaternion(computeInterpolatedGlQuaternion(prevRotation, rotation, step, false));
    }

    public static Quaternionf computeInterpolatedJomlQuaternion(com.jme3.math.Quaternion prevRotation, com.jme3.math.Quaternion rotation, float step, boolean inverse) {
        return DynamXUtils.toQuaternion(computeInterpolatedGlQuaternion(prevRotation, rotation, step, inverse));
    }

    public static org.joml.Quaternionf computeInterpolatedGlQuaternion(com.jme3.math.Quaternion prevRotation, com.jme3.math.Quaternion rotation, float step) {
        return computeInterpolatedGlQuaternion(prevRotation, rotation, step, false);
    }

    public static org.joml.Quaternionf computeInterpolatedGlQuaternion(com.jme3.math.Quaternion prevRotation, com.jme3.math.Quaternion rotation, float step, boolean inverse) {
        com.jme3.math.Quaternion cache = QuaternionPool.get();
        DynamXMath.slerp(step, prevRotation, rotation, cache);
        if (inverse)
            DynamXGeometry.inverseQuaternion(cache, cache);
        return GlQuaternionPool.get(cache);
    }

    @OnlyIn(Dist.CLIENT)
    public static int getLightNear(Level world, BlockPos pos, int horizontalRadius, int maxHeight) {
        // TODO port:1.20.1 - World.getCombinedLight(pos, ambient) -> LevelRenderer.getLightColor(level, pos)
        // and BlockState.isOpaqueCube() -> isSolidRender(level, pos). Stubbed with a simple sky+block lookup.
        BlockState state = world.getBlockState(pos);
        if (!state.isSolidRender(world, pos)) {
            return packLight(world, pos);
        }
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (int y = 0; y <= maxHeight; y++) {
            for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
                for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                    mut.set(x + pos.getX(), y + pos.getY(), z + pos.getZ());
                    if (!world.getBlockState(mut).isSolidRender(world, mut)) {
                        return packLight(world, mut);
                    }
                }
            }
        }
        return 0;
    }

    private static int packLight(Level world, BlockPos pos) {
        int sky = world.getBrightness(LightLayer.SKY, pos);
        int block = world.getBrightness(LightLayer.BLOCK, pos);
        return (sky << 20) | (block << 4);
    }

    public static void attachTrailer() {
        DynamXContext.getNetwork().sendToServer(new MessageAttachTrailer());
    }

    public static org.joml.Quaternionf inverseGlQuaternion(org.joml.Quaternionf quat, org.joml.Quaternionf result) {
        float norm = quat.length();
        if (norm > 0.0) {
            float invNorm = 1.0f / norm;
            result.set(-quat.getX() * invNorm, -quat.getY() * invNorm, -quat.getZ() * invNorm, quat.getW() * invNorm);
        }
        return result;
    }

    public static FloatBuffer getMatrixBuffer(Matrix4f matrix) {
        EMPTY_FLOAT_BUFFER.clear();
        return matrix.get(EMPTY_FLOAT_BUFFER);
    }

    /**
     * 1.20.1 helper: apply a JOML matrix multiply on a {@link PoseStack}'s current pose.
     */
    @SuppressWarnings("unused")
    public static void multPoseMatrix(PoseStack stack, Matrix4f matrix) {
        stack.last().pose().mul(matrix);
    }
}
