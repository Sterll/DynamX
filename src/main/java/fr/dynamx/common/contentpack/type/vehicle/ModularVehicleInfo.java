package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.ICollisionsContainer;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.contentpack.loader.InfoList;
import fr.dynamx.common.contentpack.parts.ILightOwner;
import fr.dynamx.common.contentpack.parts.PartLightSource;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.common.contentpack.type.ObjectCollisionsHelper;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.utils.EnumPlayerStandOnTop;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All information about a vehicle.
 *
 * TODO port:1.20.1 - Original referenced (not yet ported):
 *   - fr.dynamx.api.dxmodel.{DxModelPath, IModelTextureVariantsSupplier} (Phase 7)
 *   - fr.dynamx.api.entities.modules.ModuleListBuilder (Phase 6)
 *   - fr.dynamx.api.events.{CreatePackItemEvent, BuildSceneGraphEvent} (Phase 5/7)
 *   - fr.dynamx.client.renders.model.ItemDxModel / texture.TextureVariantData / scene.node.* (Phase 7)
 *   - fr.dynamx.common.contentpack.DynamXObjectLoaders (Phase 3b - written below)
 *   - fr.dynamx.common.entities.{BaseVehicleEntity, PackPhysicsEntity} (Phase 6)
 *   - fr.dynamx.utils.DynamXUtils.getModelPath (broken: references unported DxModelPath)
 *   - net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType -&gt; ItemDisplayContext
 *   - net.minecraftforge.common.MinecraftForge.EVENT_BUS -&gt; MinecraftForge.EVENT_BUS
 *   - net.minecraftforge.fml.relauncher.{Side, SideOnly} -&gt; net.minecraftforge.api.distmarker.{Dist, OnlyIn}
 *
 *   The createItem(), getSceneGraph(), addModules(), getTextureVariantsFor(), wheel/engine attachment in
 *   postLoad(), applyItemTransforms() and translation-key helpers are all stubbed where they require the
 *   above. The IModelTextureVariantsSupplier extension is dropped; texture-related accessors return Object.
 */
