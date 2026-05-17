package fr.dynamx.common.network.udp.auth;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.network.DynamXClientNetworkSystem;
import fr.dynamx.utils.DynamXConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class MessageDynamXUdpSettings implements IDnxPacket {
    private int voiceServerType;
    private int udpPort;
    private String hash;
    private String ip;
    private boolean syncDynamXPacks;

    public MessageDynamXUdpSettings() {
    }

    public MessageDynamXUdpSettings(int voiceServerType, int udpPort, String hash, String ip, boolean syncDynamXPacks) {
        this.voiceServerType = voiceServerType;
        this.udpPort = udpPort;
        this.hash = hash;
        this.ip = ip;
        this.syncDynamXPacks = syncDynamXPacks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        if (DynamXConfig.udpDebug)
            DynamXMain.log.info("[UDP-DEBUG] Read auth proposal");
        FriendlyByteBuf fb = (buf instanceof FriendlyByteBuf) ? (FriendlyByteBuf) buf : new FriendlyByteBuf(buf);
        this.voiceServerType = fb.readInt();
        this.udpPort = fb.readInt();
        this.hash = fb.readUtf();
        this.ip = fb.readUtf();
        this.syncDynamXPacks = fb.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        if (DynamXConfig.udpDebug)
            DynamXMain.log.info("[UDP-DEBUG] Write auth proposal");
        FriendlyByteBuf fb = (buf instanceof FriendlyByteBuf) ? (FriendlyByteBuf) buf : new FriendlyByteBuf(buf);
        fb.writeInt(this.voiceServerType);
        fb.writeInt(this.udpPort);
        fb.writeUtf(this.hash);
        fb.writeUtf(this.ip);
        fb.writeBoolean(this.syncDynamXPacks);
    }

    public int getVoiceServerType() {
        return voiceServerType;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public String getHash() {
        return hash;
    }

    public String getIp() {
        return ip;
    }

    public boolean isSyncDynamXPacks() {
        return syncDynamXPacks;
    }

    public static void handle(MessageDynamXUdpSettings packet) {
        if (DynamXConfig.udpDebug)
            DynamXMain.log.info("[UDP-DEBUG] Received auth proposal");
        DynamXMain.log.info("Received udp auth proposal. PackSync is {}.", packet.syncDynamXPacks);
        DynamXConfig.syncPacks = packet.syncDynamXPacks;
        if (FMLEnvironment.dist == Dist.CLIENT) {
            Minecraft.getInstance().execute(() ->
                    ((DynamXClientNetworkSystem) DynamXContext.getNetwork()).startNetwork(
                            EnumNetworkType.values()[packet.voiceServerType], packet.hash, packet.ip, packet.udpPort));
        }
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
