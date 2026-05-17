package fr.dynamx.client.renders.model.renderer;

import com.jme3.math.Quaternion;
import com.modularmods.mcgltf.dynamx.animation.InterpolatedChannel;
import de.javagl.jgltf.dynamx.model.AnimationModel;
import de.javagl.jgltf.dynamx.model.NodeModel;
import fr.dynamx.client.renders.animations.DxAnimation;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.QuaternionPool;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * GLTF model renderer. The "survivor" of the OBJ/GLTF split: the GLTF format is the only
 * supported format going forward.
 * <p>
 * TODO port:1.20.1 - mcgltf (com.modularmods.mcgltf.dynamx.*) and jgltf-dynamx
 * (de.javagl.jgltf.dynamx.*) are reduced to stub classes/interfaces. The runtime
 * receivers, scene rendering and texture variant hooks are no-ops; the blend/reset
 * math is kept and operates on the {@link NodeModel} interface so it stays usable
 * once the GLTF runtime is brought back online.
 * <p>
 * TODO port:1.20.1 - rendering must be performed via PoseStack + MultiBufferSource + RenderType;
 * the legacy renderForVanilla / renderForShaderMod entry points push GL_ALL_ATTRIB_BITS and rely on
 * GlStateManager which is gone in 1.20.1 core profile.
 */
public class GltfModelRenderer extends DxModelRenderer {

    @Getter
    public List<NodeModel> nodeModels;
    public HashMap<String, List<InterpolatedChannel>> animations;
    public List<AnimationModel> animationModels;
    public float time;
    public DxAnimation animation;

    public Map<NodeModel, Transform> initialNodeTransforms = new HashMap<>();

    public GltfModelRenderer(fr.dynamx.api.dxmodel.DxModelPath location, fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier textureVariants) {
        super(location, textureVariants);
        // TODO port:1.20.1 - MCglTF.getInstance().addGltfModelReceiver(this) once the receiver pipeline is ported.
    }

    @Override
    public void renderModel(byte textureDataId, boolean forceVanillaRender) {
        // TODO port:1.20.1 - call scene.renderForVanilla / renderForShaderMod via PoseStack/MultiBufferSource.
        if (isEmpty()) return;
        renderVanillaOrShader(-1, forceVanillaRender);
    }

    @Override
    public boolean renderGroup(String group, byte textureDataId, boolean forceVanillaRender) {
        // TODO port:1.20.1 - render only the matched subtree via mcgltf bridge.
        if (isEmpty()) return false;
        int nodeModelIndex = getNodeModelIndex(group);
        if (nodeModelIndex == -1) return false;
        renderVanillaOrShader(nodeModelIndex, forceVanillaRender);
        return true;
    }

    @Override
    public boolean renderDefaultParts(byte textureDataId, boolean forceVanillaRender) {
        if (isEmpty()) return false;
        boolean drawn = false;
        for (NodeModel object : nodeModels) {
            String name = object != null ? object.getName() : null;
            if (name != null && getTextureVariants() != null && getTextureVariants().canRenderPart(name)) {
                if (renderGroup(name, textureDataId, forceVanillaRender)) {
                    drawn = true;
                }
            }
        }
        return drawn;
    }

    public void renderVanillaOrShader(int nodeModelIndex, boolean forceVanillaRender) {
        // TODO port:1.20.1 - mcgltf RenderedGltfScene rendering must be rewritten on top of the
        // BufferBuilder / RenderType pipeline; until then this is a safe no-op.
    }

    @Override
    public boolean containsObjectOrNode(String name) {
        if (isEmpty() || name == null) return false;
        return nodeModels.stream().anyMatch(o -> o != null && name.equalsIgnoreCase(o.getName()));
    }

    public int getNodeModelIndex(String objectName) {
        if (isEmpty() || objectName == null) return -1;
        return IntStream.range(0, nodeModels.size())
                .filter(i -> {
                    NodeModel n = nodeModels.get(i);
                    return n != null && objectName.equalsIgnoreCase(n.getName());
                })
                .findFirst()
                .orElse(-1);
    }

    @Override
    public boolean isEmpty() {
        return nodeModels == null || nodeModels.isEmpty();
    }

