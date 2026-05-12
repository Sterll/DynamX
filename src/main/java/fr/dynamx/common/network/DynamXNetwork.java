package fr.dynamx.common.network;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.aym.acslib.utils.packetserializer.PacketDataSerializer;
import fr.aym.acslib.utils.packetserializer.PacketSerializer;
import fr.dynamx.api.network.EnumNetworkType;
import fr.dynamx.api.network.IDnxNetworkSystem;
import fr.dynamx.api.network.IDnxPacket;
import fr.dynamx.common.contentpack.sync.MessagePacksHashs;
import fr.dynamx.common.network.packets.*;
import fr.dynamx.common.network.sync.MessageMultiPhysicsEntitySync;
import fr.dynamx.common.network.sync.MessagePhysicsEntitySync;
import fr.dynamx.common.network.sync.MessageSeatsSync;
import fr.dynamx.common.network.udp.auth.MessageDynamXUdpSettings;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXConstants;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The DynamX network holding packets registry. <br>
 * In Forge 1.12 this used {@link net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper} to register
 * and dispatch messages. In NeoForge 1.20.1 message registration is performed through a
 * {@link net.minecraftforge.network.registration.PayloadRegistrar} listening to
 * {@link net.minecraftforge.network.event.RegisterPayloadHandlersEvent}, and dispatch uses
 * {@link net.minecraftforge.network.PacketDistributor}.
 *
 * The legacy registry tables (UDP_PACKETS, getUdpPacketById, getUdpMessageId) are preserved because
 * EncapsulatedUDPPacket still uses them to map packet ids on the UDP wire.
 */
