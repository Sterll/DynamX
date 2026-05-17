package fr.dynamx.common.contentpack.type.objects;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.contentpack.loader.InfoList;
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * Armor object, for "armor_" files.
 *
 * TODO port:1.20.1 - Original referenced (not yet ported):
 *   - fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier (Phase 7) - kept conceptually but extension removed
 *   - fr.dynamx.api.events.{CreatePackItemEvent, BuildSceneGraphEvent} (Phase 5/7)
 *   - fr.dynamx.client.renders.model.{ModelObjArmor,ObjObjectRenderer} / scene.node.* (Phase 7)
 *   - fr.dynamx.common.DynamXContext / DxModelRegistry (not yet ported)
 *   - fr.dynamx.common.items.DynamXItemArmor (Phase 6)
 *   - net.minecraft.inventory.EntityEquipmentSlot -&gt; net.minecraft.world.entity.EquipmentSlot
 *   - net.minecraft.item.ItemArmor.ArmorMaterial / EnumHelper.addArmorMaterial - 1.20.1 uses ArmorMaterials
 *     registry plus a custom ArmorMaterial implementation, not the legacy enum helper
 *   - SoundEvent.REGISTRY.getObject(...) - 1.20.1 uses BuiltInRegistries.SOUND_EVENT
 *
 * The createItems() / initArmorModel() / getSceneGraph() bodies are stubbed.
 */
