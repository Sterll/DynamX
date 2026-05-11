// TODO port:1.20.1 OBJ loader stub - DxModelData base class
package fr.dynamx.common.objloader.data;

import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.EnumDxModelFormats;
import com.jme3.math.Vector3f; // TODO port:1.20.1 - aligned with DynamXUtils' jme3 Vector3f usage.

import java.util.Collections;
import java.util.List;

public abstract class DxModelData {
    protected final DxModelPath modelPath;

    public DxModelData(DxModelPath modelPath) {
        this.modelPath = modelPath;
    }

    public DxModelPath getModelPath() { return modelPath; }

    public EnumDxModelFormats getFormat() {
        return modelPath != null ? modelPath.getFormat() : null;
    }

    public List<String> getMeshNames() {
        // TODO port:1.20.1 stub
        return Collections.emptyList();
    }

    public Vector3f getMeshCenter(String objectName, Vector3f out) {
        // TODO port:1.20.1 stub
        return out;
    }

    public Vector3f getMeshDimension(String objectName, Vector3f out) {
        // TODO port:1.20.1 stub
        return out;
    }

    // TODO port:1.20.1 stub - vertex helpers used by ShapeUtils. Returning empty arrays so the
    // collision-shape generator throws a clear "empty" error rather than NPE.
    public float[] getVerticesPos() {
        return new float[0];
    }

    public float[] getVerticesPos(String objectName) {
        return new float[0];
    }

    public int[] getAllMeshIndices() {
        return new int[0];
    }

    public int[] getMeshIndices(String objectName) {
        return new int[0];
    }
}
