package fr.dynamx.common.contentpack.parts;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.ICollisionsContainer;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.client.renders.model.texture.TextureVariantData;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.IRenderContext;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.client.renders.scene.node.SimpleNode;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.AbstractLightsModule;
import fr.dynamx.common.entities.vehicles.TrailerEntity;
import fr.dynamx.common.objloader.data.DxModelData;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contains multiple {@link LightObject}.
 */
@Setter
@Getter
@RegisteredSubInfoType(name = "MultiLight", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS}, strictName = false)
public class PartLightSource extends SubInfoType<ILightOwner<?>> implements ISubInfoTypeOwner<PartLightSource>, IDrawablePart<IModelPackObject> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS})
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("PartName".equals(key))
            return new IPackFilePropertyFixer.FixResult("ObjectName", false);
        return null;
    };

    private final String partName;

    private final List<LightObject> sources = new ArrayList<>();

    @PackFileProperty(configNames = "ObjectName")
    protected String objectName;

    @PackFileProperty(configNames = "BaseMaterial", required = false, defaultValue = "MaterialVariantsInfo value, or primary material configured in the model")
    protected String baseMaterial;

    private MaterialVariantsInfo<PartLightSource> variants;

    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, description = "common.position", required = false, defaultValue = "From model")
    protected Vector3f position;

    @PackFileProperty(configNames = "Rotation", required = false, defaultValue = "From model")
    protected Quaternion rotation;

    protected boolean isAutomaticPosition;

    @PackFileProperty(configNames = "DependsOnNode", required = false, description = "PartLightSource.DependsOnNode")
    protected String nodeDependingOnName;

    public PartLightSource(ISubInfoTypeOwner<ILightOwner<?>> owner, String partName) {
        super(owner);
        this.partName = partName;
    }

    /**
     * If this is a rotating light, this method reads the position and rotation from the 3D model owning this part. <br>
     * If the configured position is null, this method reads the position. <br>
     * If the configured position and rotation are null, this method also reads the rotation (only for GLTF models). <br>
     * <br>
     * If this isn't a rotating light, we don't need to do any transform to render it, so we don't need its position and rotation.
     *
     * @param model The 3D model owning this part
     */
    public void readPositionFromModel(ResourceLocation model) {
        if (getPosition() != null) {
            return;
        }
        if (sources.stream().noneMatch(s -> s.getRotateDuration() > 0)) {
            position = new Vector3f();
            return;
        }
        DxModelData modelData = DynamXContext.getDxModelDataFromCache(DynamXUtils.getModelPath(getPackName(), model));
        if (modelData != null) {
            position = DynamXUtils.readPartPosition(modelData, getObjectName(), true);
            if (getRotation() == null && position != null)
                rotation = DynamXUtils.readPartRotation(modelData, getObjectName());
        }
        if (getPosition() == null) {
            DynamXErrorManager.addPackError(getPackName(), "position_not_found_in_model", ErrorLevel.HIGH, owner.getName(), "3D object " + getObjectName() + " for part " + getName());
        } else {
            isAutomaticPosition = true;
        }
    }

    @Override
    public void appendTo(ILightOwner<?> owner) {
        if (owner instanceof AbstractItemObject)
            readPositionFromModel(((AbstractItemObject<?, ?>) owner).getModel());
        if (position == null) {
            INamedObject parent = getRootOwner();
            DynamXErrorManager.addPackError(getPackName(), "required_property", ErrorLevel.HIGH, parent.getName(), "Position in " + getName());
            position = new Vector3f();
        } else {
            position.multLocal(((ICollisionsContainer) owner).getScaleModifier());
        }
        owner.addLightSource(this);
    }

    @Nullable
    @Override
    public ILightOwner<?> getOwner() {
        return owner;
    }

    @Override
    public void addModules(Object entity, Object modules) {
        ModuleListBuilder builder = (ModuleListBuilder) modules;
        if (!builder.hasModuleOfClass(AbstractLightsModule.class)) {
            if (entity instanceof TrailerEntity)
                builder.add(new AbstractLightsModule.TrailerLightsModule(getOwner(), (PackPhysicsEntity<?, ?>) entity));
            else
                builder.add(new AbstractLightsModule.LightsModule(getOwner()));
        }
    }

    @Override
    public void addBlockModules(Object blockEntity, Object modules) {
        addModules(null, modules);
    }

    @Override
    public String getName() {
        return "PartLightSource with name " + getPartName();
    }

    @Override
    public String getNodeName() {
        return getPartName();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addToSceneGraph(IModelPackObject packInfo, Object sceneBuilder) {
        SceneBuilder<IRenderContext, IModelPackObject> sb = (SceneBuilder<IRenderContext, IModelPackObject>) sceneBuilder;
        if (nodeDependingOnName != null) {
            sb.addNode(packInfo, this, nodeDependingOnName);
        } else {
            sb.addNode(packInfo, this);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object createSceneGraph(Vector3f modelScale, List<Object> childGraph) {
        return new PartLightNode<>(this, modelScale, (List) childGraph);
    }

    /**
     * Post loads this light (computes texture variants)
     */
    public void postLoad(boolean hotReload) {
        configureLightTextureVariants(hotReload);
    }

    /**
     * Computes texture variants of this lights <br>
     * It adds the variants configured on the light, and the owner's variants, if any
     */
    public void configureLightTextureVariants(boolean hotReload) {
        TextureVariantData textureVariant;
        Map<String, TextureVariantData> nameToVariant = new HashMap<>();
        if (variants == null) {
            variants = new MaterialVariantsInfo<>(this);
            textureVariant = new TextureVariantData(baseMaterial != null ? baseMaterial : "default", (byte) 0);
            variants.addVariant(textureVariant, hotReload);
        } else if (baseMaterial != null && variants.getBaseMaterial() != null && variants.getBaseMaterial().equalsIgnoreCase("default")) {
            variants.setBaseMaterial(baseMaterial);
            textureVariant = new TextureVariantData(baseMaterial, (byte) 0);
            variants.addVariant(textureVariant, hotReload);
        }
        AtomicReference<Byte> nextTextureId = new AtomicReference<>(owner instanceof IModelTextureVariantsSupplier ? ((IModelTextureVariantsSupplier) owner).getMaxVariantId() : 0);
        variants.getTextureVariants().forEach((id, variantObj) -> {
            if (!(variantObj instanceof TextureVariantData)) return;
            TextureVariantData variant = (TextureVariantData) variantObj;
            if (nameToVariant.containsKey(variant.getName())) {
                return;
            }
            nameToVariant.put(variant.getName(), variant);
            if (variant.getId() >= nextTextureId.get()) {
                nextTextureId.set((byte) (variant.getId() + 1));
            }
        });

        List<LightObject> sources = getSources();
        for (LightObject source : sources) {
            if (source.getTextures() == null) {
                continue;
            }
            source.getBlinkTextures().clear();
            for (int j = 0; j < source.getTextures().length; j++) {
                String name = source.getTextures()[j];
                if (nameToVariant.containsKey(name)) {
                    source.getBlinkTextures().add(nameToVariant.get(name));
                } else {
                    textureVariant = new TextureVariantData(name, nextTextureId.getAndSet((byte) (nextTextureId.get() + 1)));
                    source.getBlinkTextures().add(textureVariant);
                    variants.addVariant(textureVariant, hotReload);
                    nameToVariant.put(name, textureVariant);
                }
            }
        }
    }

    public void addLightSource(LightObject object) {
        sources.add(object);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addSubProperty(ISubInfoType<PartLightSource> property) {
        if (property instanceof MaterialVariantsInfo) {
            variants = (MaterialVariantsInfo<PartLightSource>) property;
            return;
        }
        throw new IllegalStateException("Cannot add sub property to a light");
    }

    @Override
    public List<ISubInfoType<PartLightSource>> getSubProperties() {
        return Collections.emptyList();
    }

    class PartLightNode<A extends IModelPackObject> extends SimpleNode<IRenderContext, A> {
        public PartLightNode(PartLightSource lightSource, Vector3f scale, List<SceneNode<IRenderContext, A>> linkedChilds) {
            super(lightSource.getPosition(), lightSource.getRotation(), PartLightSource.this.isAutomaticPosition, scale, linkedChilds);
        }

        @Override
        public void render(IRenderContext context, A packInfo, Matrix4f parentTransform) {
            if (context.getModel() == null) return;

            boolean isEntity = context instanceof BaseRenderContext.EntityRenderContext
                    && ((BaseRenderContext.EntityRenderContext) context).getEntity() != null;
            boolean isBlock = context instanceof BaseRenderContext.BlockRenderContext
                    && ((BaseRenderContext.BlockRenderContext) context).getTileEntity() != null;
            AbstractLightsModule lights = null;
            if (isEntity) {
                lights = ((BaseRenderContext.EntityRenderContext) context).getEntity().getModuleByType(AbstractLightsModule.class);
            } else if (isBlock) {
                lights = ((BaseRenderContext.BlockRenderContext) context).getTileEntity().getModuleByType(AbstractLightsModule.class);
            }
            transformToRotationPoint(parentTransform);

            LightObject onLightObject = null;
            if (lights != null) {
                for (LightObject source : getSources()) {
                    if (lights.isLightOn(source.getLightId())) {
                        onLightObject = source;
                        break;
                    }
                }
            }
            boolean isOn = true;
            if (onLightObject == null) {
                if (getSources().isEmpty()) return;
                isOn = false;
                onLightObject = getSources().get(0);
            }
            int activeStep = 0;
            if (isOn && onLightObject.getBlinkSequence() != null) {
                int[] seq = onLightObject.getBlinkSequence();
                Entity view = Minecraft.getInstance().getCameraEntity();
                if (view != null) {
                    int mod = view.tickCount % seq[seq.length - 1];
                    isOn = false;
                    for (int i = seq.length - 1; i >= 0; i--) {
                        if (mod > seq[i]) {
                            isOn = i % 2 == 0;
                            activeStep = i + 1;
                            break;
                        }
                    }
                }
            }
            byte texId;
            if (isOn && !onLightObject.getBlinkTextures().isEmpty()) {
                activeStep = activeStep % onLightObject.getBlinkTextures().size();
                texId = onLightObject.getBlinkTextures().get(activeStep).getId();
            } else {
                texId = context.getTextureId();
                if (variants == null || !variants.hasVariant(texId)) {
                    texId = 0;
                }
            }

            PoseStack pose = ((BaseRenderContext) context).getPoseStack();
            if (pose != null) {
                pose.pushPose();
                if (translation != null) {
                    pose.translate(translation.x, translation.y, translation.z);
                }
                if (rotation != null) {
                    pose.mulPose(rotation);
                }
                if (isOn && onLightObject.getRotateDuration() > 0) {
                    Entity view = Minecraft.getInstance().getCameraEntity();
                    if (view != null) {
                        float step = ((float) (view.tickCount % onLightObject.getRotateDuration())) / onLightObject.getRotateDuration();
                        step = step * (FastMath.PI * 2);
                        pose.mulPose(Axis.YP.rotation(step));
                        transform.rotate(step, 0, 1, 0);
                    }
                }
                if (isAutomaticPosition) {
                    if (rotation != null) {
                        org.joml.Quaternionf inv = new org.joml.Quaternionf();
                        this.rotation.invert(inv);
                        pose.mulPose(inv);
                    }
                    if (translation != null) {
                        pose.translate(-translation.x, -translation.y, -translation.z);
                    }
                }
                // TODO port:1.20.1 - legacy used OpenGlHelper.setLightmapTextureCoords for emissive lighting.
                //   Re-add via RenderType.eyes or a custom packedLight override once lighting is reauthored.
                context.getModel().renderGroup(getObjectName(), texId, context.isUseVanillaRender());
                pose.popPose();
            }
            renderChildren(context, packInfo, transform);
        }
    }
}
