package fr.dynamx.client.renders.mesh.shapes;

import com.jme3.math.FastMath;
import fr.dynamx.client.renders.mesh.GLMesh;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

/**
 * A GL_LINES mesh that renders a crude 3-D arrow.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ArrowMesh extends GLMesh {
    private static final int GL_LINES = 0x0001;

    private static ArrowMesh xArrow;
    private static ArrowMesh yArrow;
    private static ArrowMesh zArrow;

    public ArrowMesh() {
        this(0.2f, 0.46f, 0.11f);
    }

    public ArrowMesh(float arrowLength, float barbAngle, float barbLength) {
        super(GL_LINES, 10);
        Validate.inRange(barbAngle, "barb angle", 0f, FastMath.PI);
        Validate.nonNegative(barbLength, "barb length");

        float z = arrowLength - barbLength * FastMath.cos(barbAngle);
        float xy = barbLength * FastMath.sin(barbAngle);

        super.setPositions(
                0f, 0f, 0f,
                0f, 0f, arrowLength,
                xy, 0f, z,
                0f, 0f, arrowLength,
                -xy, 0f, z,
                0f, 0f, arrowLength,
                0f, xy, z,
                0f, 0f, arrowLength,
                0f, -xy, z,
                0f, 0f, arrowLength);
    }

    public static ArrowMesh getMesh(int axisIndex) {
        switch (axisIndex) {
            case MyVector3f.xAxis:
                xArrow = new ArrowMesh();
                xArrow.rotate(0f, FastMath.HALF_PI, 0f);
                xArrow.makeImmutable();
                return xArrow;

            case MyVector3f.yAxis:
                yArrow = new ArrowMesh();
                yArrow.rotate(FastMath.HALF_PI, 0f, 0f);
                yArrow.makeImmutable();
                return yArrow;

            case MyVector3f.zAxis:
                zArrow = new ArrowMesh();
                zArrow.makeImmutable();
                return zArrow;

            default:
                throw new IllegalArgumentException("axisIndex = " + axisIndex);
        }
    }
}
