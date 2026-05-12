package fr.dynamx.common.contentpack.type.objects;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.contentpack.loader.InfoList;
import fr.dynamx.common.contentpack.loader.PackFilePropertyData;
import fr.dynamx.common.contentpack.loader.SubInfoTypeAnnotationCache;
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.common.contentpack.type.ObjectCollisionsHelper;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO port:1.20.1 - Original referenced (not yet ported):
 *   - fr.dynamx.api.entities.modules.ModuleListBuilder (Phase 6)
 *   - fr.dynamx.api.events.{CreatePackItemEvent.PropsItem, BuildSceneGraphEvent.BuildEntityScene} (Phase 5/7)
 *   - fr.dynamx.client.renders.scene.node.* (Phase 7)
 *   - fr.dynamx.common.contentpack.{ContentPackLoader, DynamXObjectLoaders} (Phase 3b - written below)
 *   - fr.dynamx.common.entities.PackPhysicsEntity (Phase 6)
 *   - fr.dynamx.common.items.ItemProps (Phase 6)
 *   - fr.dynamx.utils.DynamXUtils.getModelPath (broken in main due to unported deps)
 *   - net.minecraft.item.Item / ItemStack -&gt; net.minecraft.world.item.Item / ItemStack
 *   - net.minecraftforge.common.MinecraftForge.EVENT_BUS -&gt; MinecraftForge.EVENT_BUS
 *   The createItem(), getSceneGraph(), addModules(), appendTo(), postLoad(), getPickedResult() bodies
 *   that depend on the above are stubbed accordingly.
 */
