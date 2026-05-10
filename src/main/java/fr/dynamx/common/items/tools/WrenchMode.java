package fr.dynamx.common.items.tools;

import fr.dynamx.utils.DynamXConfig;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO port:1.20.1 - WrenchMode hub:
 *  - {@code TextFormatting} -> {@link ChatFormatting}.
 *  - {@code EntityPlayer} -> {@link Player}; {@code Entity} package moved to {@code net.minecraft.world.entity}.
 *  - {@code TextComponentString} / {@code TextComponentTranslation} -> {@code Component.literal(...)} /
 *    {@code Component.translatable(...)}.
 *  - {@code player.world} -> {@link Player#level()}.
 *  - {@code stack.hasTagCompound() / getTagCompound() / setTagCompound()} -> {@link ItemStack#getOrCreateTag()} +
 *    {@link ItemStack#getTag()} (data-component migration to land later).
 *  - {@code player.capabilities.isCreativeMode} -> {@link Player#getAbilities()}{@code .instabuild}.
 *  - {@code player.sendMessage(component)} -> {@link Player#sendSystemMessage(Component)} (server-side) or
 *    {@link Player#displayClientMessage(Component, boolean)} (client-side).
 *  - {@code ObfuscationReflectionHelper#findConstructor} -> {@code ObfuscationReflectionHelper#findConstructor}
 *    (still available under net.minecraftforge.fml in 1.20.1, but path differs). The ReplaceEntitiesWrenchMode
 *    body uses reflection on PhysicsEntity which is Phase 6 — stubbed.
 *  - {@code DynamXContext.getNetwork().sendToServer(new MessageDebugRequest(...))}, packet types, joint registry,
 *    physics handler, ItemProps spawn — all forward-references; the action bodies are stubbed pending Phases 4-6.
 */
public class WrenchMode {
    private static final List<WrenchMode> WRENCH_MODES = new ArrayList<>();

    public static final WrenchMode NONE = new WrenchMode("none", ChatFormatting.RED);
    public static final WrenchMode CHANGE_TEXTURE = new ChangeTextureWrenchMode();
    public static final WrenchMode ATTACH_TRAILERS = new AttachTrailersWrenchMode();
    public static final WrenchMode ATTACH_OBJECTS = new AttachObjectsWrenchMode();
    public static final WrenchMode REPLACE_ENTITIES = new ReplaceEntitiesWrenchMode();
    public static final WrenchMode ENTITY_SEAT_MODE = new EntitySeatWrenchMode();
    public static final WrenchMode LAUNCH_ENTITIES = new WrenchMode("launch_entities", ChatFormatting.GOLD) {
        @Override
        public void onWrenchRightClick(Player playerIn, InteractionHand handIn) {
            // TODO port:1.20.1 - ItemProps spawn + physics handler velocity injection depends on Phase 4-6 ports.
            //  Once available:
            //   if (!playerIn.level().isClientSide) {
            //       ItemStack itemOffhand = playerIn.getOffhandItem();
            //       Item item = itemOffhand.getItem();
            //       if (item instanceof ItemProps) {
            //           PropsEntity<?> spawnEntity = ((ItemProps<?>) item).getSpawnEntity(playerIn.level(), playerIn,
            //               Vector3fPool.get(playerIn.getX(), playerIn.getY() + 1.25, playerIn.getZ()),
            //               playerIn.getYRot() % 360.0F, 0);
            //           playerIn.level().addFreshEntity(spawnEntity);
            //           spawnEntity.setPhysicsInitCallback((modularEntity, physicsHandler) ->
            //               physicsHandler.setLinearVelocity(DynamXUtils.toVector3f(playerIn.getLookAngle()).multLocal(20)));
            //       }
            //   }
        }
    };

    private final String label;
    @Getter
    private final String initials;

    protected WrenchMode(String label, ChatFormatting color) {
        this.label = label;
        this.initials = Arrays.stream(label.split("_")).map(s -> s.substring(0, 1) + '.').collect(Collectors.joining("", color.toString(), "")).toUpperCase();
        WRENCH_MODES.add(this);
    }

    public String getLabel() {
        return "wrench.mode." + label;
    }

    public String getMessage() {
        return "wrench.mode.set." + label;
    }

    public void onWrenchLeftClickEntity(ItemStack stack, Player player, Entity entity) {
    }

    public void onWrenchRightClick(Player playerIn, InteractionHand handIn) {
    }

    public void onWrenchRightClickClient(Player playerIn, InteractionHand handIn) {
    }

    /**
     * TODO port:1.20.1 - The {@code PhysicsEntity<?>} type is from Phase 6 (not yet ported); parameter relaxed to
     *  {@link Object}.
     */
    public void onInteractWithEntity(Player player, Object targetEntity, boolean isSneaking) {
    }

    public static List<WrenchMode> getWrenchModes() {
        return WRENCH_MODES;
    }

    public static void switchMode(Player player, ItemStack s) {
        CompoundTag tag = s.getOrCreateTag();
        int l = tag.getInt("mode") + 1;
        if (l >= WRENCH_MODES.size()) {
            l = 0;
        }
        tag.putInt("mode", l);
        if (!player.getAbilities().instabuild) {
            boolean allowed = false;
            for (int i = 0; i < DynamXConfig.allowedWrenchModes.length; i++) {
                if (DynamXConfig.allowedWrenchModes[i] == l) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                switchMode(player, s);
                return;
            }
        }
        player.sendSystemMessage(Component.translatable(WRENCH_MODES.get(l).getMessage()));
    }

    public static void setMode(Player player, ItemStack s, int mode) {
        CompoundTag tag = s.getOrCreateTag();
        if (mode >= WRENCH_MODES.size()) {
            mode = 0;
        }
        tag.putInt("mode", mode);
        if (!player.getAbilities().instabuild) {
            boolean allowed = false;
            for (int i = 0; i < DynamXConfig.allowedWrenchModes.length; i++) {
                if (DynamXConfig.allowedWrenchModes[i] == mode) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                switchMode(player, s);
                return;
            }
        }
        player.sendSystemMessage(Component.translatable(WRENCH_MODES.get(mode).getMessage()));
    }

    public static void sendWrenchMode(WrenchMode mode) {
        int index = 0;
        for (int i = 0; i < WRENCH_MODES.size(); i++) {
            if (WRENCH_MODES.get(i) == mode) {
                index = i;
                break;
            }
        }
        // TODO port:1.20.1 - DynamXContext + MessageDebugRequest (network) are Phase 4-5 ports; stubbed.
        //  DynamXContext.getNetwork().sendToServer(new MessageDebugRequest(-15817 - index));
    }

    public static WrenchMode getCurrentMode(ItemStack s) {
        CompoundTag tag = s.getTag();
        if (tag != null) {
            int l = tag.getInt("mode");
            return l < WRENCH_MODES.size() ? WRENCH_MODES.get(l) : NONE;
        }
        return NONE;
    }

    public static boolean isCurrentMode(ItemStack stack, WrenchMode mode) {
        return getCurrentMode(stack) == mode;
    }

    private static class AttachObjectsWrenchMode extends WrenchMode {
        public AttachObjectsWrenchMode() {
            super("attach_objects", ChatFormatting.RED);
        }

        @Override
        public void onWrenchLeftClickEntity(ItemStack stack, Player player, Entity entity) {
            act(player, false);
        }

        @Override
        public void onInteractWithEntity(Player player, Object targetEntity, boolean isSneaking) {
            act(player, true);
        }

        private void act(Player player, boolean shouldWeldObjects) {
            // TODO port:1.20.1 - This action chains through PhysicsRaycastResult, BulletShapeType, MovableModule,
            //  JointHandlerRegistry, IPhysicsWorld — all in Phase 4-6 packages (common/physics not fully ported and
            //  common/entities not yet ported). Body stubbed; will be reimplemented once dependencies land.
        }
    }

    private static class AttachTrailersWrenchMode extends WrenchMode {
        public AttachTrailersWrenchMode() {
            super("attach_trailers", ChatFormatting.GREEN);
        }

        @Override
        public void onInteractWithEntity(Player player, Object physicsEntity, boolean isSneaking) {
            // TODO port:1.20.1 - BaseVehicleEntity / CarEntity / TrailerEntity / TrailerAttachModule and
            //  DynamXUtils.attachTrailer are Phase 6 ports. Body stubbed pending entity port.
        }
    }

    private static class ReplaceEntitiesWrenchMode extends WrenchMode {
        public ReplaceEntitiesWrenchMode() {
            super("respawn_entities", ChatFormatting.GOLD);
        }

        @Override
        public void onInteractWithEntity(Player context, Object physicsEntity, boolean isSneaking) {
            // TODO port:1.20.1 - PhysicsEntity respawn logic relies on writeToNBT/readFromNBT + ObfuscationReflectionHelper
            //  + TaskScheduler — all Phase 4-6 dependencies. Body stubbed.
            //  Notes for the port:
            //    - net.minecraft.world.level.Level constructor parameter for the reflected ctor stays Level.class.
            //    - entity.setDead() -> entity.discard().
            //    - posX/Y/Z -> getX()/getY()/getZ().
            //    - rotationYaw -> getYRot().
            //    - ObfuscationReflectionHelper now lives under net.minecraftforge.fml.util.ObfuscationReflectionHelper.
        }
    }

    private static class ChangeTextureWrenchMode extends WrenchMode {
        public ChangeTextureWrenchMode() {
            super("change_skins", ChatFormatting.BLUE);
        }

        @Override
        public void onInteractWithEntity(Player player, Object targetEntity, boolean isSneaking) {
            // TODO port:1.20.1 - BaseVehicleEntity#getMetadata/setMetadata is Phase 6. Body stubbed.
        }
    }

    private static class EntitySeatWrenchMode extends WrenchMode {
        public EntitySeatWrenchMode() {
            super("entity_seat", ChatFormatting.LIGHT_PURPLE);
        }

        HashMap<Player, Entity> playerEntityHashMap = new HashMap<>();

        @Override
        public void onWrenchLeftClickEntity(ItemStack stack, Player player, Entity entity) {
            // TODO port:1.20.1 - BaseVehicleEntity check needs Phase 6; for now we store any entity for later seat-mount.
            playerEntityHashMap.put(player, entity);
            player.sendSystemMessage(Component.literal("Entity selected: " + entity.getName().getString()));
        }

        @Override
        public void onInteractWithEntity(Player context, Object physicsEntity, boolean isSneaking) {
            // TODO port:1.20.1 - BasePartSeat / SeatsModule / IModuleContainer.ISeatsContainer are Phase 4-6. Body stubbed.
            playerEntityHashMap.remove(context);
        }
    }
}
