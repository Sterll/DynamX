package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
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
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains multiple {@link LightObject}.
 *
 * TODO port:1.20.1 - Original referenced:
 *   - fr.dynamx.client.renders.scene.* (SceneBuilder/SceneNode/SimpleNode/BaseRenderContext/IRenderContext) - Phase 7
 *   - fr.dynamx.client.renders.model.texture.TextureVariantData - Phase 7
 *   - fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier - Phase 7
 *   - fr.dynamx.api.entities.modules.ModuleListBuilder - Phase 6
 *   - fr.dynamx.common.DynamXContext / DxModelData / DynamXUtils.readPartPosition - not yet ported
 *   - fr.dynamx.common.entities.modules.AbstractLightsModule - Phase 6
 *   - fr.dynamx.common.entities.vehicles.TrailerEntity - Phase 6
 *   - fr.dynamx.common.blocks.TEDynamXBlock - Phase 4
 *   - GlStateManager/OpenGlHelper/RenderGlobal - removed in 1.20.1 (Phase 7 will rewrite with PoseStack)
 *   The configureLightTextureVariants() body, the PartLightNode inner class, readPositionFromModel
 *   and the addToSceneGraph()/createSceneGraph() bodies are stubbed accordingly.
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
     * TODO port:1.20.1 - Original used DynamXContext.getDxModelDataFromCache / DynamXUtils.readPartPosition
     *   to derive position/rotation from the obj/gltf model. The obj loader is not yet ported in 1.20.1;
     *   this method now logs a pack error and falls back to a zero position when needed.
     */
    public void readPositionFromModel(ResourceLocation model) {
        if (getPosition() != null) {
            return;
        }
        if (sources.stream().noneMatch(s -> s.getRotateDuration() > 0)) {
            position = new Vector3f();
            return;
        }
        // TODO port:1.20.1 - obj/gltf model data lookup goes here.
        DynamXErrorManager.addPackError(getPackName(), "position_not_found_in_model", ErrorLevel.HIGH, owner.getName(),
                "3D object " + getObjectName() + " for part " + getName() + " - OBJ loader removed in 1.20.1, set Position explicitly");
        position = new Vector3f();
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
        // TODO port:1.20.1 - Original:
        //   if (!modules.hasModuleOfClass(AbstractLightsModule.class)) {
        //       if (entity instanceof TrailerEntity)
        //           modules.add(new AbstractLightsModule.TrailerLightsModule(getOwner(), entity));
        //       else
        //           modules.add(new AbstractLightsModule.LightsModule(getOwner()));
        //   }
        //   AbstractLightsModule and TrailerEntity live in Phase 6.
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
    public void addToSceneGraph(IModelPackObject packInfo, Object sceneBuilder) {
        // TODO port:1.20.1 - Original:
        //   if (nodeDependingOnName != null) sceneBuilder.addNode(packInfo, this, nodeDependingOnName);
        //   else sceneBuilder.addNode(packInfo, this);
        //   SceneBuilder lives in Phase 7.
    }

    @Override
    public Object createSceneGraph(Vector3f modelScale, List<Object> childGraph) {
        // TODO port:1.20.1 - Original returned new PartLightNode<>(this, modelScale, (List) childGraph).
        return null;
    }

    /**
     * Post loads this light (computes texture variants)
     */
    public void postLoad(boolean hotReload) {
        configureLightTextureVariants(hotReload);
    }

    /**
     * Computes texture variants of this light.
     *
     * TODO port:1.20.1 - Original built TextureVariantData entries (Phase 7) and merged them with
     *   the owner's IModelTextureVariantsSupplier (Phase 7). With those types unavailable we keep
     *   only the MaterialVariantsInfo bookkeeping; texture id resolution will be re-implemented
     *   when Phase 7 lands. The method now only ensures a variants holder is created.
     */
    public void configureLightTextureVariants(boolean hotReload) {
        if (variants == null) {
            variants = new MaterialVariantsInfo<>(this);
            if (baseMaterial != null) {
                variants.setBaseMaterial(baseMaterial);
            }
        } else if (baseMaterial != null && "default".equalsIgnoreCase(variants.getBaseMaterial())) {
            variants.setBaseMaterial(baseMaterial);
        }
        // TODO port:1.20.1 - Build TextureVariantData entries from each LightObject.getTextures() once Phase 7 ports the class.
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
}
