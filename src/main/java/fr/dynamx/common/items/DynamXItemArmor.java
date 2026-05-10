package fr.dynamx.common.items;

import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import fr.dynamx.utils.DynamXUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

/**
 * TODO port:1.20.1 - Armor in 1.20.1:
 *  - {@code ItemArmor} -> {@link ArmorItem}; constructor now takes {@link ArmorItem.Type} instead of int + slot.
 *  - {@link EquipmentSlot} is now the slot enum; for ArmorItem use {@code ArmorItem.Type} which carries the slot.
 *  - {@code setHasSubtypes(true)} + {@code setMaxDamage(0)} are gone; variants must move to data-components/NBT.
 *  - {@code getSubItems(...)} removed; creative-tab population now goes through CreativeModeTabRegistryEvent.
 *  - {@code getArmorModel} / {@code getArmorTexture} hooks now live on the NeoForge client extension layer
 *    ({@code IClientItemExtensions}); both are stubbed below pending the client phase.
 *  - We forward {@link EquipmentSlot} where the legacy code used {@code EntityEquipmentSlot}; the constructor
 *    derives {@code ArmorItem.Type} from the slot.
 */
public class DynamXItemArmor<T extends ArmorObject<?>> extends ArmorItem implements IDynamXItem<T>, IResourcesOwner {
    protected final int textureNum;
    protected T armorInfo;
    // Preserves the legacy armor slot field name for downstream code that referenced `armorType`.
    protected final EquipmentSlot armorType;

    public DynamXItemArmor(T armorInfo, ArmorMaterial material, EquipmentSlot armorType) {
        super(material, slotToType(armorType), new Properties());
        this.armorType = armorType;
        this.armorInfo = armorInfo;
        // TODO port:1.20.1 - setRegistryName / setTranslationKey / setCreativeTab removed; handled by DeferredRegister.
        DynamXItemRegistry.add(this);
        textureNum = armorInfo.getMaxVariantId();
        // TODO port:1.20.1 - setHasSubtypes(true) / setMaxDamage(0) removed. Variants live in data-components now.
    }

    /**
     * Use this constructor to create a custom armor item having the same functionalities as pack armor <br>
     * You can customise custom properties using this.getInfo() <br> <br>
     * NOTE : Registry name and translation key are automatically set and the item is automatically registered into Forge by DynamX,
     * but don't forget to set a creative tab !<br><br>
     *
     * <strong>NOTE : Should be called during addons initialization</strong>
     *
     * @param modid     The mod owning this item used to register the item
     * @param itemName  The name of the item
     * @param model     The obj model of the block "namespace:resourceName.obj"
     * @param material  The armor material
     * @param armorType The armor type
     */
    @SuppressWarnings("unchecked")
    public DynamXItemArmor(String modid, String itemName, ResourceLocation model, ArmorMaterial material, EquipmentSlot armorType) {
        super(material, slotToType(armorType), new Properties());
        this.armorType = armorType;
        if (modid.contains("builtin_mod_")) { //Backward-compatibility
            armorInfo = (T) DynamXObjectLoaders.ARMORS.addBuiltinObject(this, modid, itemName);
            modid = modid.replace("builtin_mod_", "");
        } else {
            armorInfo = (T) DynamXObjectLoaders.ARMORS.addBuiltinObject(this, "dynx." + modid, itemName);
        }
        armorInfo.setModel(model);
        armorInfo.setDescription("Builtin " + modid + "'s armor");
        textureNum = 1;

        // TODO port:1.20.1 - setRegistryName / setTranslationKey removed; handled by DeferredRegister.
        DynamXItemRegistry.add(this);
    }

    private static ArmorItem.Type slotToType(EquipmentSlot slot) {
        switch (slot) {
            case HEAD: return Type.HELMET;
            case CHEST: return Type.CHESTPLATE;
            case LEGS: return Type.LEGGINGS;
            case FEET: return Type.BOOTS;
            default: return Type.CHESTPLATE; // fallback, legacy code never passed non-armor slots
        }
    }

    public T getInfo() {
        return armorInfo;
    }

    public void setInfo(T itemInfo) {
        this.armorInfo = itemInfo;
    }

    @Override
    public String getJsonName(int meta) {
        return getInfo().getFullName().toLowerCase() + "_" + armorType.getName() + "_" + getInfo().getMainObjectVariantName((byte) meta);
    }

    @Override
    public boolean createJson() {
        return getDxModel().get3DItemRenderLocation() != Enum3DRenderLocation.ALL;
    }

    // TODO port:1.20.1 - getSubItems removed; populate via CreativeModeTabRegistryEvent in the registry phase.
    /*
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            for (byte m = 0; m < textureNum; m++) {
                items.add(new ItemStack(this, 1, m));
            }
        }
    }
    */

    @Override
    public String getDescriptionId(ItemStack stack) {
        // TODO port:1.20.1 - stack.getMetadata() removed; variant byte is 0 until data-components migration lands.
        byte variant = 0;
        if (variant != 0 && textureNum > 1) {
            return super.getDescriptionId(stack) + "_" + getInfo().getMainObjectVariantName(variant);
        }
        return super.getDescriptionId(stack);
    }

    @Override
    public int getMaxMeta() {
        return textureNum;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        java.util.List<String> raw = new java.util.ArrayList<>();
        DynamXUtils.addItemTooltip(raw, getInfo(), (byte) 0); // TODO port:1.20.1 - was stack.getMetadata().
        raw.forEach(line -> tooltip.add(Component.literal(line)));
    }

    // TODO port:1.20.1 - getArmorTexture / getArmorModel hooks have moved to IClientItemExtensions in NeoForge 1.20.1.
    //  See net.minecraftforge.client.extensions.common.IClientItemExtensions#getHumanoidArmorModel and
    //  #getArmorTexture. Implementation deferred to the client phase.
    @Nullable
    @OnlyIn(Dist.CLIENT)
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return null; // TODO port:1.20.1
    }

    @Nullable
    @OnlyIn(Dist.CLIENT)
    public Object getArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot armorSlot, Object _default) {
        // TODO port:1.20.1 - ModelBiped -> HumanoidModel; armor model wiring deferred to the client phase.
        return null;
    }

    @Override
    public IModelPackObject getDxModel() {
        return getInfo();
    }
}
