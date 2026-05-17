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
import io.netty.buffer.ByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * DynamX network entry point.
 *
 * In Forge 1.12 this was a {@link net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper}
 * wrapper. In Forge 1.20.1 we still use {@link SimpleChannel} (the NeoForge-only PayloadRegistrar
 * is not present in MinecraftForge 47.x), but registration is centralized in {@link DynamXPayloads}.
 * This class owns the UDP id table, jME3 packet serializers, and the dispatch helpers used
 * elsewhere in the codebase.
 */
public class DynamXNetwork {
    /**
     * UDP wire id to packet class. Populated below; consumed by
     * {@link fr.dynamx.common.network.udp.EncapsulatedUDPPacket}.
     */
    public static final BiMap<Integer, Class<? extends IDnxPacket>> UDP_PACKETS = HashBiMap.create();

    /**
     * Alias so existing call-sites that read {@code DynamXNetwork.CHANNEL} keep compiling.
     */
    public static final SimpleChannel CHANNEL = DynamXPayloads.CHANNEL;

    private static int udpId;
    private static boolean registered;

    /**
     * Creates the {@link IDnxNetworkSystem} for this side and, on the first call, registers every
     * DynamX packet on {@link DynamXPayloads#CHANNEL} plus the jME3 packet serializers.
     */
    public static IDnxNetworkSystem init(LogicalSide side) {
        EnumNetworkType type = DynamXConfig.useUdp ? EnumNetworkType.DYNAMX_UDP : EnumNetworkType.VANILLA_TCP;

        if (!registered) {
            registerPackets();
            registerSerializers();
            registered = true;
        }

        if (side == LogicalSide.SERVER) {
            return new fr.dynamx.server.network.DynamXServerNetworkSystem(type);
        }
        return new DynamXClientNetworkSystem(type);
    }

    /**
     * Registers every DynamX packet on the SimpleChannel. Order must match between client and
     * server because SimpleChannel is index-based; both sides walk this method body.
     */
    private static void registerPackets() {
        // UDP packets (also flow over the TCP fallback). The UDP id assignment must follow the
        // legacy order so saved-state and existing remote clients wire up identically.

        // Bi-way
        registerMessageWithUDP(MessagePhysicsEntitySync.class, LogicalSide.CLIENT, LogicalSide.SERVER);
        registerMessageWithUDP(MessageMultiPhysicsEntitySync.class, LogicalSide.CLIENT, LogicalSide.SERVER);
        registerMessageWithUDP(MessagePing.class, LogicalSide.SERVER, LogicalSide.CLIENT);
        registerMessageWithUDP(MessageWalkingPlayer.class, LogicalSide.CLIENT, LogicalSide.SERVER);

        // To server
        registerMessageWithUDP(MessageRequestFullEntitySync.class, LogicalSide.SERVER);

        // Standard packets

        // Bi-way
        DynamXPayloads.register(MessageQueryChunks.class, LogicalSide.CLIENT, LogicalSide.SERVER);
        DynamXPayloads.register(MessageOpenDebugGui.class, LogicalSide.SERVER, LogicalSide.CLIENT);

        // To client
        DynamXPayloads.register(MessageDynamXUdpSettings.class, LogicalSide.CLIENT);
        DynamXPayloads.register(MessageSyncConfig.class, LogicalSide.CLIENT);
        DynamXPayloads.register(MessagePacksHashs.class, LogicalSide.CLIENT, LogicalSide.SERVER);
        DynamXPayloads.register(MessageSeatsSync.class, LogicalSide.CLIENT);
        DynamXPayloads.register(MessageUpdateChunk.class, LogicalSide.CLIENT);
        DynamXPayloads.register(MessageChunkData.class, LogicalSide.CLIENT);
        DynamXPayloads.register(MessageForcePlayerPos.class, LogicalSide.CLIENT);
        DynamXPayloads.register(MessageJoints.class, LogicalSide.CLIENT);
        DynamXPayloads.register(MessageSyncPlayerPicking.class, LogicalSide.CLIENT);
        DynamXPayloads.register(MessageSwitchAutoSlopesMode.class, LogicalSide.CLIENT);
        DynamXPayloads.register(MessageCollisionDebugDraw.class, LogicalSide.CLIENT);
        DynamXPayloads.register(MessageHandleExplosion.class, LogicalSide.CLIENT);

        // To server
        DynamXPayloads.register(MessageEntityInteract.class, LogicalSide.SERVER);
        DynamXPayloads.register(MessagePickObject.class, LogicalSide.SERVER);
        DynamXPayloads.register(MessageChangeDoorState.class, LogicalSide.SERVER);
        DynamXPayloads.register(MessageSyncBlockCustomization.class, LogicalSide.SERVER);
        DynamXPayloads.register(MessageSlopesConfigGui.class, LogicalSide.SERVER);
        DynamXPayloads.register(MessageDebugRequest.class, LogicalSide.SERVER);
        DynamXPayloads.register(MessageAttachTrailer.class, LogicalSide.SERVER);
    }

    /**
     * Same as {@link DynamXPayloads#register} but also assigns a UDP-wire id used by
     * {@link fr.dynamx.common.network.udp.EncapsulatedUDPPacket}.
     */
    private static void registerMessageWithUDP(Class<? extends IDnxPacket> message, LogicalSide... sides) {
        if (udpId > 245)
            throw new RuntimeException("There is too many packets, limit is 245 for the UDP !");
        DynamXPayloads.register(message, sides);
        UDP_PACKETS.put(udpId, message);
        udpId++;
    }

    private static void registerSerializers() {
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

    public static void sendTo(IDnxPacket msg, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToAll(IDnxPacket msg) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }

    public static void sendToAllTracking(IDnxPacket msg, Entity entity) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
    }
}