// TODO port:1.20.1 - The whole SimpleNetworkWrapper machinery is gone. Once Phase 5b lands the
// PayloadRegistrar wiring, replace the registerMessage / registerMessageWithUDP helpers with
// playToClient / playToServer / playBidirectional calls and convert each IDnxPacket implementer
// into a CustomPacketPayload (with StreamCodec). For now the init() method only constructs the
// network system + populates the UDP id table so the rest of the codebase keeps compiling.
public class DynamXNetwork {
    public static final BiMap<Integer, Class<? extends IDnxPacket>> UDP_PACKETS = HashBiMap.create();

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(DynamXConstants.ID, "main"))
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();

    private static int id;
    private static int udpId;

    /**
     * Creates a new {@link IDnxNetworkSystem} for this side and registers all packets. <br>
     * The network instance is stored in DynamXContext (not yet ported in Phase 4b).
     */
    // TODO port:1.20.1 - Side is now LogicalSide (or net.minecraftforge.api.distmarker.Dist for client/dedicated).
    // Server-side DynamXServerNetworkSystem is part of Phase 5b (server network handlers); until then
    // we always return a client network system on logical client and null on server.
    public static IDnxNetworkSystem init(LogicalSide side) {
        EnumNetworkType type = DynamXConfig.useUdp ? EnumNetworkType.DYNAMX_UDP : EnumNetworkType.VANILLA_TCP;
        IDnxNetworkSystem network;
        if (side == LogicalSide.SERVER) {
            // TODO port:1.20.1 - swap for DynamXServerNetworkSystem when UDP server lands.
            network = new fr.dynamx.server.network.DynamXServerNetworkSystem(type);
        } else {
            network = new DynamXClientNetworkSystem(type);
        }
        // TODO port:1.20.1 - getChannel() returns null in 1.20.1; the legacy `channel` variable is unused.

        // The block below documents the legacy registration order so the UDP packet ids
        // match the server side once both ends are ported. Each call is replaced by a TODO and
        // simply populates the UDP_PACKETS BiMap when the legacy registerMessageWithUDP was used.

        //Udp packets

        //Bi-way
        registerMessageWithUDP(MessagePhysicsEntitySync.class, LogicalSide.CLIENT, LogicalSide.SERVER);
        registerMessageWithUDP(MessageMultiPhysicsEntitySync.class, LogicalSide.CLIENT, LogicalSide.SERVER);
        registerMessageWithUDP(MessagePing.class, LogicalSide.SERVER, LogicalSide.CLIENT);
        registerMessageWithUDP(MessageWalkingPlayer.class, LogicalSide.CLIENT, LogicalSide.SERVER);

        //To server
        registerMessageWithUDP(MessageRequestFullEntitySync.class, LogicalSide.SERVER);

        //Standard packets

        //Bi-way
        registerMessage(MessageQueryChunks.class, LogicalSide.CLIENT, LogicalSide.SERVER);
        registerMessage(MessageOpenDebugGui.class, LogicalSide.SERVER, LogicalSide.CLIENT);

        //To client
        registerMessage(MessageDynamXUdpSettings.class, LogicalSide.CLIENT);
        registerMessage(MessageSyncConfig.class, LogicalSide.CLIENT);
        registerMessage(MessagePacksHashs.class, LogicalSide.CLIENT);
        registerMessage(MessageSeatsSync.class, LogicalSide.CLIENT);
        registerMessage(MessageUpdateChunk.class, LogicalSide.CLIENT);
        registerMessage(MessageChunkData.class, LogicalSide.CLIENT);
        registerMessage(MessageForcePlayerPos.class, LogicalSide.CLIENT);
        registerMessage(MessageJoints.class, LogicalSide.CLIENT);
        registerMessage(MessageSyncPlayerPicking.class, LogicalSide.CLIENT);
        registerMessage(MessageSwitchAutoSlopesMode.class, LogicalSide.CLIENT);
        registerMessage(MessageCollisionDebugDraw.class, LogicalSide.CLIENT);
        registerMessage(MessageCollisionDebugDraw.class, LogicalSide.CLIENT);
        registerMessage(MessageHandleExplosion.class, LogicalSide.CLIENT);

        //To server
        registerMessage(MessagePacksHashs.class, LogicalSide.SERVER);
        registerMessage(MessageEntityInteract.class, LogicalSide.SERVER);
        registerMessage(MessagePickObject.class, LogicalSide.SERVER);
        registerMessage(MessageChangeDoorState.class, LogicalSide.SERVER);
        registerMessage(MessageSyncBlockCustomization.class, LogicalSide.SERVER);
        registerMessage(MessageSlopesConfigGui.class, LogicalSide.SERVER);
        registerMessage(MessageDebugRequest.class, LogicalSide.SERVER);
        registerMessage(MessageAttachTrailer.class, LogicalSide.SERVER);

        //Packet serializers (jME3 Vector3f / Quaternion adapters used by acslib's PacketSerializer)
        PacketSerializer.addCustomSerializer(new PacketDataSerializer<Vector3f>() {
            @Override
            public Class<Vector3f> objectType() {
                return Vector3f.class;
            }

            @Override
            public void serialize(ByteBuf to, Vector3f vector3f) {
                to.writeFloat(vector3f.x);
                to.writeFloat(vector3f.y);
                to.writeFloat(vector3f.z);
            }

            @Override
            public Vector3f deserialize(ByteBuf byteBuf) {
                return new Vector3f(byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat());
            }
        });
        PacketSerializer.addCustomSerializer(new PacketDataSerializer<Quaternion>() {
            @Override
            public Class<Quaternion> objectType() {
                return Quaternion.class;
            }

            @Override
            public void serialize(ByteBuf to, Quaternion quaternion) {
                to.writeFloat(quaternion.getX());
                to.writeFloat(quaternion.getY());
                to.writeFloat(quaternion.getZ());
                to.writeFloat(quaternion.getW());
            }

            @Override
            public Quaternion deserialize(ByteBuf byteBuf) {
                return new Quaternion(byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat());
            }
        });

        return network;
    }

    /**
     * Registers a packet on the SimpleChannel. The channel is bi-directional; the side argument
     * controls the direction Forge enforces (a CLIENT receiver means the packet flows to client).
     */
    private static <T extends IDnxPacket> void registerMessage(Class<T> message, LogicalSide... sides) {
        for (LogicalSide side : sides) {
            NetworkDirection direction = side == LogicalSide.CLIENT
                    ? NetworkDirection.PLAY_TO_CLIENT
                    : NetworkDirection.PLAY_TO_SERVER;
            CHANNEL.messageBuilder(message, id++, direction)
                    .encoder((msg, buf) -> msg.toBytes(buf))
                    .decoder(buf -> newPacket(message, buf))
                    .consumerMainThread((msg, ctxSupplier) -> {
                        net.minecraftforge.network.NetworkEvent.Context ctx = ctxSupplier.get();
                        try {
                            net.minecraft.world.entity.player.Player player;
                            LogicalSide receiveSide;
                            if (ctx.getDirection().getReceptionSide().isServer()) {
                                player = ctx.getSender();
                                receiveSide = LogicalSide.SERVER;
                            } else {
                                player = net.minecraftforge.fml.DistExecutor.safeCallWhenOn(
                                        net.minecraftforge.api.distmarker.Dist.CLIENT,
                                        () -> ClientNetworkBridge::getLocalPlayer);
                                receiveSide = LogicalSide.CLIENT;
                            }
                            // PhysicsEntityMessage subclasses already implement handleUDPReceive
                            // via processMessage; other packets default to a no-op until ported.
                            msg.handleUDPReceive(player, receiveSide);
                        } catch (UnsupportedOperationException expected) {
                            // Packet hasn't been ported yet — drop silently.
                        } catch (Throwable t) {
                            org.apache.logging.log4j.LogManager.getLogger("DynamX")
                                    .error("Failed to handle packet " + message.getSimpleName(), t);
                        }
                        ctx.setPacketHandled(true);
                    })
                    .add();
        }
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

    /**
     * Same as {@link #registerMessage} but also assigns a UDP-wire id used by
     * {@link fr.dynamx.common.network.udp.EncapsulatedUDPPacket}.
     */
    private static void registerMessageWithUDP(Class<? extends IDnxPacket> message, LogicalSide... sides) {
        if (udpId > 245)
            throw new RuntimeException("There is too many packets, limit is 245 for the UDP !");
        registerMessage(message, sides);
        UDP_PACKETS.put(udpId, message);
        udpId++;
    }

    public static IDnxPacket getUdpPacketById(int id) {
        try {
            return UDP_PACKETS.get(id).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getUdpMessageId(IDnxPacket message) {
        return UDP_PACKETS.inverse().get(message.getClass());
    }

    public static void sendToServer(IDnxPacket msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void sendTo(IDnxPacket msg, net.minecraft.server.level.ServerPlayer player) {
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToAll(IDnxPacket msg) {
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), msg);
    }

    public static void sendToAllTracking(IDnxPacket msg, net.minecraft.world.entity.Entity entity) {
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
    }
}
