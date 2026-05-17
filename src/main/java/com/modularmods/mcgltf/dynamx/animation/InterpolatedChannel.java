// TODO port:1.20.1 stub - InterpolatedChannel placeholder.
// Mirrors the legacy abstract base shape so DxAnimation can iterate keys + timers
// without depending on the full mcgltf runtime port.
package com.modularmods.mcgltf.dynamx.animation;

import de.javagl.jgltf.dynamx.model.NodeModel;
import fr.dynamx.client.renders.animations.DxAnimation;
import fr.dynamx.client.renders.model.renderer.GltfModelRenderer;

public abstract class InterpolatedChannel {

    protected final float[] timesS;
    public NodeModel nodeModel;
    public DxAnimation.Timer timer = new DxAnimation.Timer();

    protected InterpolatedChannel() {
        this(new float[]{0f}, null);
    }

    protected InterpolatedChannel(float[] timesS, NodeModel nodeModel) {
        this.timesS = timesS != null ? timesS : new float[]{0f};
        this.nodeModel = nodeModel;
    }

    public float[] getKeys() {
        return timesS;
    }

    public TransformType update(float timeS) {
        return null;
    }

    public static class TransformType {
        public float[] copiedValues;
        public float[] initialValues;
        public GltfModelRenderer.EnumTransformType type;

        public TransformType(float[] copiedValues, float[] initialValues, GltfModelRenderer.EnumTransformType type) {
            this.copiedValues = copiedValues;
            this.initialValues = initialValues != null ? initialValues.clone() : null;
            this.type = type;
        }
    }
}
