package fr.dynamx.common.contentpack.sync;

import fr.dynamx.DynamX;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.loader.InfoLoader;
import fr.dynamx.common.network.DynamXNetwork;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.Vector3fPool;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    public void handleUDPReceive(Player context, LogicalSide side) {
        if (side == LogicalSide.SERVER) {
            handleServer(context);
        } else {
            handleClient();
        }
    }

    private void handleServer(Player context) {
        if (!(context instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!DynamXConfig.syncPacks) {
            DynamXMain.log.warn("[PackSync] Sync requested by {}, but disabled on server", serverPlayer);
            return;
        }
        try {
            Map<String, List<String>> delta = PackSyncHandler.getFullDelta(objects);
            if (delta.values().stream().allMatch(List::isEmpty)) {
                DynamXMain.log.debug("[PackSync] No delta for {}", serverPlayer);
                return;
            }
            Map<String, Map<String, byte[]>> fullData = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : delta.entrySet()) {
                String s = entry.getKey();
                List<String> l = entry.getValue();
                fullData.put(s, new HashMap<>());
                InfoLoader<?> loader = DynamXObjectLoaders.getInfoLoaders().stream()
                        .filter(i -> i.getPrefix().equals(s)).findFirst().orElse(null);
                if (loader == null) {
                    continue;
                }
                loader.encodeObjects(l, fullData.get(s));
            }
            DynamXMain.log.info("[PackSync] Sending {} changed pack files to {}",
                    fullData.entrySet().stream().map(e -> e.getKey() + "->" + e.getValue().size()).collect(Collectors.toList()),
                    serverPlayer);
            DynamXNetwork.sendTo(new MessagePacksHashs(fullData), serverPlayer);
        } catch (Exception e) {
            DynamXMain.log.error("[PackSync] Failed to sync changed pack files for " + serverPlayer, e);
            serverPlayer.connection.disconnect(Component.literal("Invalid DynamX pack " + e.getMessage()));
        }
    }

    private void handleClient() {
        DynamXMain.log.info("[PackSync] Received server packs, applying diff of {} elements...",
                objects.entrySet().stream().map(e -> e.getKey() + "->" + e.getValue().size()).collect(Collectors.toList()));
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setOverlayMessage(Component.literal("Synchronizing DynamX packs..."), false);
        mc.execute(() -> {
            Vector3fPool.openPool();
            try {
                for (Map.Entry<String, Map<String, byte[]>> entry : objects.entrySet()) {
                    String s = entry.getKey();
                    Map<String, byte[]> l = entry.getValue();
                    InfoLoader<?> loader = DynamXObjectLoaders.getInfoLoaders().stream()
                            .filter(i -> i.getPrefix().equals(s)).findFirst().orElse(null);
                    if (loader != null) {
                        loader.receiveObjects(l);
                    }
                }
                DynamXUtils.hotswapWorldPackInfos(DynamXMain.proxy.getClientWorld());
                mc.gui.setOverlayMessage(Component.literal(""), false);
            } catch (Exception e) {
                DynamXMain.log.fatal("Cannot sync DynamX packs. Connection to the server will be closed.", e);
                if (mc.getConnection() != null) {
                    mc.getConnection().getConnection().disconnect(
                            Component.literal("Failed to sync DynamX packs. Update your client packs."));
                }
            } finally {
                Vector3fPool.closePool();
            }
        });
    }
}
