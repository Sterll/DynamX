package fr.dynamx.common.network.packets;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.client.gui.NewGuiDnxDebug;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;

public class MessageOpenDebugGui implements IDnxPacket {
    private byte action;

    public MessageOpenDebugGui() {
    }

    public MessageOpenDebugGui(byte action) {
        this.action = action;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
    }

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        if (side != LogicalSide.CLIENT) {
            return;
        }
        if (action == 125) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ACsGuiApi.asyncLoadThenShowGui("Dnx Debug", NewGuiDnxDebug::new));
        }
    }
}
