package fr.dynamx.common.core.mixin;

import fr.dynamx.common.DynamXContext;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Patches the server game-packet listener to disable "moving too quickly"/"moved wrongly" warnings
 * when players walk on moving vehicles, and to override reach distance when interacting with
 * {@code DynamXBlock} instances.
 *
 * <p>TODO port:1.20.1 - The 1.12 mixin {@code @Redirect}-ed two {@code ordinal} calls of
 * {@code EntityPlayerMP#isInvulnerableDimensionChange()Z} inside {@code processPlayer}, captured
 * the local {@code IAttributeInstance} returned by {@code getAttributeValue()}, then redirected
 * that same call. In 1.20.1:
 * <ul>
 *   <li>{@code processPlayer} is now {@code handleMovePlayer(ServerboundMovePlayerPacket)}.</li>
 *   <li>{@code isInvulnerableDimensionChange()} is now {@code isChangingDimension()} (mojmap)
 *       and its placement inside the new {@code handleMovePlayer} body is not 1:1.</li>
 *   <li>{@code IAttributeInstance#getAttributeValue()} is now
 *       {@code AttributeInstance#getValue()}; reach distance is no longer an attribute in vanilla
 *       1.20.1 (NeoForge {@code NeoForgeMod.BLOCK_REACH}/{@code ENTITY_REACH} exposes it).</li>
 *   <li>{@code processPlayerDigging} is now
 *       {@code handlePlayerAction(ServerboundPlayerActionPacket)} and
 *       {@code CPacketPlayerDigging.Action} -> {@code ServerboundPlayerActionPacket.Action}.</li>
 * </ul>
 *
 * <p>Body stubbed (with the shadowed {@code player} field kept so the mixin class still binds);
 * to be re-injected once the new mojmap signatures and the reach-attribute story are pinned.
 */
@Mixin(value = ServerGamePacketListenerImpl.class, remap = DynamXConstants.REMAP)
public class MixinNetHandlerPlayServer {

    @Shadow
    public ServerPlayer player;

    @Unique
    private int dynamX$overrideReachDistance;

    /**
     * Suppresses vanilla "moved too quickly" / "moved wrongly" warnings (and the teleport-back they
     * trigger) when the player is walking on a DynamX vehicle. Both guards in
     * {@code handleMovePlayer} short-circuit when {@code isChangingDimension()} returns true; we
     * piggy-back on that path for walking players.
     */
    @Redirect(method = "handleMovePlayer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;isChangingDimension()Z"),
            remap = DynamXConstants.REMAP)
    private boolean dynamX$skipMovementChecks(ServerPlayer self) {
        if (DynamXContext.getWalkingPlayers().containsKey(self)) {
            return true;
        }
        return self.isChangingDimension();
    }

    // TODO port:1.20.1 - re-inject into handlePlayerAction to override reach distance for
    //   DynamXBlock interactions (vanilla 1.20.1 no longer uses an attribute for reach;
    //   it's NeoForgeMod.BLOCK_REACH).
}
