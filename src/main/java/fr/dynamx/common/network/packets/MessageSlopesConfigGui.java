package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

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

    public static void handle(MessageSlopesConfigGui message /*, IPayloadContext ctx */) {
        // TODO port:1.20.1 - Original wrote NBT "ptconfig" into the player's held ItemSlopes stack.
        // Re-port using Player#getMainHandItem and stack.getOrCreateTag once ItemSlopes is ported.
    }
}
