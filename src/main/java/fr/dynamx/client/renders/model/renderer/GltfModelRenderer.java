package fr.dynamx.client.renders.model.renderer;

import com.jme3.math.Quaternion;
import fr.dynamx.client.renders.animations.DxAnimation;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.QuaternionPool;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GLTF model renderer. The "survivor" of the OBJ/GLTF split: the GLTF format is the only
 * supported format going forward.
 * <p>
 * TODO port:1.20.1 - mcgltf (com.modularmods.mcgltf.dynamx.*) and jgltf-dynamx
 * (de.javagl.jgltf.dynamx.*) have not been ported to 1.20.1 yet. All scene/render/animation
 * receiver hooks are stubbed. The blend/reset logic (which is pure math) is kept as-is.
 * <p>
 * TODO port:1.20.1 - rendering must be performed via PoseStack + MultiBufferSource + RenderType;
 * the legacy renderForVanilla / renderForShaderMod entry points push GL_ALL_ATTRIB_BITS and rely on
 * GlStateManager which is gone in 1.20.1 core profile.
 */
public class GltfModelRenderer extends DxModelRenderer {

    @Getter
    public List<Object> nodeModels; // TODO port:1.20.1 - was List<NodeModel> (jgltf)
    public HashMap<String, List<Object>> animations; // TODO port:1.20.1 - List<InterpolatedChannel>
    public List<Object> animationModels; // TODO port:1.20.1 - List<AnimationModel>
    public float time;
    public DxAnimation animation;

    public Map<Object, Transform> initialNodeTransforms = new HashMap<>();

    public GltfModelRenderer(fr.dynamx.api.dxmodel.DxModelPath location, fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier textureVariants) {
        super(location, textureVariants);
        // TODO port:1.20.1 - MCglTF.getInstance().addGltfModelReceiver(this);
    }

    @Override
    public void renderModel(byte textureDataId, boolean forceVanillaRender) {
        // TODO port:1.20.1 - call scene.renderForVanilla / renderForShaderMod via PoseStack/MultiBufferSource
        renderVanillaOrShader(-1, forceVanillaRender);
    }

    @Override
    public boolean renderGroup(String group, byte textureDataId, boolean forceVanillaRender) {
        // TODO port:1.20.1 - look up nodeModelIndex by name and render only that subtree via mcgltf bridge
        int nodeModelIndex = getNodeModelIndex(group);
        if (nodeModelIndex == -1) return false;
        renderVanillaOrShader(nodeModelIndex, forceVanillaRender);
        return true;
    }

    @Override
    public boolean renderDefaultParts(byte textureDataId, boolean forceVanillaRender) {
        // TODO port:1.20.1 - iterate nodeModels and ask textureVariants.canRenderPart(name) once API is ported
        return false;
    }

    public void renderVanillaOrShader(int nodeModelIndex, boolean forceVanillaRender) {
        // TODO port:1.20.1 - mcgltf RenderedGltfScene rendering replaced by BufferBuilder/RenderType pipeline
    }

    @Override
    public boolean containsObjectOrNode(String name) {
        if (nodeModels == null) return false;
        // TODO port:1.20.1 - compare nodeModel.getName() once NodeModel is ported
        return false;
    }

    public int getNodeModelIndex(String objectName) {
        // TODO port:1.20.1 - resolve nodeModel index by name once NodeModel is ported
        return -1;
    }

    @Override
    public boolean isEmpty() {
        return nodeModels == null || nodeModels.isEmpty();
    }

    public void resetModel(float partialTicks) {
        for (Map.Entry<Object, Transform> entry : initialNodeTransforms.entrySet()) {
            Object node = entry.getKey();
            Transform initialTransform = entry.getValue();
            resetNodeModel(node, initialTransform, partialTicks);
        }
    }

    public void resetNodeModel(Object nodeModel, Transform initialTransform, float partialTicks) {
        blendInitialPose(nodeModel, initialTransform.translation, EnumTransformType.TRANSLATION, partialTicks);
        blendInitialPose(nodeModel, initialTransform.rotation, EnumTransformType.ROTATION, partialTicks);
        blendInitialPose(nodeModel, initialTransform.scale, EnumTransformType.SCALE, partialTicks);
        blendInitialPose(nodeModel, initialTransform.weights, EnumTransformType.WEIGHTS, partialTicks);
    }

    public void blendInitialPose(Object nodeModel, float[] animation, EnumTransformType type, float partialTicks) {
        // TODO port:1.20.1 - apply blend on the real NodeModel once jgltf-dynamx is ported.
        // The original implementation mutated nodeModel.getTranslation()/getRotation()/getScale()/getWeights()
        // using DynamXMath.interpolateLinear / slerp + QuaternionPool. Math kept below for reference.
        if (nodeModel == null || animation == null) return;
        switch (type) {
            case ROTATION:
                if (animation.length >= 4) {
                    Quaternion quaternionInitial = QuaternionPool.get(animation[0], animation[1], animation[2], animation[3]);
                    // TODO port:1.20.1 - DynamXMath.slerp(partialTicks, currentRotation, quaternionInitial)
                    @SuppressWarnings("unused")
                    Quaternion ignored = quaternionInitial;
                }
                break;
            case TRANSLATION:
            case SCALE:
            case WEIGHTS:
                // TODO port:1.20.1 - DynamXMath.interpolateLinear(partialTicks, current[i], animation[i])
                @SuppressWarnings("unused")
                float sample = DynamXMath.interpolateLinear(partialTicks, 0f, animation.length > 0 ? animation[0] : 0f);
                break;
        }
    }

    public fr.dynamx.api.dxmodel.DxModelPath getModelLocation() {
        // TODO port:1.20.1 - was @Override DxModelPath getModelLocation() from IGltfModelReceiver
        return location;
    }

    public void onReceiveSharedModel(Object renderedModel) {
        // TODO port:1.20.1 - was IGltfModelReceiver callback (mcgltf)
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