@Getter
public class ModularVehicleInfo extends AbstractItemObject<ModularVehicleInfo, ModularVehicleInfo> implements IPhysicsPackInfo,
        ParticleEmitterInfo.IParticleEmitterContainer, IModelPackObject, IPartContainer<ModularVehicleInfo>, ICollisionsContainer, ILightOwner<ModularVehicleInfo> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER})
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("UseHullShape".equals(key))
            return new IPackFilePropertyFixer.FixResult("UseComplexCollisions", true);
        if ("Textures".equals(key))
            return new IPackFilePropertyFixer.FixResult("MaterialVariants", true, true);
        return null;
    };

    @Setter
    private VehicleValidator validator;

    /* == Pack properties == */

    @PackFileProperty(configNames = "DefaultEngine", required = false)
    protected String defaultEngine;
    @PackFileProperty(configNames = "DefaultSounds", required = false)
    protected String defaultSounds;

    @PackFileProperty(configNames = "MaxVehicleSpeed", required = false, defaultValue = "infinite")
    protected float vehicleMaxSpeed = Integer.MAX_VALUE;

    /**
     * The directing wheel id <br>
     * Used to render the steering wheel
     */
    private int directingWheel;

    @Getter
    @Setter
    @PackFileProperty(configNames = "PlayerStandOnTop", required = false, defaultValue = "ALWAYS")
    protected EnumPlayerStandOnTop playerStandOnTop = EnumPlayerStandOnTop.ALWAYS;

    @Getter
    @Setter
    @PackFileProperty(configNames = "DefaultZoomLevel", required = false, defaultValue = "4")
    protected int defaultZoomLevel = 4;

    /* == Physics properties == */

    @Getter
    @Setter
    @PackFileProperty(configNames = "EmptyMass")
    protected int emptyMass;
    @Getter
    @Setter
    @PackFileProperty(configNames = "CenterOfGravityOffset", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false)
    protected Vector3f centerOfMass;

    @Getter
    @Setter
    @PackFileProperty(configNames = "DragCoefficient", required = false)
    protected float dragFactor;

    @Getter
    @Setter
    @PackFileProperty(configNames = "LinearDamping", required = false, defaultValue = "0.5 for helicopters, 0 for others")
    protected float linearDamping;
    @Getter
    @Setter
    @PackFileProperty(configNames = "AngularDamping", required = false, defaultValue = "0.9 for helicopters, 0.5 for boats, 0 for others")
    protected float angularDamping;

    @PackFileProperty(configNames = "InWaterLinearDamping", required = false, defaultValue = "0.6")
    protected float inWaterLinearDamping = 0.6f;
    @PackFileProperty(configNames = "InWaterAngularDamping", required = false, defaultValue = "0.9 for helicopters, 0.6 for others")
    protected float inWaterAngularDamping = 0.6f;

    @Getter
    @Setter
    @PackFileProperty(configNames = "UseComplexCollisions", required = false, defaultValue = "true", description = "common.UseComplexCollisions")
    protected boolean useComplexCollisions = true;

    /**
     * The shapes of this vehicle, can be used for collisions
     */
    @Getter
    protected ObjectCollisionsHelper collisionsHelper = new ObjectCollisionsHelper();

    /**
     * The friction points of this vehicle
     */
    protected final List<FrictionPoint> frictionPoints = new ArrayList<>();

    /* == Render properties == */

    @PackFileProperty(configNames = "ShapeYOffset", required = false)
    protected float shapeYOffset;

    @PackFileProperty(configNames = "ScaleModifier", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false,
            defaultValue = "1 1 1")
    protected Vector3f scaleModifier = new Vector3f(1, 1, 1);

    @Setter
    @PackFileProperty(configNames = "RenderDistanceSquared", required = false, defaultValue = "-1")
    protected float renderDistanceSquared = -1;

    /**
     * The particle emitters of this vehicle
     */
    protected final List<ParticleEmitterInfo<?>> particleEmitters = new ArrayList<>();

    /**
     * The light sources of this vehicle
     */
    protected final Map<String, PartLightSource> lightSources = new HashMap<>();

    /**
     * Maps the metadata to the texture data
     */
    private MaterialVariantsInfo<ModularVehicleInfo> variants;

    protected fr.dynamx.client.renders.scene.node.SceneNode<?, ?> sceneGraph;

    @Deprecated
    @PackFileProperty(configNames = "Textures", required = false, type = DefinitionType.DynamXDefinitionTypes.STRING_ARRAY_2D)
    private String[][] texturesArray;


    public ModularVehicleInfo(String packName, String fileName, VehicleValidator validator) {
        super(packName, fileName);
        this.validator = validator;
        this.validator.initProperties(this);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean postLoad(boolean hot) {
        // TODO port:1.20.1 - Original called DynamXUtils.getModelPath(getPackName(), model). That helper
        //   references the unported DxModelPath; pass null until Phase 7 brings the model pipeline back.
        collisionsHelper.loadCollisions(this, null, "chassis", centerOfMass, shapeYOffset, useComplexCollisions, scaleModifier, ObjectCollisionsHelper.CollisionType.VEHICLE);

        // Attach wheels: PartWheel.defaultWheelInfo must be wired from DynamXObjectLoaders.WHEELS
        // so getDefaultWheelInfo() returns a valid PartWheelInfo at render time. Without this the
        // wheel scene nodes bail out (info==null) and no wheels are rendered.
        Map<String, PartWheelInfo> wheels = fr.dynamx.common.contentpack.DynamXObjectLoaders.WHEELS.getInfos();
        boolean hasHandbrake = false;
        int directingWheel = -1;
        List<PartWheel> partsByType = getPartsByType(PartWheel.class);
        for (int i = 0; i < partsByType.size(); i++) {
            PartWheel partWheel = partsByType.get(i);
            partWheel.setDefaultWheelInfo(wheels.get(partWheel.getDefaultWheelName()));
            if (partWheel.isHandBrakingWheel())
                hasHandbrake = true;
            if (directingWheel == -1 && partWheel.isWheelIsSteerable())
                directingWheel = i;
        }
        if (directingWheel == -1)
            directingWheel = 0;
        this.directingWheel = directingWheel;
        if (!hasHandbrake) {
            for (PartWheel partWheel : partsByType) {
                if (!partWheel.isDrivingWheel())
                    partWheel.setHandBrakingWheel(true);
            }
        }
        if (defaultEngine != null) {
            BaseEngineInfo engine = fr.dynamx.common.contentpack.DynamXObjectLoaders.ENGINES.findOrLoadInfo(
                    defaultEngine, validator.getEngineClass());
            if (engine == null) {
                throw new IllegalArgumentException("Engine " + defaultEngine + " of " + getFullName()
                        + " was not found, check file names and previous loading errors!");
            }
            engine.appendTo(this);
            engine.postLoad(this, hot);
        }
        variants = getSubPropertyByType(MaterialVariantsInfo.class);
        if (texturesArray != null) {
            variants = new MaterialVariantsInfo(this, texturesArray);
            variants.appendTo(this);
        }
        //Map lights
        lightSources.values().forEach(l -> l.postLoad(hot));
        //Post-load sub-properties
        if (!super.postLoad(hot))
            return false;
        //Validate vehicle type
        validator.validate(this);
        return true;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public IDynamXItem<ModularVehicleInfo> createItem(InfoList<ModularVehicleInfo> loader) {
        return (IDynamXItem) validator.getSpawnItem(this);
    }

    @Override
    public void addModules(Object entity, Object modules) {
        getSubProperties().forEach(sub -> sub.addModules(entity, modules));
        getAllParts().forEach(sub -> sub.addModules(entity, modules));
        getLightSources().values().forEach(compoundLight -> compoundLight.addModules(entity, modules));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void applyItemTransforms(net.minecraft.world.item.ItemDisplayContext renderType, ItemStack stack, fr.dynamx.client.renders.model.ItemDxModel model, Matrix4f transform) {
        super.applyItemTransforms(renderType, stack, model, transform);
        if (renderType == net.minecraft.world.item.ItemDisplayContext.GUI) {
            transform.rotate((float) Math.PI, 0, 1, 0);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <A extends InteractivePart<?, ?>> List<A> getInteractiveParts() {
        // TODO port:1.20.1 - go through raw List for capture compatibility under 1.20.1 javac.
        return (List<A>) (List) getPartsByType(InteractivePart.class);
    }

    @Override
    public ItemStack getPickedResult(int metadata) {
        // TODO port:1.20.1 - Original built new ItemStack((Item) getItems()[0], 1, metadata). Metadata is
        //   gone in 1.20.1 (DataComponents replace it). Items will be present after Phase 6.
        if (getItems() == null || getItems().length == 0)
            return ItemStack.EMPTY;
        Object first = getItems()[0];
        if (first instanceof net.minecraft.world.item.Item) {
            return new ItemStack((net.minecraft.world.item.Item) first);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public fr.dynamx.client.renders.scene.node.SceneNode<?, ?> getSceneGraph() {
        // Build the scene graph from this vehicle's drawable parts (wheels, doors, lights, ...).
        // Without this, the EntityNode would have no children and only the chassis body would
        // render - PartWheel scene nodes would never be visited so the wheels stay invisible.
        if (sceneGraph == null) {
            fr.dynamx.client.renders.scene.SceneBuilder<
                    fr.dynamx.client.renders.scene.BaseRenderContext.EntityRenderContext,
                    ModularVehicleInfo> builder = new fr.dynamx.client.renders.scene.SceneBuilder<>();
            sceneGraph = builder.buildEntitySceneGraph(this,
                    (java.util.List) getDrawableParts(),
                    getScaleModifier());
        }
        return sceneGraph;
    }

    @Override
    public PartLightSource getLightSource(String objectName) {
        return lightSources.get(objectName);
    }

    // TODO port:1.20.1 - MaterialVariantsInfo.variantsMap holds Object placeholders until TextureVariantData is ported (Phase 7).
    //   The original .getName() call lived on TextureVariantData; values are currently plain String names (see MaterialVariantsInfo.appendTo).
    public byte getIdForVariant(String variantName) {
        if (variants != null) {
            for (byte i = 0; i < variants.getVariantsMap().size(); i++) {
                if (variants.getVariantsMap().get(i).getName().equalsIgnoreCase(variantName))
                    return i;
            }
        }
        return 0;
    }

    public String getVariantName(byte variantId) {
        if (variants != null) {
            return variants.getVariantsMap().getOrDefault(variantId, variants.getDefaultVariant()).getName();
        }
        return "default";
    }

    @Override
    public String getIconFileName(byte metadata) {
        return variants != null ? variants.getVariantsMap().get(metadata).getName() : super.getIconFileName(metadata);
    }

    /**
     * TODO port:1.20.1 - Original returned IModelTextureVariants from an ObjObjectRenderer (Phase 7).
     *   Parameter/return relaxed to Object. Per-object light-source resolution still works thanks to
     *   getLightSource(String) but we cannot inspect the ObjObjectRenderer here.
     */
    public Object getTextureVariantsFor(Object objObjectRenderer) {
        return getVariants();
    }

    public boolean hasTextureVariants() {
        return getVariants() != null;
    }

    public byte getMaxVariantId() {
        return (byte) (hasTextureVariants() ? getVariants().getVariantsMap().size() : 1);
    }

    @Override
    public String getTranslationKey(IDynamXItem<ModularVehicleInfo> item, int itemMeta) {
        if (itemMeta == 0)
            return super.getTranslationKey(item, itemMeta);
        // TODO port:1.20.1 - Original used TextureVariantData (Phase 7). MaterialVariantsInfo holds the
        //   name list which is enough for translation keys; we look up the variant by id.
        String name = variants != null && variants.getVariantsMap().containsKey((byte) itemMeta)
                ? String.valueOf(variants.getVariantsMap().get((byte) itemMeta)).toLowerCase()
                : "";
        return super.getTranslationKey(item, itemMeta) + "_" + name;
    }

    @Override
    public String getTranslatedName(IDynamXItem<ModularVehicleInfo> item, int itemMeta) {
        if (itemMeta == 0)
            return super.getTranslatedName(item, itemMeta);
        String name = variants != null && variants.getVariantsMap().containsKey((byte) itemMeta)
                ? String.valueOf(variants.getVariantsMap().get((byte) itemMeta))
                : "";
        return super.getTranslatedName(item, itemMeta) + " " + name;
    }

    @Override
    public String toString() {
        return "ModularVehicleInfo named " + getFullName();
    }

    public void addFrictionPoint(FrictionPoint frictionPoint) {
        frictionPoints.add(frictionPoint);
    }

    @Override
    public void addParticleEmitter(ParticleEmitterInfo<?> particleEmitterInfo) {
        particleEmitters.add(particleEmitterInfo);
    }

    /**
     * Adds a light source to this vehicle
     *
     * @param source The light source to add
     */
    @Override
    public void addLightSource(PartLightSource source) {
        if (lightSources.containsKey(source.getObjectName())) {
            DynamXErrorManager.addPackError(getPackName(), "duplicated_multi_light", ErrorLevel.HIGH, getName(), "Light named " + source.getPartName() + " on part " + source.getObjectName() + " is in conflict with " + lightSources.get(source.getObjectName()).getPartName());
            return;
        }
        lightSources.put(source.getObjectName(), source);
        addDrawablePart(source);
    }

    @Override
    public float getBaseItemScale() {
        return 0.2f;
    }
}
