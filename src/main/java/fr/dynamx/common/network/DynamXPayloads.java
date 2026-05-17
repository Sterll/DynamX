package fr.dynamx.common.network;

import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashSet;
import java.util.Set;

/**
 * Central registration helper for the DynamX SimpleChannel.
 *
 * Forge 1.20.1 (47.x) keeps the SimpleChannel/NetworkRegistry API; the NeoForge-only
 * PayloadRegistrar / RegisterPayloadHandlersEvent does not exist here. This class holds:
 * <ul>
 *   <li>the channel identifier, protocol version, and the {@link SimpleChannel} instance</li>
 *   <li>a deduplicating {@code register} helper that picks the right NetworkDirection for
 *       unidirectional packets and no direction for bidirectional ones (SimpleChannel keys
 *       its codec by message class, so registering the same class twice with two directions
 *       would silently overwrite the first)</li>
 * </ul>
 *
 * Packet bodies (toBytes/fromBytes/handleUDPReceive) still live in each {@link IDnxPacket}
 * implementer; this class only owns the wire-up.
 */
public final class DynamXPayloads {
    public static final String PROTOCOL_VERSION = "1";
    public static final ResourceLocation CHANNEL_ID = new ResourceLocation(DynamXConstants.ID, "main");

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(CHANNEL_ID)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();

    /**
     * Tracks classes already registered on {@link #CHANNEL} so the second call for the same
     * message class is a safe no-op rather than a silent codec overwrite.
     */
    private static final Set<Class<?>> REGISTERED = new HashSet<>();

    /**
     * Monotonically-increasing codec index. Stable as long as init() is called once per JVM.
     */
    private static int nextId;

    private DynamXPayloads() {
    }

    /**
     * Registers a packet on the channel.
     *
     * @param message the packet class
     * @param sides   the logical sides this packet can be received on. Empty or both sides means
     *                bidirectional (no direction restriction); a single side means the packet may
     *                only flow toward that side.
     */
    public static <T extends IDnxPacket> void register(Class<T> message, LogicalSide... sides) {
        if (!REGISTERED.add(message)) {
            return;
        }
        boolean toClient = false;
        boolean toServer = false;
        for (LogicalSide s : sides) {
            if (s == LogicalSide.CLIENT) toClient = true;
            else if (s == LogicalSide.SERVER) toServer = true;
        }
        NetworkDirection direction;
        if (toClient && !toServer) direction = NetworkDirection.PLAY_TO_CLIENT;
        else if (toServer && !toClient) direction = NetworkDirection.PLAY_TO_SERVER;
        else direction = null;

        int index = nextId++;
        SimpleChannel.MessageBuilder<T> builder = direction == null
                ? CHANNEL.messageBuilder(message, index)
                : CHANNEL.messageBuilder(message, index, direction);
        builder
                .encoder((msg, buf) -> msg.toBytes(buf))
                .decoder(buf -> newPacket(message, buf))
                .consumerMainThread((msg, ctxSupplier) -> dispatch(msg, ctxSupplier, message))
                .add();
    }

    private static <T extends IDnxPacket> T newPacket(Class<T> message, FriendlyByteBuf buf) {
        try {
            T packet = message.getDeclaredConstructor().newInstance();
            packet.fromBytes(buf);
            return packet;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate DynamX packet " + message.getName(), e);
        }
    }

    private static <T extends IDnxPacket> void dispatch(T msg,
                                                        java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> ctxSupplier,
                                                        Class<T> messageClass) {
        net.minecraftforge.network.NetworkEvent.Context ctx = ctxSupplier.get();
        try {
            net.minecraft.world.entity.player.Player player;
            LogicalSide receiveSide;
            if (ctx.getDirection().getReceptionSide().isServer()) {
                player = ctx.getSender();
                receiveSide = LogicalSide.SERVER;
            } else {
                player = net.minecraftforge.fml.loading.FMLEnvironment.dist
                        == net.minecraftforge.api.distmarker.Dist.CLIENT
                        ? ClientNetworkBridge.getLocalPlayer()
                        : null;
                receiveSide = LogicalSide.CLIENT;
            }
            msg.handleUDPReceive(player, receiveSide);
        } catch (UnsupportedOperationException notPorted) {
            // Packet hasn't migrated its handler yet (tab 2 territory); drop silently.
        } catch (Throwable t) {
            org.apache.logging.log4j.LogManager.getLogger("DynamX")
                    .error("Failed to handle packet " + messageClass.getSimpleName(), t);
        }
        ctx.setPacketHandled(true);
    }

    /**
     * Resets the registration state. Test-only.
     */
    static void resetForTests() {
        REGISTERED.clear();
        nextId = 0;
    }

    public static int registeredCount() {
        return REGISTERED.size();
    }
}
