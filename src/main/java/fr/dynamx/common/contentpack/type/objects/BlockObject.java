package fr.dynamx.common.contentpack.type.objects;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.contentpack.loader.InfoList;
import fr.dynamx.common.contentpack.parts.ILightOwner;
import fr.dynamx.common.contentpack.parts.PartLightSource;
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.common.contentpack.type.ObjectCollisionsHelper;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO port:1.20.1 - Original referenced (not yet ported):
 *   - fr.dynamx.api.dxmodel.EnumDxModelFormats (Phase 7) - replaced by a string suffix check
 *   - fr.dynamx.api.entities.modules.ModuleListBuilder (Phase 6) - relaxed to Object
 *   - fr.dynamx.api.events.CreatePackItemEvent.SimpleBlock (Phase 5)
 *   - fr.dynamx.api.events.client.BuildSceneGraphEvent.BuildBlockScene (Phase 7)
 *   - fr.dynamx.client.renders.model.renderer.ObjObjectRenderer / scene.node.* (Phase 7)
 *   - fr.dynamx.common.blocks.DynamXBlock / TEDynamXBlock (Phase 4)
 *   - net.minecraft.block.material.Material - removed in 1.20.1, replaced by MapColor and
 *     BlockBehaviour.Properties (Material/MaterialColor are gone; we use MapColor as a stand-in)
 *   - net.minecraftforge.common.MinecraftForge.EVENT_BUS - MinecraftForge.EVENT_BUS
 *   The createItem(), getSceneGraph(), getTextureVariantsFor() and postLoad collisions setup are stubbed
 *   where they require Phase 4/5/6/7 types. The new addModules(...) signature takes Object/Object.
 */
