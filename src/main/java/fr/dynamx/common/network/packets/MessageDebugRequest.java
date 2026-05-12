package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.items.tools.ItemSlopes;
import fr.dynamx.common.items.tools.ItemWrench;
import fr.dynamx.common.items.tools.WrenchMode;
import fr.dynamx.common.physics.PhysicsTickHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.LogicalSide;

public class MessageDebugRequest implements IDnxPacket {
    public int debugMode;

    public MessageDebugRequest() {
    }

    /**
     * @param debugMode A terrain debug mode id, OR -15815 to switch slopes item mode, OR -15816 to switch wrench item mode
     */
    public MessageDebugRequest(int debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(debugMode);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        debugMode = buf.readInt();
    }

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        if (side != LogicalSide.SERVER || !(context instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (debugMode == -15815) {
            if (stack.getItem() instanceof ItemSlopes) {
                CompoundTag tag = stack.getOrCreateTag();
                int mode = tag.getInt("mode");
                if (mode == 0) {
                    tag.putInt("mode", 1);
                    player.sendSystemMessage(Component.literal(ChatFormatting.GREEN + "Mode mis à [CREATE]"));
                } else if (mode == 1) {
                    tag.putInt("mode", 2);
                    player.sendSystemMessage(Component.literal(ChatFormatting.GOLD + "Mode mis à [AUTO]"));
                } else {
                    tag.putInt("mode", 0);
                    player.sendSystemMessage(Component.literal(ChatFormatting.LIGHT_PURPLE + "Mode mis à [DELETE]"));
                }
            }
            return;
        }
        if (debugMode == -15816) {
            if (stack.getItem() instanceof ItemWrench) {
                WrenchMode.switchMode(player, stack);
            }
            return;
        }
        if (debugMode <= -15817) {
            int mode = (debugMode + 15817) * -1;
            if (stack.getItem() instanceof ItemWrench) {
                WrenchMode.setMode(player, stack, mode);
            }
            return;
        }
        if (player.hasPermissions(2)) {
            PhysicsTickHandler.requestedDebugInfo.put(player, debugMode);
        } else {
            DynamXMain.log.warn(player + " tried to enable debug mode " + debugMode + " while not in the debug gui");
            player.sendSystemMessage(Component.literal("[DynamX] You are not allowed to use server debug !"));
        }
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
