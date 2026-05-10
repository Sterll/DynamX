package fr.dynamx.common.network.udp.auth;

import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.utils.DynamXConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;

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
        FriendlyByteBuf fb = (buf instanceof FriendlyByteBuf) ? (FriendlyByteBuf) buf : new FriendlyByteBuf(buf);
        if (DynamXConfig.udpDebug) {
            System.out.println("[UDP-DEBUG] Read auth proposal");
        }
        this.voiceServerType = fb.readInt();
        this.udpPort = fb.readInt();
        this.hash = fb.readUtf();
        this.ip = fb.readUtf();
        this.syncDynamXPacks = fb.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        FriendlyByteBuf fb = (buf instanceof FriendlyByteBuf) ? (FriendlyByteBuf) buf : new FriendlyByteBuf(buf);
        if (DynamXConfig.udpDebug) {
            System.out.println("[UDP-DEBUG] Write auth proposal");
        }
        fb.writeInt(this.voiceServerType);
        fb.writeInt(this.udpPort);
        fb.writeUtf(this.hash);
        fb.writeUtf(this.ip);
        fb.writeBoolean(this.syncDynamXPacks);
    }

    public int getVoiceServerType() { return voiceServerType; }
    public int getUdpPort() { return udpPort; }
    public String getHash() { return hash; }
    public String getIp() { return ip; }
    public boolean isSyncDynamXPacks() { return syncDynamXPacks; }

    public static void handle(MessageDynamXUdpSettings packet /*, IPayloadContext ctx */) {
        if (DynamXConfig.udpDebug) {
            System.out.println("[UDP-DEBUG] Received auth proposal");
        }
        // TODO port:1.20.1 - Re-port body using DynamXContext.getNetwork() + Minecraft.getInstance().execute(...).
        DynamXConfig.syncPacks = packet.syncDynamXPacks;
    }

    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }
}
