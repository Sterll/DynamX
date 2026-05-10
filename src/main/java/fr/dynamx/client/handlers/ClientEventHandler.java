package fr.dynamx.client.handlers;

import fr.aym.acsguis.api.ACsGuiApi;
import fr.aym.acslib.ACsLib;
import fr.aym.mps.ModProtectionSystem;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.client.ClientProxy;
import fr.dynamx.client.gui.*;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.items.DynamXItemSpawner;
import fr.dynamx.common.items.tools.ItemSlopes;
import fr.dynamx.common.network.packets.MessageEntityInteract;
import fr.dynamx.common.physics.player.WalkingOnPlayerController;
import fr.dynamx.common.slopes.GuiSlopesConfig;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.LevelEvent;

import java.util.UUID;

/**
 * Big aggregator class for client-side Forge events.
 *
 * <p>TODO port:1.20.1 - extensive migration:</p>
 * <ul>
 *   <li>{@code Minecraft.getMinecraft()} -> {@code Minecraft.getInstance()}.</li>
 *   <li>{@code WorldEvent.Unload} -> {@code LevelEvent.Unload}; {@code event.getWorld().isRemote} -> {@code event.getLevel().isClientSide()}.</li>
 *   <li>{@code EnumHand.MAIN_HAND} -> {@code InteractionHand.MAIN_HAND}; {@code EnumActionResult.SUCCESS} -> {@code InteractionResult.SUCCESS}.</li>
 *   <li>{@code mc.displayGuiScreen} -> {@code mc.setScreen}.</li>
 *   <li>{@code GuiOpenEvent} -> {@code ScreenEvent.Opening}; {@code GuiScreenEvent.InitGuiEvent.Post} -> {@code ScreenEvent.Init.Post}.</li>
 *   <li>{@code RenderGameOverlayEvent.Pre(CROSSHAIRS / ALL)} -> {@code RenderGuiOverlayEvent.Pre} on {@code VanillaGuiOverlay.CROSSHAIR} / fully replaced for ALL.</li>
 *   <li>{@code SoundLoadEvent} / {@code SoundSetupEvent} -> the new 1.20 sound system (paulscode SoundSystem is gone, see DynamXSoundHandler).</li>
 *   <li>{@code DrawBlockHighlightEvent} -> {@code RenderHighlightEvent.Block}.</li>
 *   <li>{@code EntityViewRenderEvent.CameraSetup} -> {@code ViewportEvent.ComputeCameraAngles}.</li>
 *   <li>{@code RenderWorldLastEvent} -> {@code RenderLevelStageEvent}.</li>
 *   <li>{@code FMLNetworkEvent.Client*} -> {@code ClientPlayerNetworkEvent.LoggingIn/LoggingOut}.</li>
 *   <li>{@code RenderPlayerEvent.Pre} / {@code RenderLivingEvent.Pre} -> same names but on the NeoForge bus; {@code event.getRenderer().getRenderManager().isRenderShadow()} -> {@code event.getEntityRenderDispatcher().shouldRenderShadow}.</li>
 *   <li>{@code TextFormatting} -> {@code ChatFormatting}; {@code SoundCategory.values()} stays.</li>
 *   <li>{@code CustomModLoadingErrorDisplayException} is gone in 1.20.1; the GuiMpsLoadingError now extends {@code Screen} directly.</li>
 *   <li>{@code player.world}/{@code player.world.isRemote} -> {@code player.level()}/{@code level().isClientSide()}.</li>
 *   <li>{@code MC.objectMouseOver} -> {@code MC.hitResult}; {@code .typeOfHit == RayTraceResult.Type.BLOCK/MISS} -> {@code .getType() == HitResult.Type.BLOCK/MISS}.</li>
 *   <li>{@code MC.player.getRidingEntity()} -> {@code MC.player.getVehicle()}.</li>
 *   <li>{@code DxModelRenderer} / preview rendering deferred to Phase 7; {@code model} field typed loosely.</li>
 * </ul>
 * The class keeps its event subscriptions but most rendering / overlay logic is stubbed.
 */
public class ClientEventHandler {
    public static final Minecraft MC = Minecraft.getInstance();
    public static UUID renderingEntity;
    public static boolean isRenderingEntitiesWithOptifineShaders;

    /* World events */