public class ArmorObject<T extends ArmorObject<T>> extends AbstractItemObject<T, T> {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.ARMORS)
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("Textures".equals(key))
            return new IPackFilePropertyFixer.FixResult("MaterialVariants", true, true);
        return null;
    };

    @Getter
    @PackFileProperty(configNames = "ArmorHead", required = false)
    protected String armorHead;
    @Getter
    @PackFileProperty(configNames = "ArmorBody", required = false)
    protected String armorBody;
    @Getter
    @PackFileProperty(configNames = "ArmorArms", required = false)
    protected String[] armorArms;
    @Getter
    @PackFileProperty(configNames = "ArmorLegs", required = false)
    protected String[] armorLegs;
    @Getter
    @PackFileProperty(configNames = "ArmorFoot", required = false)
    protected String[] armorFoot;
    @Getter
    @PackFileProperty(configNames = "Durability", required = false, defaultValue = "5")
    protected int durability = 5;
    @Getter
    @PackFileProperty(configNames = "Enchantability", required = false, defaultValue = "15")
    protected int enchantibility = 15;
    @Getter
    @PackFileProperty(configNames = "EquipSound", required = false, defaultValue = "item.armor.equip_leather")
    protected SoundEvent sound = SoundEvents.ARMOR_EQUIP_LEATHER;
    @Getter
    @PackFileProperty(configNames = "Toughness", required = false, defaultValue = "0")
    protected float toughness = 0;
    @Getter
    @PackFileProperty(configNames = "DamageReduction", required = false, defaultValue = "\"1 2 3 1\" (leather)")
    protected int[] reductionAmount = new int[]{1, 2, 3, 1};
    @Getter
    @Deprecated
    @PackFileProperty(configNames = "Textures", required = false, type = DefinitionType.DynamXDefinitionTypes.STRING_ARRAY_2D)
    protected String[][] texturesArray;

    /**
     * TODO port:1.20.1 - Was ModelObjArmor (Phase 7). Relaxed to Object; the side-only annotation moved
     *   to NeoForge's net.minecraftforge.api.distmarker.OnlyIn.
     */
    @OnlyIn(Dist.CLIENT)
    protected Object objArmor;

    /**
     * TODO port:1.20.1 - Was SceneNode&lt;?, ?&gt; (Phase 7).
     */
    protected fr.dynamx.client.renders.scene.node.SceneNode<?, ?> sceneNode;

    public ArmorObject(String packName, String fileName) {
        super(packName, fileName);
    }

    /**
     * TODO port:1.20.1 - Original built a ModelObjArmor from DxModelRegistry. Both are Phase 7;
     *   stubbed until then.
     */
    public void initArmorModel() {
        // objArmor = new ModelObjArmor(this, DynamXContext.getDxModelRegistry().getModel(getModel()));
    }

    @OnlyIn(Dist.CLIENT)
    public Object getObjArmor() {
        return objArmor;
    }

    public MaterialVariantsInfo<?> getVariants() {
        return getSubPropertyByType(MaterialVariantsInfo.class);
    }

    /**
     * TODO port:1.20.1 - Original returned IModelTextureVariants from an ObjObjectRenderer parameter.
     *   Phase 7 type; signature relaxed to Object.
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
    protected IDynamXItem<T> createItem(InfoList<T> loader) {
        throw new IllegalArgumentException("Call createOwners !");
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public IDynamXItem<T>[] createItems(InfoList<T> loader) {
        // 1.20.1: ItemArmor + EnumHelper.addArmorMaterial are gone. We build a per-pack
        // ArmorMaterial implementation from the pack-file fields (Durability, Enchantability,
        // DamageReduction, EquipSound, Toughness) and create one DynamXItemArmor per configured
        // slot. DamageReduction order in pack files is [feet, legs, chest, head] (legacy mapping).
        List<IDynamXItem<T>> owners = new ArrayList<>();
        net.minecraft.world.item.ArmorMaterial material = buildArmorMaterial();
        if (armorFoot != null)
            owners.add((IDynamXItem) new fr.dynamx.common.items.DynamXItemArmor((ArmorObject) this, material, EquipmentSlot.FEET));
        if (armorLegs != null)
            owners.add((IDynamXItem) new fr.dynamx.common.items.DynamXItemArmor((ArmorObject) this, material, EquipmentSlot.LEGS));
        if (armorBody != null || armorArms != null)
            owners.add((IDynamXItem) new fr.dynamx.common.items.DynamXItemArmor((ArmorObject) this, material, EquipmentSlot.CHEST));
        if (armorHead != null)
            owners.add((IDynamXItem) new fr.dynamx.common.items.DynamXItemArmor((ArmorObject) this, material, EquipmentSlot.HEAD));
        if (owners.isEmpty())
            DynamXErrorManager.addPackError(getPackName(), "armor_error", ErrorLevel.FATAL, getName(),
                    "Armor " + getFullName() + " has no slot configured (ArmorHead/Body/Arms/Legs/Foot)");
        this.items = owners.toArray(new IDynamXItem[0]);
        return this.items;
    }

    /**
     * Builds an {@link net.minecraft.world.item.ArmorMaterial} from this armor's pack-file fields.
     * The legacy code obtained one through {@code EnumHelper.addArmorMaterial}; 1.20.1 only requires
     * an interface implementation, so we synthesize one per armor.
     */
    private net.minecraft.world.item.ArmorMaterial buildArmorMaterial() {
        final String materialName = (getPackName() + "." + getName()).toLowerCase().replace('.', '_');
        final int dur = durability;
        final int ench = enchantibility;
        final float tough = toughness;
        final SoundEvent equipSound = sound;
        final int[] reductions = reductionAmount != null && reductionAmount.length >= 4
                ? reductionAmount : new int[]{1, 2, 3, 1};
        return new net.minecraft.world.item.ArmorMaterial() {
            @Override
            public int getDurabilityForType(net.minecraft.world.item.ArmorItem.Type type) {
                // Legacy multiplied the base durability per slot like vanilla materials do.
                int[] multipliers = {13, 15, 16, 11};
                return dur * multipliers[type.ordinal()];
            }

            @Override
            public int getDefenseForType(net.minecraft.world.item.ArmorItem.Type type) {
                // Pack DamageReduction is stored as [feet, legs, chest, head]; ArmorItem.Type
                // ordering is HELMET, CHESTPLATE, LEGGINGS, BOOTS.
                switch (type) {
                    case BOOTS: return reductions[0];
                    case LEGGINGS: return reductions[1];
                    case CHESTPLATE: return reductions[2];
                    case HELMET: return reductions[3];
                    default: return 0;
                }
            }

            @Override
            public int getEnchantmentValue() { return ench; }

            @Override
            public SoundEvent getEquipSound() { return equipSound; }

            @Override
            public net.minecraft.world.item.crafting.Ingredient getRepairIngredient() {
                return net.minecraft.world.item.crafting.Ingredient.EMPTY;
            }

            @Override
            public String getName() { return materialName; }

            @Override
            public float getToughness() { return tough; }

            @Override
            public float getKnockbackResistance() { return 0f; }
        };
    }

    @Override
    public boolean postLoad(boolean hot) {
        if (hot && FMLEnvironment.dist.isClient())
            initArmorModel();
        if (texturesArray != null)
            new MaterialVariantsInfo(this, texturesArray).appendTo(this);
        return super.postLoad(hot);
    }

    @Override
    public String getTranslationKey(IDynamXItem<T> item, int itemMeta) {
        // TODO port:1.20.1 - Original cast item to DynamXItemArmor<T> and inserted slot.getName().
        //   DynamXItemArmor is Phase 6 - the cast/lookup is stubbed to fall back on the base name.
        EquipmentSlot slot = getSlotFor(item);
        String slotName = slot != null ? slot.getName().toLowerCase() : "armor";
        if (itemMeta == 0 || getVariants() == null)
            return super.getTranslationKey(item, itemMeta) + "_" + slotName;
        // TODO port:1.20.1 - getVariant returns Object pending TextureVariantData port.
        return super.getTranslationKey(item, itemMeta) + "_" + slotName + "_" + String.valueOf(getVariants().getVariant((byte) itemMeta));
    }

    @Override
    public String getTranslatedName(IDynamXItem<T> item, int itemMeta) {
        String prefix = "";
        EquipmentSlot slot = getSlotFor(item);
        if (slot != null) {
            switch (slot) {
                case FEET:
                    prefix = "Chaussures de";
                    break;
                case LEGS:
                    prefix = "Pantalon de";
                    break;
                case CHEST:
                    prefix = "T-shirt de";
                    break;
                case HEAD:
                    prefix = "Casque de";
                    break;
                default:
                    break;
            }
        }
        if (itemMeta == 0 || getVariants() == null)
            return prefix + " " + super.getTranslatedName(item, itemMeta);
        return prefix + " " + super.getTranslatedName(item, itemMeta) + "_" + String.valueOf(getVariants().getVariant((byte) itemMeta));
    }

    @SuppressWarnings({"rawtypes"})
    private EquipmentSlot getSlotFor(IDynamXItem<T> item) {
        if (item instanceof fr.dynamx.common.items.DynamXItemArmor) {
            return ((fr.dynamx.common.items.DynamXItemArmor) item).armorType;
        }
        return null;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public fr.dynamx.client.renders.scene.node.SceneNode<?, ?> getSceneGraph() {
        if (sceneNode == null) {
            fr.dynamx.client.renders.scene.SceneBuilder<
                    fr.dynamx.client.renders.scene.BaseRenderContext.ArmorRenderContext,
                    ArmorObject<T>> builder = new fr.dynamx.client.renders.scene.SceneBuilder<>();
            sceneNode = builder.buildArmorSceneGraph((ArmorObject<T>) this,
                    (java.util.List) getDrawableParts(),
                    new com.jme3.math.Vector3f(1, 1, 1));
        }
        return sceneNode;
    }

    @Override
    public float getBaseItemScale() {
        return 0.7f;
    }

    @Override
    public String toString() {
        return "ArmorObject named " + getFullName();
    }
}
