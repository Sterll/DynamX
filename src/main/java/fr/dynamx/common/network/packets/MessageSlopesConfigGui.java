package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.items.tools.ItemSlopes;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.LogicalSide;

public class MessageSlopesConfigGui implements IDnxPacket {
    private CompoundTag serializedConfig;

    public MessageSlopesConfigGui() {
    }

    public MessageSlopesConfigGui(CompoundTag serializedConfig) {
        this.serializedConfig = serializedConfig;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void fromBytes(ByteBuf byteBuf) {
        FriendlyByteBuf fb = (byteBuf instanceof FriendlyByteBuf) ? (FriendlyByteBuf) byteBuf : new FriendlyByteBuf(byteBuf);
        // TODO port:1.20.1 - ByteBufUtils.readTag → FriendlyByteBuf#readNbt.
        serializedConfig = fb.readNbt();
    }

    @Override
    public void toBytes(ByteBuf byteBuf) {
        FriendlyByteBuf fb = (byteBuf instanceof FriendlyByteBuf) ? (FriendlyByteBuf) byteBuf : new FriendlyByteBuf(byteBuf);
        // TODO port:1.20.1 - ByteBufUtils.writeTag → FriendlyByteBuf#writeNbt.
        fb.writeNbt(serializedConfig);
    }

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        if (side != LogicalSide.SERVER || context == null) {
            return;
        }
        ItemStack stack = context.getMainHandItem();
        if (stack.getItem() instanceof ItemSlopes) {
            stack.getOrCreateTag().put("ptconfig", serializedConfig);
        }
    }
}