@RegisteredSubInfoType(name = "prop", registries = SubInfoTypeRegistries.BLOCKS, strictName = false)
public class PropObject<T extends PropObject<T>> extends AbstractProp<T> implements IPhysicsPackInfo,
        ISubInfoType<BlockObject<?>>, ParticleEmitterInfo.IParticleEmitterContainer {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS})
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("UseHullShape".equals(key))
            return new IPackFilePropertyFixer.FixResult("UseComplexCollisions", true);
        if ("Textures".equals(key))
            return new IPackFilePropertyFixer.FixResult("MaterialVariants", true, true);
        return null;
    };

    private final BlockObject<?> owner;
    @PackFileProperty(configNames = "EmptyMass")
    @Getter
    @Setter
    protected int emptyMass;
    @PackFileProperty(configNames = "CenterOfGravityOffset", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F)
    @Getter
    @Setter
    protected Vector3f centerOfMass;
    @PackFileProperty(configNames = "SpawnOffset", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0.65 0")
    @Getter
    @Setter
    protected Vector3f spawnOffset = new Vector3f(0, 0.65f, 0);

    @PackFileProperty(configNames = "ContinuousCollisionDetection", required = false, defaultValue = "false")
    @Getter
    @Setter
    protected boolean isCCDEnabled;
    @PackFileProperty(configNames = "Friction", required = false, defaultValue = "0.5")
    @Getter
    @Setter
    protected float friction = 0.5f;
    @PackFileProperty(configNames = "Margin", required = false, defaultValue = "0.04")
    @Getter
    @Setter
    protected float margin = 0.04f;
    @Getter
    @Setter
    @PackFileProperty(configNames = "Bounciness", required = false, defaultValue = "0")
    protected float restitutionFactor;

    @PackFileProperty(configNames = "DespawnTime", required = false, defaultValue = "\"-1\" (disabled)")
    @Getter
    @Setter
    protected float despawnTime = -1;

    @PackFileProperty(configNames = "LinearDamping", required = false, defaultValue = "0")
    @Getter
    @Setter
    protected float linearDamping;
    @PackFileProperty(configNames = "AngularDamping", required = false, defaultValue = "0")
    @Getter
    @Setter
    protected float angularDamping;

    @Getter
    @PackFileProperty(configNames = "InWaterLinearDamping", required = false, defaultValue = "0.6")
    protected float inWaterLinearDamping = 0.6f;
    @Getter
    @PackFileProperty(configNames = "InWaterAngularDamping", required = false, defaultValue = "0.6")
    protected float inWaterAngularDamping = 0.6f;

    protected final List<ParticleEmitterInfo<?>> particleEmitters = new ArrayList<>();

    /**
     * TODO port:1.20.1 - Was SceneNode&lt;?, ?&gt; (Phase 7).
     */
    protected Object sceneGraph;

    public PropObject(ISubInfoTypeOwner<BlockObject<?>> owner, String fileName) {
        super(owner.getPackName(), fileName);
        this.itemIcon = "Prop";
        BlockObject<?> block = (BlockObject<?>) owner;
        this.owner = block;
        this.setDefaultName(block.getDefaultName());
        this.setDescription(block.getDescription());
        this.model = block.getModel();
        this.itemScale = block.getItemScale();
        this.itemTranslate = block.getItemTranslate();
        this.itemRotate = block.getItemRotate();
        this.item3DRenderLocation = block.getItem3DRenderLocation();
        this.translation = block.getTranslation();
        this.scaleModifier = block.getScaleModifier();
        this.renderDistanceSquared = block.getRenderDistanceSquared();
        this.creativeTabName = block.getCreativeTabName();
        this.useComplexCollisions = block.useComplexCollisions();
        this.particleEmitters.addAll(block.getParticleEmitters());
        this.collisionsHelper = block.getCollisionsHelper().copy();
        this.getDrawableParts().addAll(block.getDrawableParts());
    }

    @Override
    public List<Object> getInitiallyConfiguredProperties() {
        //Don't require properties of the block
        // TODO port:1.20.1 - ISubInfoTypeOwner.getInitiallyConfiguredProperties() was relaxed to List<Object>;
        //   the stream still returns PackFilePropertyData entries but is widened on return.
        return SubInfoTypeAnnotationCache.getOrLoadData(BlockObject.class).values().stream()
                .filter(PackFilePropertyData::isRequired)
                .collect(Collectors.toList());
    }

    @Override
    public boolean postLoad(boolean hot) {
        // TODO port:1.20.1 - Original passed DynamXUtils.getModelPath(getPackName(), model) which depends
        //   on the unported DxModelPath. We pass null until the obj/gltf pipeline is back online.
        collisionsHelper.loadCollisions(this, null, "", centerOfMass, 0, useComplexCollisions, scaleModifier, ObjectCollisionsHelper.CollisionType.PROP);
        if (collisionsHelper.hasPhysicsCollisions())
            collisionsHelper.getPhysicsCollisionShape().setMargin(margin);
        if (!super.postLoad(hot))
            return false;
        return true;
    }

    @Override
    public void appendTo(BlockObject<?> owner) {
        owner.propObject = this;
        // TODO port:1.20.1 - Original: DynamXObjectLoaders.PROPS.loadItems(this, ContentPackLoader.isHotReloading);
        //   Both still live in this Phase 3b batch; once those classes are written this line should be restored.
    }

    @Nullable
    @Override
    public BlockObject<?> getOwner() {
        return owner;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public IDynamXItem<T> createItem(InfoList<T> loader) {
        return (IDynamXItem<T>) new fr.dynamx.common.items.ItemProps((PropObject) this);
    }

    @Override
    public MaterialVariantsInfo<?> getVariants() {
        return owner != null ? owner.getVariants() : null;
    }

    @Override
    public ItemStack getPickedResult(int metadata) {
        // TODO port:1.20.1 - Original was new ItemStack((Item) getItems()[0], 1, metadata).
        //   1.20.1 ItemStack no longer carries metadata; once Items are created (Phase 6) we should
        //   build the stack from getItems()[0] and attach metadata via DataComponents.
        if (getItems() == null || getItems().length == 0)
            return ItemStack.EMPTY;
        Object first = getItems()[0];
        if (first instanceof net.minecraft.world.item.Item) {
            return new ItemStack((net.minecraft.world.item.Item) first);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean shouldRegisterModel() {
        return owner == null || !model.equals(owner.getModel()); //Don't register the model twice if there is a block owning this prop
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
        return "PropObject named " + getFullName();
    }

    @Override
    public void addModules(Object entity, Object modules) {
        // TODO port:1.20.1 - Original signature took (PackPhysicsEntity<?,?>, ModuleListBuilder).
        //   Both relaxed to Object (Phase 6). Body mirrors the legacy aggregation logic.
        getSubProperties().forEach(sub -> sub.addModules(entity, modules));
        getAllParts().forEach(sub -> sub.addModules(entity, modules));
        if (getOwner() != null)
            getOwner().getLightSources().values().forEach(compoundLight -> compoundLight.addModules(entity, modules));
    }

    @Override
    public Object getSubInfoTypesRegistry() {
        // TODO port:1.20.1 - Original:
        //   return (SubInfoTypesRegistry<T>) SubInfoTypeRegistries.PROPS.getInfoList().getDefaultSubInfoTypesRegistry();
        //   SubInfoTypesRegistry/InfoList still need to be ported in this Phase 3b batch.
        return null;
    }

    @Override
    public Object getSceneGraph() {
        // TODO port:1.20.1 - Original posted BuildEntityScene event then fell back to new EntityNode<>(...).
        //   Phase 7 dependency.
        return sceneGraph;
    }
}
