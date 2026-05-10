// TODO port:1.20.1 OBJ loader stub - GltfModelData
package fr.dynamx.common.objloader.data;

import fr.dynamx.api.dxmodel.DxModelPath;

public class GltfModelData extends DxModelData {
    public GltfModelData(DxModelPath modelPath) {
        super(modelPath);
    }

    /**
     * @return placeholder NodeModel; the real type from de.javagl.jgltf.model is unavailable here.
     */
    public Object getNodeModel(String objectName) {
        // TODO port:1.20.1 stub - implement once gltf loader is ported
        return null;
    }
}
