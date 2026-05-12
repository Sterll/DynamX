package fr.dynamx.common.network.packets;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;

public class MessageSwitchAutoSlopesMode implements IDnxPacket {
    public int mode;

    public MessageSwitchAutoSlopesMode(int mode) {
        this.mode = mode;
    }

    public MessageSwitchAutoSlopesMode() {
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        mode = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(mode);
    }

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        if (side == LogicalSide.CLIENT) {
            fr.dynamx.common.contentpack.ContentPackLoader.PLACE_SLOPES = mode == 1;
        }
    }
}
