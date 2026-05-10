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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;

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
     *   to NeoForge's net.neoforged.api.distmarker.OnlyIn.
     */
    @OnlyIn(Dist.CLIENT)
    protected Object objArmor;

    /**
     * TODO port:1.20.1 - Was SceneNode&lt;?, ?&gt; (Phase 7).
     */
    protected Object sceneNode;

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
        // TODO port:1.20.1 - Original used ItemArmor.ArmorMaterial + EnumHelper.addArmorMaterial(...)
        //   (removed in 1.20.1) and DynamXItemArmor (Phase 6). Building items requires the new
        //   ArmorMaterial / Holder<ArmorMaterial> registry plus the DynamXItemArmor port.
        //   Stub: leaves items empty and logs a fatal error if no armor slot was configured, mirroring
        //   the legacy validation.
        List<IDynamXItem<T>> owners = new ArrayList<>();
        if (owners.isEmpty())
            DynamXErrorManager.addPackError(getPackName(), "armor_error", ErrorLevel.FATAL, getName(),
                    "Armor items cannot be created yet (Phase 6 not ported)");
        this.items = owners.toArray(new IDynamXItem[0]);
        return this.items;
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
        return super.getTranslationKey(item, itemMeta) + "_" + slotName + "_" + getVariants().getVariant((byte) itemMeta).getName();
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
        return prefix + " " + super.getTranslatedName(item, itemMeta) + "_" + getVariants().getVariant((byte) itemMeta).getName();
    }

    /**
     * TODO port:1.20.1 - Helper that originally cast item to DynamXItemArmor and read its armorType.
     *   Without DynamXItemArmor (Phase 6) we cannot resolve the slot; returns null.
     */
    private EquipmentSlot getSlotFor(IDynamXItem<T> item) {
        return null;
    }

    @Override
    public Object getSceneGraph() {
        // TODO port:1.20.1 - Original posted BuildArmorScene event and returned an ArmorNode. Phase 7.
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
