package fr.dynamx.client.camera;

import com.jme3.math.Quaternion;

import java.util.function.BiFunction;

/**
 * Vehicle camera modes for {@link CameraSystem}
 *
 * <p>TODO port:1.20.1 - the {@code Integer} argument used to be {@code gameSettings.thirdPersonView}
 * (0/1/2); in 1.20.1 that is {@code Minecraft.getInstance().options.getCameraType().ordinal()}.</p>
 */
public enum CameraMode {
    AUTO((i, q) -> {
        if (i != 0)
            q.set(0, q.getY(), 0, q.getW());
        return null;
    }),
    FIXED((i, q) -> null),
    FREE((i, q) -> {
        q.set(0, 0, 0, 1);
        return null;
    });

    public final BiFunction<Integer, Quaternion, Object> rotator;

    CameraMode(BiFunction<Integer, Quaternion, Object> rotator) {
        this.rotator = rotator;
    }
}