    @SubscribeEvent
    public void onWorldUnloaded(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ClientProxy.SOUND_HANDLER.unload();
        }
        DynamXDebugOptions.PROFILING.disable();
    }

    /* Interaction events */

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            return;
        }
        if (!(event.getTarget() instanceof PhysicsEntity) || !event.getHand().equals(InteractionHand.MAIN_HAND) || event.getEntity().getItemInHand(event.getHand()).getItem() instanceof DynamXItemSpawner) {
            return;
        }
        DynamXContext.getNetwork().sendToServer(new MessageEntityInteract(event.getTarget().getId()));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public void onRightClickAir(PlayerInteractEvent.RightClickItem e) {
        if (e.getLevel().isClientSide() && (MC.hitResult == null || MC.hitResult.getType() == HitResult.Type.MISS) && !e.getEntity().isShiftKeyDown() && e.getItemStack().getItem() instanceof ItemSlopes) {
            Minecraft.getInstance().setScreen(new GuiSlopesConfig(e.getItemStack()).getGuiScreen());
        }
    }

    @SubscribeEvent
    public void onMount(VehicleEntityEvent.EntityMount event) {
        if (!(event.getEntityMounted() instanceof Player) || !((Player) event.getEntityMounted()).isLocalPlayer()) {
            return;
        }
        ACsGuiApi.asyncLoadThenShowHudGui("Vehicle HUD", () -> new VehicleHud((IModuleContainer.ISeatsContainer) event.getEntity()));
    }

    @SubscribeEvent
    public void onDismount(VehicleEntityEvent.EntityDismount event) {
        if (!(event.getEntityDismounted() instanceof Player) || !((Player) event.getEntityDismounted()).isLocalPlayer()) {
            return;
        }
        ACsGuiApi.closeHudGui(VehicleHud.class);
    }

    /* Gui events */

    @SubscribeEvent
    public void guiOpenEvent(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof TitleScreen && DynamXMain.memoizedLoadingError != null) {
            DynamXMain.log.warn("Some errors occurred while loading DynamX content. Showing user a custom error screen.");
            event.setNewScreen(new GuiMpsLoadingError((TitleScreen) event.getNewScreen()));
        }
    }

    @SubscribeEvent
    public void initMainMenu(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen && DynamXErrorManager.getErrorManager().hasErrors(ACsLib.getPlatform().getACsLibErrorCategory(), DynamXErrorManager.INIT_ERRORS, DynamXErrorManager.PACKS_ERRORS, DynamXErrorManager.MODEL_ERRORS, ACsGuiApi.getCssErrorType(), ModProtectionSystem.getMpsErrorCategory())) {
            // TODO port:1.20.1 - add a GuiTexturedButton("DynamX loading errors") on the title screen.
            // 1.20 uses Button.builder(...) and ScreenEvent.Init.Post exposes addListener via the screen API.
        } else if (event.getScreen() instanceof TitleScreen && DynamXErrorManager.getErrorManager().hasErrors(DynamXErrorManager.UPDATES)) {
            // TODO port:1.20.1 - add an "Update available" Button.
        }
        // TODO port:1.20.1 - GuiScreenOptionsSounds is OptionsSubScreen.SoundOptionsScreen now; add the
        // "DynamX Sounds" ButtonSlider through ScreenEvent.Init.Post addListener mechanism.
    }

    /* Overlay events */

    /**
     * <p>TODO port:1.20.1 - replace with {@code RenderGuiOverlayEvent.Pre} on {@code VanillaGuiOverlay.CROSSHAIR}.
     * Use {@code event.getGuiGraphics()} to {@code blit} the custom cursor texture. The "connecting to server"
     * status overlay should be on {@code VanillaGuiOverlay.PLAYER_LIST} or a generic post overlay.</p>
     */
    @SubscribeEvent
    public void drawHudCursor(/* RenderGuiOverlayEvent.Pre */ Object event) {
        // TODO port:1.20.1 - body stubbed pending RenderGuiOverlayEvent integration.
    }

    /* Network events */

    private static long connectionTime = -1;

    /**
     * <p>TODO port:1.20.1 - {@code FMLNetworkEvent.ClientConnectedToServerEvent} -> {@code ClientPlayerNetworkEvent.LoggingIn}.</p>
     */
    @SubscribeEvent
    public void onClientConnected(/* ClientPlayerNetworkEvent.LoggingIn */ Object event) {
        connectionTime = System.currentTimeMillis();
    }

    /**
     * <p>TODO port:1.20.1 - {@code FMLNetworkEvent.ClientDisconnectionFromServerEvent} -> {@code ClientPlayerNetworkEvent.LoggingOut}.</p>
     */
    @SubscribeEvent
    public void onClientDisconnected(/* ClientPlayerNetworkEvent.LoggingOut */ Object event) {
        DynamXContext.getNetwork().stopNetwork();
        connectionTime = -1;
    }

    /* Sound events */

    /**
     * <p>TODO port:1.20.1 - {@code SoundSetupEvent} is gone, replaced by hooking into the {@code SoundEngine}.
     * See {@link DynamXSoundHandler} for the heavy rewrite.</p>
     */
    @SubscribeEvent
    public void onSoundSystemSetup(/* SoundSetupEvent */ Object event) {
        // ClientProxy.SOUND_HANDLER.setup(event);
        // TODO port:1.20.1 - stubbed.
    }

    /**
     * <p>TODO port:1.20.1 - {@code SoundLoadEvent} still exists; the body just needs to call into the
     * new {@link DynamXSoundHandler#load} once that's ported.</p>
     */
    @SubscribeEvent
    public void onSoundSystemLoad(/* SoundLoadEvent */ Object event) {
        // ClientProxy.SOUND_HANDLER.load(event);
        // TODO port:1.20.1 - stubbed.
    }

    /* Tick/render events */

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        ClientProxy.SOUND_HANDLER.tick();

        if (connectionTime != -1 && !Minecraft.getInstance().hasSingleplayerServer()) {
            if ((System.currentTimeMillis() - connectionTime) > 30000) {
                connectionTime = -1;
                if (!DynamXContext.getNetwork().isConnected()) {
                    DynamXMain.log.fatal("Failed to establish an TCP/UDP connection : timed out (0x1)");
                    if (Minecraft.getInstance().getConnection() != null && DynamXConfig.doUdpTimeOut) {
                        Minecraft.getInstance().getConnection().getConnection().disconnect(Component.literal("DynamX UDP connection timed out (Auth not started)"));
                    }
                }
            }
        }

        Player entityPlayer = Minecraft.getInstance().player;
        if (entityPlayer == null) {
            return;
        }
        if (DynamXContext.getWalkingPlayers().containsKey(entityPlayer)) {
            PhysicsEntity<?> physicsEntity = DynamXContext.getWalkingPlayers().get(entityPlayer);
            if (!physicsEntity.canPlayerStandOnTop()) {
                if (WalkingOnPlayerController.controller != null) {
                    WalkingOnPlayerController.controller.disable();
                    entityPlayer.setDeltaMovement(entityPlayer.getDeltaMovement().add(0, 0.2D, 0));
                }
            }
        }

        // TODO port:1.20.1 - in-world DxModel preview placement uses DxModelRenderer (Phase 7).
        // The original body computed playerOrientation/blockPos for the placement preview model.
    }

    /**
     * <p>TODO port:1.20.1 - {@code DrawBlockHighlightEvent} -> {@code RenderHighlightEvent.Block};
     * preview rendering uses {@code DxModelRenderer} which lives in Phase 7.</p>
     */
    @SubscribeEvent
    public void onDrawBlockHighlight(/* RenderHighlightEvent.Block */ Object event) {
        // TODO port:1.20.1 - stubbed.
    }

    /**
     * <p>TODO port:1.20.1 - {@code EntityViewRenderEvent.CameraSetup} -> {@code ViewportEvent.ComputeCameraAngles}.
     * The vehicle-camera-rotation helper signature changes accordingly.</p>
     */
    @SubscribeEvent
    public void onEntityCameraSetup(/* ViewportEvent.ComputeCameraAngles */ Object event) {
        // if (event.getCamera().getEntity().getVehicle() instanceof PhysicsEntity) {
        //     CameraSystem.rotateVehicleCamera(event);
        // }
        // TODO port:1.20.1 - stubbed.
    }

    /**
     * <p>TODO port:1.20.1 - {@code RenderWorldLastEvent} -> {@code RenderLevelStageEvent} (filter by
     * {@code Stage.AFTER_TRANSLUCENT_BLOCKS}). The body renders MovableLines, debug, and "big entities".</p>
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void renderWorldLast(/* RenderLevelStageEvent */ Object event) {
        // TODO port:1.20.1 - stubbed.
    }

    /**
     * <p>TODO port:1.20.1 - body needs {@code MC.level.entitiesForRendering()} iteration plus a custom
     * {@code Frustum} construction. {@code MC.world.loadedEntityList} is gone; use {@code level.entitiesForRendering()}.
     * Big-entity rendering also routes through {@code MC.getEntityRenderDispatcher()}.</p>
     */
    public static void resetBigEntities() {
        // TODO port:1.20.1 - stubbed.
    }

    /**
     * <p>TODO port:1.20.1 - see resetBigEntities. Uses {@code MC.getEntityRenderDispatcher()} + a custom Frustum.</p>
     */
    public static void renderBigEntities(float partialTicks, boolean isRenderWorldLast) {
        // TODO port:1.20.1 - stubbed.
    }

    /**
     * <p>TODO port:1.20.1 - {@code RenderPlayerEvent.Pre} still exists; access dispatcher via
     * {@code event.getRenderer().entityRenderDispatcher.shouldRenderShadow}.</p>
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void playerRender(/* RenderPlayerEvent.Pre */ Object event) {
        // TODO port:1.20.1 - stubbed.
    }

    /**
     * <p>TODO port:1.20.1 - see playerRender. Cancels rendering of riders that aren't being drawn by
     * the entity's own renderer (to keep mod compatibility with priority HIGHEST).</p>
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void entityRender(/* RenderLivingEvent.Pre */ Object event) {
        // TODO port:1.20.1 - stubbed.
    }

    @SuppressWarnings("unused")
    private static ResourceLocation focusTexture() {
        return new ResourceLocation(DynamXConstants.ID, "textures/focus.png");
    }
}
