package fr.dynamx.common.items;

import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.common.contentpack.parts.PartEntitySeat;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

/**
 * TODO port:1.20.1 - Modular entity spawner item:
 *  - {@code maxStackSize = 1} field assignment removed; must come from {@code Item.Properties#stacksTo(1)} at
 *    construction. The parent {@link DynamXItem} already routes stack size through Properties, but the legacy
 *    "force stack=1" needs to land in the constructor of the parent class once registration is rewired.
 *  - {@code setCreativeTab(...)} removed; creative-tab membership now flows through CreativeModeTabRegistryEvent.
 *  - {@code setHasSubtypes(true)} removed.
 *  - {@code getSubItems(...)} removed.
 *  - {@code addInformation(...)} -> {@link #appendHoverText(ItemStack, Level, List, TooltipFlag)} (List of Component).
 */
public abstract class ItemModularEntity extends DynamXItemSpawner<ModularVehicleInfo> implements IDynamXItem<ModularVehicleInfo> {
    private final int textureNum;

    public ItemModularEntity(ModularVehicleInfo modulableVehicleInfo) {
        super(modulableVehicleInfo);
        // TODO port:1.20.1 - maxStackSize must be set via Item.Properties#stacksTo(1) at construction; cannot reapply
        //  post-hoc here. Pending DeferredRegister migration.
        // TODO port:1.20.1 - setCreativeTab no longer exists; CreativeModeTab membership flows through events.

        textureNum = modulableVehicleInfo.getMaxVariantId();
        // TODO port:1.20.1 - setHasSubtypes removed; variants live in data-components/NBT.
    }

    @Override
    public String getJsonName(int meta) {
        return super.getJsonName(meta) + "_" + getInfo().getVariantName((byte) meta);
    }

    @Override
    public boolean createJson() {
        return getDxModel().get3DItemRenderLocation() != Enum3DRenderLocation.ALL;
    }

    // TODO port:1.20.1 - getSubItems removed; populate creative tabs via CreativeModeTabRegistryEvent.
    /*
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) { ... }
    */

    @Override
    public String getDescriptionId(ItemStack stack) {
        // TODO port:1.20.1 - stack.getMetadata() removed; variant byte placeholder 0 until data-components migration.
        byte variant = 0;
        if (variant != 0 && textureNum > variant) {
            return super.getDescriptionId(stack) + "_" + getInfo().getVariantName(variant);
        }
        return super.getDescriptionId(stack);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);

        if (flagIn.isAdvanced()) {
            getInfo().getPartsByType(PartWheel.class).forEach(vehicleWheelInfo -> tooltip.add(Component.literal("Wheel: " + vehicleWheelInfo.getDefaultWheelName())));

            int seats = getInfo().getPartsByType(PartEntitySeat.class).size();
            if (seats > 0) {
                tooltip.add(Component.literal(seats + " seats"));
            }
        }
    }

    @Override
    public int getMaxMeta() {
        return textureNum;
    }
}
