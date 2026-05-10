package fr.dynamx.common.items.tools;

import fr.dynamx.common.items.DynamXItemRegistry;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * TODO port:1.20.1 - Wrench item:
 *  - {@code onLeftClickEntity} -> NeoForge's {@code IItemExtension#onLeftClickEntity} is still callable; signature
 *    unchanged in spirit but with new types.
 *  - {@code onItemRightClick(...)} -> {@link #use(Level, Player, InteractionHand)}.
 *  - {@code player.world} -> {@link Player#level()}.
 *  - {@code ACsGuiApi.asyncLoadThenShowGui(...)} relies on fr.aym.acsguis (not ported); GUI open call stubbed.
 *  - {@code I18n.format(...)} -> {@link I18n#get(String, Object...)}.
 *  - {@code PhysicsEntity / PackPhysicsEntity} are Phase 6 (not yet ported); related helpers (writeEntity, getEntity,
 *    hasEntity, removeEntity, interact) keep their public signatures but operate on raw int ids / {@link Entity}.
 *  - {@code Constants.NBT.TAG_INT} -> {@link Tag#TAG_INT}.
 *  - {@code stack.hasTagCompound() / getTagCompound() / setTagCompound()} -> {@link ItemStack#getOrCreateTag()} +
 *    {@link ItemStack#getTag()}.
 */
public class ItemWrench extends Item {

    public ItemWrench() {
        super(new Properties().stacksTo(1));
        // TODO port:1.20.1 - setRegistryName / setTranslationKey / setCreativeTab removed; DeferredRegister handles it.
        DynamXItemRegistry.add(this);
    }

    /**
     * TODO port:1.20.1 - NeoForge IItemExtension#onLeftClickEntity signature: returns boolean, signature unchanged.
     *  Method kept as an override-style helper; actual interaction with PhysicsEntity is delegated to WrenchMode.
     */
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.level().isClientSide) {
            WrenchMode.getCurrentMode(stack).onWrenchLeftClickEntity(stack, player, entity);
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        if (worldIn.isClientSide) {
            if (playerIn.isShiftKeyDown()) {
                // TODO port:1.20.1 - ACsGuiApi.asyncLoadThenShowGui / GuiWrenchSelection rely on fr.aym.acsguis (not ported)
                //  and fr.dynamx.client.gui (Phase 8). GUI open call stubbed pending the client phase.
                //  ACsGuiApi.asyncLoadThenShowGui("wrench_gui", GuiWrenchSelection::new);
            }
        }
        if (!worldIn.isClientSide) {
            WrenchMode.getCurrentMode(playerIn.getItemInHand(handIn)).onWrenchRightClick(playerIn, handIn);
        } else
            WrenchMode.getCurrentMode(playerIn.getItemInHand(handIn)).onWrenchRightClickClient(playerIn, handIn);
        return super.use(worldIn, playerIn, handIn);
    }

    /**
     * TODO port:1.20.1 - {@code PhysicsEntity<?>} is Phase 6; parameter relaxed to {@link Entity}. The stored value is
     *  still the entity's int id.
     */
    public static void writeEntity(ItemStack stack, Entity entity) {
        stack.getOrCreateTag().putInt("Entity1", entity.getId());
    }

    public static boolean hasEntity(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("Entity1", Tag.TAG_INT);
    }

    public static void removeEntity(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove("Entity1");
        }
    }

    /**
     * TODO port:1.20.1 - Return type was {@code PhysicsEntity<?>} (Phase 6); relaxed to {@link Entity}.
     *  {@code World#getEntityByID} -> {@link Level#getEntity(int)}.
     */
    @Nullable
    public static Entity getEntity(ItemStack stack, Level world) {
        if (hasEntity(stack)) {
            Entity e = world.getEntity(stack.getTag().getInt("Entity1"));
            // TODO port:1.20.1 - When PhysicsEntity is ported, restore the type-check that returned null for non-physics entities.
            return e;
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        tooltip.add(Component.literal(I18n.get("wrench.mode.mode", I18n.get(WrenchMode.getCurrentMode(stack).getLabel()))));
        if (hasEntity(stack) && worldIn != null) {
            Entity e = getEntity(stack, worldIn);
            // TODO port:1.20.1 - PackPhysicsEntity tooltip variant deferred to Phase 6.
            if (e != null) {
                tooltip.add(Component.literal("Linked entity " + e.getName().getString()));
            }
        }
    }

    /**
     * TODO port:1.20.1 - parameter was {@code PhysicsEntity<?>}; relaxed to {@link Object} until Phase 6.
     */
    public void interact(Player context, Object physicsEntity) {
        WrenchMode.getCurrentMode(context.getMainHandItem()).onInteractWithEntity(context, physicsEntity, context.isShiftKeyDown());
    }
}
