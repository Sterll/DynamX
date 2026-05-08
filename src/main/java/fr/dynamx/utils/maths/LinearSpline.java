package fr.dynamx.utils.maths;

import com.jme3.math.Vector3f;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class LinearSpline {

    @Getter
    private final List<Vector3f> controlPoints = new ArrayList<>();

    public LinearSpline(List<Vector3f> controlPoints) {
        this.controlPoints.addAll(controlPoints);
    }

    public Vector3f interpolate(float value, int currentControlPoint, Vector3f store) {
        if (store == null) store = new Vector3f();
        DynamXMath.interpolateLinear(value, controlPoints.get(currentControlPoint), controlPoints.get(currentControlPoint + 1), store);
        return store;
    }
}
