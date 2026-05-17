// TODO port:1.20.1 stub - minimal NodeModel surface used by GltfModelRenderer/DxAnimation.
// Full implementation lives in src/legacy/java/de/javagl/jgltf/dynamx/model/NodeModel.java.
package de.javagl.jgltf.dynamx.model;

public interface NodeModel {

    String getName();

    float[] getTranslation();

    float[] getRotation();

    float[] getScale();

    float[] getWeights();
}
