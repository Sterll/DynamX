package fr.dynamx.server.network.udp;

import com.mojang.authlib.GameProfile;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.IDnxNetworkHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.network.udp.auth.MessageDynamXUdpSettings;
import fr.dynamx.server.network.DynamXServerNetworkSystem;
import fr.dynamx.utils.DynamXConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Listens to player login/logout events and either initiates UDP auth or notifies the client
 * that only the TCP fallback is available.
 *
 * <p>TODO port:1.20.1 - Forge {@code FMLCommonHandler#instance()} and Forge
 * {@code PlayerEvent.PlayerLoggedInEvent} are replaced by NeoForge's
 * {@code net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent}.
 */
public class UdpServerConnectionHandler {
    private final IDnxNetworkHandler networkHandler;
    private final List<GameProfile> loggedIn;

    public UdpServerConnectionHandler(IDnxNetworkHandler vc) {
        this.networkHandler = vc;
        this.loggedIn = new ArrayList<>();
    }

    @SubscribeEvent
    public void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!this.loggedIn.contains(player.getGameProfile())) {
            if (DynamXConfig.udpDebug)
                DynamXMain.log.info("[UDP-DEBUG] Logging " + player);
            this.loggedIn.add(player.getGameProfile());
            this.onConnected(player);
        }
    }

    private void onConnected(Player entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        if (!(networkHandler instanceof UdpServerNetworkHandler)) {
            DynamXContext.getNetwork().sendToClient(new MessageDynamXUdpSettings(this.networkHandler.getType().ordinal(), 0, "", "", DynamXConfig.syncPacks), EnumPacketTarget.PLAYER, player);
            return;
        }

        if (DynamXConfig.udpDebug) {
            DynamXMain.log.info("[UDP-DEBUG] Initiating auth of {}", player);
        }

        String hash = null;
        while (hash == null) {
            try {
                hash = this.sha256(RandomStringUtils.random(32));
            } catch (NoSuchAlgorithmException e) {
                DynamXMain.log.error(e);
            }
        }

        UdpServerNetworkHandler voiceServer = (UdpServerNetworkHandler) this.networkHandler;
        voiceServer.waitingAuth.put(hash, player);
        DynamXContext.getNetwork().sendToClient(new MessageDynamXUdpSettings(this.networkHandler.getType().ordinal(), DynamXConfig.udpPort, hash, DynamXConfig.usingProxy ? ((DynamXServerNetworkSystem) DynamXContext.getNetwork()).getServerIPAdressRetriever().getAddress() : "", DynamXConfig.syncPacks), EnumPacketTarget.PLAYER, player);
    }

    @SubscribeEvent
    public void onDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }

        if (DynamXConfig.udpDebug) {
            DynamXMain.log.info("[UDP-DEBUG] Disconnected {}", player);
        }

        if (networkHandler instanceof UdpServerNetworkHandler) {
            ((UdpServerNetworkHandler) networkHandler).closeConnection(player.getId());
        }

        this.loggedIn.remove(player.getGameProfile());
    }

    private String sha256(String s) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(s.getBytes());
        StringBuilder sb = new StringBuilder();

        for (byte aHash : Objects.requireNonNull(hash)) {
            String hex = Integer.toHexString(aHash);

            if (hex.length() == 1) {
                sb.append(0);
                sb.append(hex.charAt(hex.length() - 1));
            } else
                sb.append(hex.substring(hex.length() - 2));
        }
        return sb.toString();
    }
}
