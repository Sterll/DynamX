package fr.dynamx.client.handlers;

import com.mojang.blaze3d.platform.InputConstants;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.client.camera.CameraSystem;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.BasePartSeat;
import fr.dynamx.common.contentpack.parts.PartDoor;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.entities.modules.movables.PickingObjectHelper;
import fr.dynamx.common.items.tools.ItemSlopes;
import fr.dynamx.common.items.tools.ItemWrench;
import fr.dynamx.common.network.packets.MessageChangeDoorState;
import fr.dynamx.common.network.packets.MessageDebugRequest;
import fr.dynamx.common.network.packets.MessagePickObject;
import fr.dynamx.common.physics.player.WalkingOnPlayerController;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

import static fr.dynamx.client.handlers.ClientEventHandler.MC;

/**
 * Keybinding registration + per-tick key polling.
 *
 * <p>TODO port:1.20.1 - migration:</p>
 * <ul>
 *   <li>{@code KeyBinding} -> {@code KeyMapping}.</li>
 *   <li>{@code org.lwjgl.input.Keyboard.KEY_*} (LWJGL 2) -> {@code GLFW.GLFW_KEY_*} (LWJGL 3); use {@code InputConstants.Type.KEYSYM.getOrCreate(keycode)}.</li>
 *   <li>{@code ClientRegistry.registerKeyBinding} -> {@code RegisterKeyMappingsEvent.register} on the mod event bus.</li>
 *   <li>{@code MouseEvent} -> {@code InputEvent.MouseScrollingEvent} + {@code InputEvent.MouseButton.Pre}.
 *       {@code Mouse.getEventDWheel()} -> {@code event.getScrollDelta()}.</li>
 *   <li>{@code mc.player.getRidingEntity()} -> {@code mc.player.getVehicle()}.</li>
 *   <li>{@code mc.player.getHeldItemMainhand()} -> {@code mc.player.getMainHandItem()}.</li>
 *   <li>{@code mc.player.sendChatMessage(...)} -> {@code mc.player.connection.sendCommand(...)} for slash commands.</li>
 *   <li>{@code mc.gameSettings} -> {@code mc.options}; {@code keyBindForward.isKeyDown()} -> {@code keyDown.isDown()}.</li>
 *   <li>{@code mc.isSingleplayer()} -> {@code mc.hasSingleplayerServer()}.</li>
 *   <li>{@code mc.player.getEntityId()} -> {@code mc.player.getId()}.</li>
 * </ul>
 * Key registrations are kept; the {@code MouseEvent} subscriber is stubbed and must be wired to
 * {@code InputEvent.MouseScrollingEvent} in Phase 10.
 */
