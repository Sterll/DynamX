// TODO port:1.20.1 OBJ loader stub - DxModelData base class
package fr.dynamx.common.objloader.data;

import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.api.dxmodel.EnumDxModelFormats;
import org.joml.Vector3f;

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
}
