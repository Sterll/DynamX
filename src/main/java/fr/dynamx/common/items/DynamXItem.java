package fr.dynamx.common.items;

import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * TODO port:1.20.1 - Item registration model rewritten in 1.20.1.
 *  - `setRegistryName(...)`, `setTranslationKey(...)`, `setCreativeTab(...)`, `setMaxStackSize(...)` no longer exist on
 *    {@link Item} instances. They must be supplied via {@link Item.Properties} at construction time, and registry names
 *    come from {@link net.minecraftforge.registries.DeferredRegister}. The 1.12 constructors are preserved here
 *    so existing call sites compile, but the actual registration is stubbed and will be wired up in the entry-point
 *    phase.
 *  - `addInformation(...)` is replaced by `appendHoverText(...)` returning {@link Component} instead of {@link String};
 *    the existing tooltip helper still produces {@link String}, so we convert each line to {@link Component#literal}.
 *  - `ItemStack#getMetadata()` is gone — use NBT/data-components. Variant byte is forwarded as 0 until the
 *    metadata model migration lands.
 */
public class DynamXItem<T extends AbstractItemObject<?, ?>> extends Item implements IDynamXItem<T>, IResourcesOwner {
    protected T itemInfo;

    /**
     * Use the other constructor to create custom blocks and easily set BlockObject's properties
     */
    public DynamXItem(T itemInfo) {
        super(new Properties().stacksTo(itemInfo.getMaxItemStackSize()));
        this.setInfo(itemInfo);
        // TODO port:1.20.1 - setRegistryName / setTranslationKey / setCreativeTab were removed; registration is now
        //  handled via DeferredRegister in the entry-point phase. Registry id was previously
        //  DynamXConstants.ID + ":" + itemInfo.getFullName().toLowerCase().
        DynamXItemRegistry.add(this);
    }

    /**
     * Use this constructor to create a custom item having the same functionalities as pack item <br>
     * You can customise item properties using this.getInfo() <br> <br>
     * NOTE : Registry name and translation key are automatically set and the item is automatically registered into Forge by DynamX,
     * but don't forget to set a creative tab !<br><br>
     *
     * <strong>NOTE : Should be called during addons initialization</strong>
     *
     * @param modid    The mod owning this item used to register the item
     * @param itemName The name of the item
     * @param model    The obj model of the block "namespace:resourceName.obj"
     */
    @SuppressWarnings("unchecked")
    public DynamXItem(String modid, String itemName, ResourceLocation model) {
        super(new Properties());
        if (modid.contains("builtin_mod_")) { //Backward-compatibility
            itemInfo = (T) DynamXObjectLoaders.ITEMS.addBuiltinObject(this, modid, itemName);
            modid = modid.replace("builtin_mod_", "");
        } else {
            itemInfo = (T) DynamXObjectLoaders.ITEMS.addBuiltinObject(this, "dynx." + modid, itemName);
        }
        itemInfo.setModel(model);
        itemInfo.setDescription("Builtin " + modid + "'s item");
        // TODO port:1.20.1 - setRegistryName / setTranslationKey / setMaxStackSize removed. Stack size must be passed
        //  through Item.Properties at construction time; we cannot reapply it post-hoc here. Pending DeferredRegister
        //  migration.
        DynamXItemRegistry.add(this);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        // TODO port:1.20.1 - addItemTooltip(...) still operates on List<String>; bridge by collecting and converting.
        java.util.List<String> raw = new java.util.ArrayList<>();
        DynamXUtils.addItemTooltip(raw, getInfo(), (byte) 0); // metadata replaced by NBT/components in 1.20.1
        raw.forEach(line -> tooltip.add(Component.literal(line)));
    }

    public T getInfo() {
        return itemInfo;
    }

    public void setInfo(T itemInfo) {
        this.itemInfo = itemInfo;
        // TODO port:1.20.1 - setMaxStackSize was removed; pass via Item.Properties#stacksTo at construction.
    }

    @Override
    public String toString() {
        return "DynamXItem{" +
                "itemInfo=" + getInfo().getFullName() +
                '}';
    }

    @Override
    public String getJsonName(int meta) {
        return getInfo().getFullName().toLowerCase();
    }

    @Override
    public IModelPackObject getDxModel() {
        return getInfo();
    }

    @Override
    public boolean createJson() {
        return IResourcesOwner.super.createJson() || itemInfo.get3DItemRenderLocation() != Enum3DRenderLocation.ALL;
    }
}