public class KeyHandler {
    public static final KeyMapping KEY_ENGINE_ON = new KeyMapping("key.startEngine", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories." + DynamXConstants.ID);
    public static final KeyMapping KEY_HANDBRAKE = new KeyMapping("key.brake", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_SPACE, "key.categories." + DynamXConstants.ID);
    public static final KeyMapping KEY_SPEED_LIMITIER = new KeyMapping("key.speedlimit", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories." + DynamXConstants.ID);
    public static final KeyMapping KEY_LOCK_DOOR = new KeyMapping("key.toggleLockDoor", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, "key.categories." + DynamXConstants.ID);

    public static final KeyMapping KEY_CAMERA_MODE = new KeyMapping("key.cammode", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_MULTIPLY, "key.categories." + DynamXConstants.ID);
    public static final KeyMapping KEY_ZOOM_IN = new KeyMapping("key.camin", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_ADD, "key.categories." + DynamXConstants.ID);
    public static final KeyMapping KEY_ZOOM_OUT = new KeyMapping("key.camout", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_SUBTRACT, "key.categories." + DynamXConstants.ID);
    public static final KeyMapping KEY_WATCH_BEHIND = new KeyMapping("key.watchbehind", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, "key.categories." + DynamXConstants.ID);
    public static final KeyMapping KEY_PICK_OBJECT = new KeyMapping("key.pickobject", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories." + DynamXConstants.ID);
    public static final KeyMapping KEY_TAKE_OBJECT = new KeyMapping("key.takeobject", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "key.categories." + DynamXConstants.ID);

    public static final KeyMapping KEY_DEBUG = new KeyMapping("key.debug", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories." + DynamXConstants.ID);

    public static final KeyMapping KEY_POWERUP = new KeyMapping("key.powerup", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_CAPS_LOCK, "key.categories." + DynamXConstants.ID);
    public static final KeyMapping KEY_POWERDOWN = new KeyMapping("key.powerdown", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, "key.categories." + DynamXConstants.ID);
    public static final KeyMapping KEY_HELICOPTER_PITCH_FORWARD = new KeyMapping("key.helicopter_pitch_forward", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_8, "key.categories." + DynamXConstants.ID);
    public static final KeyMapping KEY_HELICOPTER_PITCH_BACKWARD = new KeyMapping("key.helicopter_pitch_backward", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_5, "key.categories." + DynamXConstants.ID);
    public static final KeyMapping KEY_HELICOPTER_YAW_LEFT = new KeyMapping("key.helicopter_yaw_left", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_4, "key.categories." + DynamXConstants.ID);
    public static final KeyMapping KEY_HELICOPTER_YAW_RIGHT = new KeyMapping("key.helicopter_yaw_right", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_6, "key.categories." + DynamXConstants.ID);
    public static final KeyMapping KEY_LOCK_ROTATION = new KeyMapping("key.lock_rotation", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_1, "key.categories." + DynamXConstants.ID);

    public static final KeyMapping KEY_ATTACH_TRAILER = new KeyMapping("key.attachTrailer", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories." + DynamXConstants.ID);

    /** All keymappings, registered through RegisterKeyMappingsEvent. */
    public static final KeyMapping[] ALL_KEYS = {
            KEY_HANDBRAKE, KEY_ENGINE_ON, KEY_SPEED_LIMITIER, KEY_LOCK_DOOR, KEY_ATTACH_TRAILER,
            KEY_CAMERA_MODE, KEY_ZOOM_IN, KEY_ZOOM_OUT, KEY_DEBUG, KEY_WATCH_BEHIND, KEY_PICK_OBJECT,
            KEY_TAKE_OBJECT, KEY_POWERUP, KEY_POWERDOWN, KEY_HELICOPTER_PITCH_FORWARD,
            KEY_HELICOPTER_PITCH_BACKWARD, KEY_HELICOPTER_YAW_LEFT, KEY_HELICOPTER_YAW_RIGHT,
            KEY_LOCK_ROTATION
    };

    private final Minecraft mc;
    private int holdingDown;
    private boolean justPressed;

    public KeyHandler(Minecraft minecraft) {
        this.mc = minecraft;
        // Registration moved to RegisterKeyMappingsEvent in ClientProxy.
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void tick(TickEvent.ClientTickEvent event) {
        if ((mc.player != null) && (event.phase == TickEvent.Phase.START)) {
            if (WalkingOnPlayerController.controller != null && (MC.player.isPassenger() || MC.options.keyUp.isDown() || MC.options.keyDown.isDown() || MC.options.keyLeft.isDown() || MC.options.keyRight.isDown() || MC.options.keyJump.isDown())) {
                WalkingOnPlayerController.controller.disable();
            }
            controlCamera();

            if (KEY_DEBUG.consumeClick()) {
                if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.connection != null) {
                    Minecraft.getInstance().player.connection.sendCommand("dynamx debug_gui");
                }
            }

            if (KEY_PICK_OBJECT.isDown() && MC.player.getVehicle() == null && MC.player.getMainHandItem().isEmpty()) {
                if (!DynamXContext.getPlayerPickingObjects().containsKey(MC.player.getId())) {
                    if (MC.hasSingleplayerServer()) {
                        PickingObjectHelper.handlePickingControl(new MovableModule.Action(MovableModule.EnumAction.PICK, 3), MC.player);
                    } else {
                        DynamXContext.getNetwork().sendToServer(new MessagePickObject(new MovableModule.Action(MovableModule.EnumAction.PICK, 3)));
                    }
                }
            } else {
                if (DynamXContext.getPlayerPickingObjects().containsKey(MC.player.getId())) {
                    if (MC.hasSingleplayerServer()) {
                        PickingObjectHelper.handlePickingControl(new MovableModule.Action(MovableModule.EnumAction.UNPICK), MC.player);
                    } else {
                        DynamXContext.getNetwork().sendToServer(new MessagePickObject(new MovableModule.Action(MovableModule.EnumAction.UNPICK)));
                    }
                }
            }

            if (KEY_LOCK_DOOR.consumeClick()) {
                Entity entity = mc.player.getVehicle();
                if (entity instanceof BaseVehicleEntity && entity instanceof IModuleContainer.IDoorContainer && ((IModuleContainer.IDoorContainer) entity).getDoors() != null) {
                    // TODO port:1.20.1 - IModuleContainer typed as Object pending entity port.
                    BasePartSeat<?, ?> seat = ((fr.dynamx.common.entities.modules.SeatsModule) ((IModuleContainer.ISeatsContainer) entity).getSeats()).getRidingSeat(MC.player);
                    if (seat == null)
                        return;
                    PartDoor door = seat.getLinkedPartDoor();
                    if (door == null)
                        return;
                    DoorsModule doors = (DoorsModule) ((IModuleContainer.IDoorContainer) entity).getDoors();
                    DynamXContext.getNetwork().sendToServer(new MessageChangeDoorState((BaseVehicleEntity<?>) entity, doors.getInverseCurrentState(door.getId()), door.getId()));
                }
            }

            // TODO port:1.20.1 - MC.objectMouseOver -> MC.hitResult; entityHit -> ((EntityHitResult) hit).getEntity().
            if (MC.hitResult != null) {
                Entity entityHit = null; // TODO port:1.20.1 - extract from MC.hitResult if it's an EntityHitResult.
                if (KEY_TAKE_OBJECT.isDown()) {
                    if (holdingDown == 0) {
                        if (MC.player.getVehicle() == null && MC.player.getMainHandItem().isEmpty() && !DynamXContext.getPlayerPickingObjects().containsKey(MC.player.getId())) {
                            if (entityHit != null) {
                                justPressed = true;
                                if (MC.hasSingleplayerServer()) {
                                    PickingObjectHelper.handlePickingControl(new MovableModule.Action(MovableModule.EnumAction.TAKE, entityHit.getId()), MC.player);
                                } else {
                                    DynamXContext.getNetwork().sendToServer(new MessagePickObject(new MovableModule.Action(MovableModule.EnumAction.TAKE, entityHit.getId())));
                                }
                            }
                        }
                    }
                    holdingDown++;
                } else {
                    if (holdingDown > 10) {
                        if (DynamXContext.getPlayerPickingObjects().containsKey(MC.player.getId())) {
                            if (MC.hasSingleplayerServer()) {
                                PickingObjectHelper.handlePickingControl(new MovableModule.Action(MovableModule.EnumAction.THROW, holdingDown), MC.player);
                            } else {
                                DynamXContext.getNetwork().sendToServer(new MessagePickObject(new MovableModule.Action(MovableModule.EnumAction.THROW, holdingDown)));
                            }
                        }
                        holdingDown = 0;
                    } else if (holdingDown > 0) {
                        if (!justPressed) {
                            if (DynamXContext.getPlayerPickingObjects().containsKey(MC.player.getId())) {
                                if (MC.hasSingleplayerServer()) {
                                    PickingObjectHelper.handlePickingControl(new MovableModule.Action(MovableModule.EnumAction.UNTAKE), MC.player);
                                } else {
                                    DynamXContext.getNetwork().sendToServer(new MessagePickObject(new MovableModule.Action(MovableModule.EnumAction.UNTAKE)));
                                }
                            }
                        } else {
                            justPressed = false;
                        }
                        holdingDown = 0;
                    }
                }
            }
        }
    }

    private void controlCamera() {
        Entity entity = mc.player.getVehicle();
        if (!(entity instanceof IModuleContainer.ISeatsContainer))
            return;
        if (KEY_ZOOM_IN.consumeClick()) {
            CameraSystem.changeCameraZoom(false);
        }
        if (KEY_ZOOM_OUT.consumeClick()) {
            CameraSystem.changeCameraZoom(true);
        }
        if (KEY_CAMERA_MODE.consumeClick()) {
            // TODO port:1.20.1 - mc.ingameGUI.setOverlayMessage -> mc.gui.setOverlayMessage(Component, boolean).
            mc.gui.setOverlayMessage(net.minecraft.network.chat.Component.literal("Vehicle camera mode : " + CameraSystem.cycleCameraMode((IModuleContainer.ISeatsContainer) entity)), true);
        }
        CameraSystem.setWatchingBehind(KEY_WATCH_BEHIND.isDown());
    }

    /**
     * <p>TODO port:1.20.1 - was {@code MouseEvent} (mixed wheel/click). 1.20 splits into:
     * {@code InputEvent.MouseScrollingEvent} (wheel) and {@code InputEvent.MouseButton.Pre/Post} (clicks).
     * The body reacts to wheel scrolls when holding a wrench/slopes item while sneaking.
     * TODO port:1.20.1 - re-add @SubscribeEvent once the parameter is typed as a real Event subclass.</p>
     */
    public void onMouseEvent(/* InputEvent.MouseScrollingEvent */ Object event) {
        if (MC.player != null) {
            if (MC.player.getMainHandItem().getItem() instanceof ItemWrench) {
                if (MC.player.isShiftKeyDown()) {
                    // TODO port:1.20.1 - if (event.getScrollDelta() != 0) { send packet; event.setCanceled(true); }
                    @SuppressWarnings("unused") Class<?> debug = MessageDebugRequest.class; // keep usage
                }
            } else if (MC.player.isShiftKeyDown() && MC.player.getMainHandItem().getItem() instanceof ItemSlopes) {
                // TODO port:1.20.1 - if (event.getScrollDelta() != 0) { send packet; event.setCanceled(true); }
            }
        }
    }
}
