package fr.dynamx.common.contentpack.sync;

import fr.dynamx.DynamX;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Pack-sync payload exchanged between client and server.
 *
 * TODO port:1.20.1 - Original referenced (not yet ported):
 *   - fr.dynamx.api.network.{EnumNetworkType, EnumPacketTarget, IDnxPacket} (Phase 6 - DynamX network rewrite).
 *   - fr.dynamx.common.{DynamXContext, DynamXMain} - replaced by DynamX.LOGGER.
 *   - fr.dynamx.common.handlers.TaskScheduler (Phase 5 - not yet ported).
 *   - net.minecraftforge.fml.common.network.{ByteBufUtils, simpleimpl.*} - the Forge SimpleImpl
 *     network was removed in NeoForge; replaced by SimpleChannel / new Custom Packets. The
 *     handler entry points have been removed for now and will be re-implemented in Phase 6
 *     against {@link net.minecraftforge.network.NetworkRegistry} or its successor.
 *   - net.minecraft.client.Minecraft#getIngameGUI -&gt; Minecraft.getInstance().gui (1.20.1).
 *   - net.minecraft.util.text.TextComponentString -&gt; Component.literal.
 *   - net.minecraft.client.Minecraft.addScheduledTask -&gt; Minecraft.getInstance().tell.
 *   For Phase 3b we keep the on-the-wire encode/decode logic (read/writeUTF8String inlined via
 *   length-prefixed UTF-8 byte arrays so we do not depend on Forge's ByteBufUtils) and stub
 *   the handler classes.
 */
public class MessagePacksHashs implements IDnxPacket {
    private Map<String, Map<String, byte[]>> objects;

    public MessagePacksHashs() {
    }

    public MessagePacksHashs(Map<String, Map<String, byte[]>> objects) {
        this.objects = objects;
    }

    public Map<String, Map<String, byte[]>> getObjects() {
        return objects;
    }

    /**
     * TODO port:1.20.1 - Was EnumNetworkType.VANILLA_TCP. Phase 6 will reintroduce the channel.
     */
    @Override
    public EnumNetworkType getPreferredNetwork() {
        return EnumNetworkType.VANILLA_TCP;
    }

    /**
     * Length-prefixed UTF-8 reader replacing Forge's ByteBufUtils.readUTF8String.
     */
    private static String readUtf8(ByteBuf buf) {
        int len = buf.readInt();
        byte[] tmp = new byte[len];
        buf.readBytes(tmp);
        return new String(tmp, StandardCharsets.UTF_8);
    }

    private static void writeUtf8(ByteBuf buf, String s) {
        byte[] tmp = s.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(tmp.length);
        buf.writeBytes(tmp);
    }

    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        objects = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String name = readUtf8(buf);
            Map<String, byte[]> map = new HashMap<>();
            int s = buf.readInt();
            for (int j = 0; j < s; j++) {
                String object = readUtf8(buf);
                byte[] arr = new byte[buf.readInt()];
                buf.readBytes(arr);
                map.put(object, arr);
            }
            objects.put(name, map);
        }
    }

    public void toBytes(ByteBuf buf) {
        buf.writeInt(objects.size());
        objects.forEach((s, m) -> {
            writeUtf8(buf, s);
            buf.writeInt(m.size());
            m.forEach((o, b) -> {
                writeUtf8(buf, o);
                buf.writeInt(b.length);
                buf.writeBytes(b);
            });
        });
    }

    /**
     * TODO port:1.20.1 - Original implemented IMessageHandler&lt;MessagePacksHashs, IMessage&gt;
     *   and used MessageContext.getServerHandler().player + .getNetworkManager().closeChannel(...).
     *   The IMessageHandler / MessageContext API is gone in NeoForge (use SimpleChannel
     *   IPayloadHandler in 1.20.1). The body is stubbed; logic will be ported in Phase 6.
     */
    public static class HandlerServer {
        public Object onMessage(MessagePacksHashs message, Object ctx) {
            DynamX.LOGGER.warn("[PackSync] HandlerServer.onMessage called but the NeoForge channel is not yet wired (Phase 6).");
            return null;
        }
    }

    /**
     * TODO port:1.20.1 - Original applied the diff on the client thread, refreshed the model
     *   registry, hot-swapped world pack infos and displayed an overlay message. All of these
     *   depend on DynamXContext / DynamXMain.proxy / DxModelRegistry / Minecraft.gui setOverlayMessage.
     *   The body is stubbed; logic will be ported in Phase 6.
     */
    public static class HandlerClient {
        public Object onMessage(MessagePacksHashs message, Object ctx) {
            DynamX.LOGGER.warn("[PackSync] HandlerClient.onMessage called but the NeoForge channel is not yet wired (Phase 6).");
            return null;
        }
    }
}