    public void resetModel(float partialTicks) {
        for (Map.Entry<NodeModel, Transform> entry : initialNodeTransforms.entrySet()) {
            resetNodeModel(entry.getKey(), entry.getValue(), partialTicks);
        }
    }

    public void resetNodeModel(NodeModel nodeModel, Transform initialTransform, float partialTicks) {
        if (nodeModel == null || initialTransform == null) return;
        blendInitialPose(nodeModel, initialTransform.translation, EnumTransformType.TRANSLATION, partialTicks);
        blendInitialPose(nodeModel, initialTransform.rotation, EnumTransformType.ROTATION, partialTicks);
        blendInitialPose(nodeModel, initialTransform.scale, EnumTransformType.SCALE, partialTicks);
        blendInitialPose(nodeModel, initialTransform.weights, EnumTransformType.WEIGHTS, partialTicks);
    }

    public void blendInitialPose(NodeModel nodeModel, float[] animation, EnumTransformType type, float partialTicks) {
        if (nodeModel == null || animation == null) return;
        float x, y, z, w;
        switch (type) {
            case TRANSLATION: {
                float[] t = nodeModel.getTranslation();
                if (t == null || t.length < 3 || animation.length < 3) break;
                t[0] = DynamXMath.interpolateLinear(partialTicks, t[0], animation[0]);
                t[1] = DynamXMath.interpolateLinear(partialTicks, t[1], animation[1]);
                t[2] = DynamXMath.interpolateLinear(partialTicks, t[2], animation[2]);
                break;
            }
            case ROTATION: {
                float[] r = nodeModel.getRotation();
                if (r == null || r.length < 4 || animation.length < 4) break;
                x = animation[0];
                y = animation[1];
                z = animation[2];
                w = animation[3];
                Quaternion quaternionInitial = QuaternionPool.get(x, y, z, w);
                Quaternion quaternionCurrent = QuaternionPool.get(r[0], r[1], r[2], r[3]);
                Quaternion blended = DynamXMath.slerp(partialTicks, quaternionCurrent, quaternionInitial);
                r[0] = blended.getX();
                r[1] = blended.getY();
                r[2] = blended.getZ();
                r[3] = blended.getW();
                break;
            }
            case SCALE: {
                float[] s = nodeModel.getScale();
                if (s == null || s.length < 3 || animation.length < 3) break;
                s[0] = DynamXMath.interpolateLinear(partialTicks, s[0], animation[0]);
                s[1] = DynamXMath.interpolateLinear(partialTicks, s[1], animation[1]);
                s[2] = DynamXMath.interpolateLinear(partialTicks, s[2], animation[2]);
                break;
            }
            case WEIGHTS: {
                float[] weights = nodeModel.getWeights();
                if (weights == null) break;
                int n = Math.min(weights.length, animation.length);
                for (int i = 0; i < n; i++) {
                    weights[i] = DynamXMath.interpolateLinear(partialTicks, weights[i], animation[i]);
                }
                break;
            }
        }
    }

    public fr.dynamx.api.dxmodel.DxModelPath getModelLocation() {
        // TODO port:1.20.1 - was @Override DxModelPath getModelLocation() from IGltfModelReceiver.
        return location;
    }

    public void onReceiveSharedModel(Object renderedModel) {
        // TODO port:1.20.1 - was IGltfModelReceiver callback (mcgltf). Once RenderedGltfModel exposes
        // scene/nodeModels/animationModels, populate nodeModels, animations, and initialNodeTransforms.
    }

    public static class Transform {
        public float[] translation;
        public float[] rotation;
        public float[] scale;
        public float[] weights;

        public Transform(float[] translation, float[] rotation, float[] scale, float[] weights) {
            this.translation = translation != null ? translation.clone() : null;
            this.rotation = rotation != null ? rotation.clone() : null;
            this.scale = scale != null ? scale.clone() : new float[]{1, 1, 1};
            this.weights = weights != null ? weights.clone() : null;
        }
    }

    public enum EnumTransformType {
        TRANSLATION, ROTATION, SCALE, WEIGHTS
    }
}