public class BlockObject<T extends BlockObject<T>> extends AbstractProp<T> implements ParticleEmitterInfo.IParticleEmitterContainer, ILightOwner<T> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.BLOCKS)
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("UseHullShape".equals(key))
            return new IPackFilePropertyFixer.FixResult("UseComplexCollisions", true);
        if ("Textures".equals(key))
            return new IPackFilePropertyFixer.FixResult("MaterialVariants", true, true);
        return null;
    };

    @Getter
    @Setter
    protected PropObject<?> propObject;

    @PackFileProperty(configNames = "Rotate", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0 0")
    @Getter
    @Setter
    protected Vector3f rotation = new Vector3f(0, 0, 0); //Not supported by props

    @PackFileProperty(configNames = "LightLevel", defaultValue = "0", required = false)
    @Getter
    @Setter
    protected float lightLevel;

    /**
     * TODO port:1.20.1 - Was net.minecraft.block.material.Material (Material.ROCK by default).
     *   In 1.20.1, Material was removed; BlockBehaviour.Properties replaces it. We store the parsed
     *   MapColor here as a reasonable analog. The DefinitionType parser for the Material key must be
     *   reworked to produce a MapColor (or a BlockBehaviour.Properties factory) in a later phase.
     */
    @PackFileProperty(configNames = "Material", required = false, defaultValue = "STONE")
    @Getter
    protected MapColor material = MapColor.STONE;

    @PackFileProperty(configNames = {"BreakHardness", "Hardness"}, required = false, defaultValue = "0.6")
    @Getter
    protected float blockHardness = 0.6f;

    @PackFileProperty(configNames = {"ExplosionResistance", "Resistance"}, required = false, defaultValue = "3")
    @Getter
    protected float blockResistance = 3;

    @PackFileProperty(configNames = "SoundType", required = false, defaultValue = "STONE")
    @Getter
    protected SoundType soundType = SoundType.STONE;

    @PackFileProperty(configNames = "HarvestTool", required = false)
    @Getter
    protected String harvestTool;

    @PackFileProperty(configNames = "HarvestLevel", required = false, defaultValue = "0")
    @Getter
    protected int harvestLevel;

    /**
     * The light sources of this block
     */
    @Getter
    protected final Map<String, PartLightSource> lightSources = new HashMap<>();

    protected final List<ParticleEmitterInfo<?>> particleEmitters = new ArrayList<>();

    /**
     * TODO port:1.20.1 - Was SceneNode&lt;?, ?&gt; (Phase 7).
     */
    protected fr.dynamx.client.renders.scene.node.SceneNode<?, ?> sceneNode;

    public BlockObject(String packName, String fileName) {
        super(packName, fileName);
        itemIcon = "Block";
        collisionsHelper = new ObjectCollisionsHelper();
    }

    /**
     * TODO port:1.20.1 - Original returned an IModelTextureVariants from an ObjObjectRenderer (Phase 7).
     *   Parameter relaxed to Object; without the renderer we cannot resolve a per-object light source,
     *   so we fall back to the super implementation.
     */
    @Override
    public Object getTextureVariantsFor(Object objObjectRenderer) {
        return super.getTextureVariantsFor(objObjectRenderer);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MaterialVariantsInfo<?> getVariants() {
        return getSubPropertyByType(MaterialVariantsInfo.class);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean postLoad(boolean hot) {
        if (texturesArray != null)
            new MaterialVariantsInfo(this, texturesArray).appendTo(this);
        //Map lights
        lightSources.values().forEach(l -> l.postLoad(hot));
        // TODO port:1.20.1 - Original called:
        //   collisionsHelper.loadCollisions(this, DynamXUtils.getModelPath(getPackName(), model), "", translation, 0, useComplexCollisions, scaleModifier, ObjectCollisionsHelper.CollisionType.BLOCK);
        //   DynamXUtils.getModelPath returns a DxModelPath (Phase 7). Passing null for the modelPath is
        //   safe because the ported loadCollisions only consumes part shapes when modelPath is unusable.
        collisionsHelper.loadCollisions(this, null, "", translation, 0, useComplexCollisions, scaleModifier, ObjectCollisionsHelper.CollisionType.BLOCK);
        if (hasTextureVariants() && getMaxVariantId() > 16 && (getCreativeTabName() == null || !getCreativeTabName().equalsIgnoreCase("None"))) {
            DynamXErrorManager.addError(getPackName(), DynamXErrorManager.PACKS_ERRORS, "too_many_variants", ErrorLevel.HIGH, getName(), "You can't use more than 16 variants on blocks !");
        }
        return super.postLoad(hot);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public IDynamXItem<T> createItem(InfoList<T> loader) {
        return (IDynamXItem<T>) new fr.dynamx.common.blocks.DynamXBlock<>((T) this);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public fr.dynamx.client.renders.scene.node.SceneNode<?, ?> getSceneGraph() {
        if (sceneNode == null) {
            fr.dynamx.client.renders.scene.SceneBuilder<
                    fr.dynamx.client.renders.scene.BaseRenderContext.BlockRenderContext,
                    BlockObject<T>> builder = new fr.dynamx.client.renders.scene.SceneBuilder<>();
            sceneNode = builder.buildBlockSceneGraph((BlockObject<T>) this,
                    (java.util.List) getDrawableParts(),
                    getScaleModifier());
        }
        return sceneNode;
    }

    @Override
    public String getTranslationKey(IDynamXItem<T> item, int itemMeta) {
        return super.getTranslationKey(item, itemMeta).replace("item", "tile");
    }

    @Override
    public void addParticleEmitter(ParticleEmitterInfo<?> emitterInfo) {
        particleEmitters.add(emitterInfo);
    }

    @Override
    public List<ParticleEmitterInfo<?>> getParticleEmitters() {
        return particleEmitters;
    }

    @Override
    public String toString() {
        return "BlockObject named " + getFullName();
    }

    public boolean isDxModel() {
        // TODO port:1.20.1 - Original used EnumDxModelFormats.isDxModel(getModel().getPath()). Phase 7.
        //   Use a simple suffix-based fallback until the format enum is ported.
        if (getModel() == null) return false;
        String path = getModel().getPath().toLowerCase();
        return path.endsWith(".obj") || path.endsWith(".gltf") || path.endsWith(".glb");
    }

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
    public PartLightSource getLightSource(String objectName) {
        return lightSources.get(objectName);
    }

    /**
     * Adds block entity modules associated with this block.
     *
     * TODO port:1.20.1 - Original signature:
     *   public void addModules(TEDynamXBlock blockEntity, ModuleListBuilder modules)
     *   TEDynamXBlock is Phase 4 and ModuleListBuilder is Phase 6 - both parameters are relaxed
     *   to Object until those phases land.
     */
    public void addModules(Object blockEntity, Object modules) {
        getSubProperties().forEach(sub -> sub.addBlockModules(blockEntity, modules));
        getAllParts().forEach(sub -> sub.addBlockModules(blockEntity, modules));
        getLightSources().values().forEach(compoundLight -> compoundLight.addBlockModules(blockEntity, modules));
    }
}
