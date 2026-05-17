package fr.dynamx.common.contentpack.parts;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.scene.BaseRenderContext;
import fr.dynamx.client.renders.scene.SceneBuilder;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.client.renders.scene.node.SimpleNode;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.entities.vehicles.CarEntity;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.utils.EnumSeatPlayerPosition;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.List;

@Setter
@RegisteredSubInfoType(name = "seat", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartEntitySeat extends BasePartSeat<Object, ModularVehicleInfo> implements IDrawablePart<IPhysicsPackInfo> {
    @PackFileProperty(configNames = "Driver")
    protected boolean isDriver;

    @Getter
    @Nullable
    @PackFileProperty(configNames = "LinkedDoorPart", required = false)
    protected String linkedDoor;

    @PackFileProperty(configNames = "DependsOnNode", required = false, description = "PartEntitySeat.DependsOnNode")
    protected String nodeDependingOnName;

    @PackFileProperty(configNames = "InteractPosition", required = false, defaultValue = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    protected Vector3f interactPosition;

    public PartEntitySeat(ModularVehicleInfo owner, String partName) {
        super(owner, partName);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        super.appendTo(owner);
        if (interactPosition != null) {
            interactPosition.multLocal(getScaleModifier(this.owner));
        }
    }

    @Override
    public boolean interact(Object vehicleEntity, Player player) {
        if (!(vehicleEntity instanceof IModuleContainer.ISeatsContainer)) {
            return false;
        }
        SeatsModule seats = (SeatsModule) ((IModuleContainer.ISeatsContainer) vehicleEntity).getSeats();
        if (seats == null) return false;
        Entity seatRider = seats.getSeatToPassengerMap().get(this);
        if (seatRider != null && seatRider != player) {
            player.sendSystemMessage(Component.literal("The seat is already taken"));
            return false;
        }
        if (!hasDoor()) {
            return mountEntity(vehicleEntity, seats, player);
        }
        if (!(vehicleEntity instanceof CarEntity)) {
            return false;
        }
        PartDoor door = getLinkedPartDoor();
        if (door == null) {
            DynamXMain.log.error("Cannot mount : part door not found : " + linkedDoor);
            return false;
        }
        IModuleContainer.IDoorContainer doorContainer = (IModuleContainer.IDoorContainer) vehicleEntity;
        DoorsModule doors = (DoorsModule) doorContainer.getDoors();
        if (door.isPlayerMounting() || doors == null) {
            return false;
        }
        if (door.isEnabled() && !doors.isDoorAttached(door.getId())) {
            return false;
        }
        if (!door.isEnabled() || doors.isDoorOpened(door.getId())) {
            boolean didMount = mountEntity(vehicleEntity, seats, player);
            if (didMount) {
                doors.setDoorState(door.getId(), DoorsModule.DoorState.CLOSING);
            }
            return didMount;
        } else {
            return door.interact(vehicleEntity, player);
        }
    }

    @Override
    public boolean hasDoor() {
        return getLinkedDoor() != null;
    }

    @Nullable
    @Override
    public PartDoor getLinkedPartDoor() {
        return getLinkedDoor() == null ? null : getOwner().getPartsByType(PartDoor.class).stream().filter(partDoor -> partDoor.getPartName().equalsIgnoreCase(getLinkedDoor())).findFirst().orElse(null);
    }

    @Override
    public void postLoad(ModularVehicleInfo owner, boolean hot) {
        super.postLoad(owner, hot);
        if (hasDoor() && getLinkedPartDoor() == null) {
            DynamXErrorManager.addPackError(getPackName(), "seat_door_not_found", ErrorLevel.HIGH, getName(), "Door " + getLinkedDoor() + " not found in " + owner.getFullName());
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void addModules(Object entity, Object modules) {
        if (!(entity instanceof IModuleContainer.ISeatsContainer)) return;
        if (entity instanceof HelicopterEntity) return; // Helicopters have their own SeatsModule
        if (modules instanceof ModuleListBuilder) {
            ModuleListBuilder list = (ModuleListBuilder) modules;
            if (!list.hasModuleOfClass(SeatsModule.class) && entity instanceof PackPhysicsEntity) {
                list.add(new SeatsModule((PackPhysicsEntity<?, ?>) entity));
            }
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addToSceneGraph(IPhysicsPackInfo packInfo, Object sceneBuilder) {
        SceneBuilder<?, IPhysicsPackInfo> builder = (SceneBuilder<?, IPhysicsPackInfo>) sceneBuilder;
        if (nodeDependingOnName != null) {
            builder.addNode(packInfo, this, nodeDependingOnName);
        } else {
            builder.addNode(packInfo, this);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object createSceneGraph(Vector3f modelScale, List<Object> childGraph) {
        return new PartEntitySeatNode<>(this, modelScale, (List) childGraph);
    }

    @Override
    public String getNodeName() {
        return getPartName();
    }

    @Override
    public String getObjectName() {
        return null;
    }

    @Override
    public Vector3f getPosition() {
        return interactPosition != null ? interactPosition : super.getPosition();
    }

    public Vector3f getRelativeRenderPosition() {
        return super.getPosition();
    }

    @Override
    public boolean isDriver() {
        return isDriver;
    }

    class PartEntitySeatNode<A extends IPhysicsPackInfo> extends SimpleNode<BaseRenderContext.EntityRenderContext, A> {
        public PartEntitySeatNode(PartEntitySeat seat, Vector3f scale, List<SceneNode<BaseRenderContext.EntityRenderContext, A>> linkedChilds) {
            super(seat.getRelativeRenderPosition(), seat.getRotation(), PartEntitySeat.this.isAutomaticPosition, scale, linkedChilds);
        }

        @Override
        public void render(BaseRenderContext.EntityRenderContext context, A packInfo, Matrix4f parentTransform) {
            if (!(context.getEntity() instanceof IModuleContainer.ISeatsContainer)) {
                renderChildren(context, packInfo, parentTransform);
                return;
            }
            SeatsModule seats = (SeatsModule) ((IModuleContainer.ISeatsContainer) context.getEntity()).getSeats();
            if (seats == null) {
                renderChildren(context, packInfo, parentTransform);
                return;
            }
            Entity seatRider = seats.getSeatToPassengerMap().get(PartEntitySeat.this);
            Minecraft mc = Minecraft.getInstance();
            if (seatRider == null || (seatRider == mc.player && mc.options.getCameraType().isFirstPerson())) {
                renderChildren(context, packInfo, parentTransform);
                return;
            }

            ClientEventHandler.renderingEntity = seatRider.getUUID();
            transformToRotationPoint(parentTransform);

            EnumSeatPlayerPosition position = getPlayerPosition();
            RenderPhysicsEntity.shouldRenderPlayerSitting = position == EnumSeatPlayerPosition.SITTING;

            PoseStack pose = context.getPoseStack();
            if (pose != null) {
                pose.pushPose();
                if (translation != null) {
                    pose.translate(translation.x, translation.y, translation.z);
                }
                if (rotation != null) {
                    pose.mulPose(rotation);
                }
                if (getPlayerSize() != null) {
                    pose.scale(getPlayerSize().x, getPlayerSize().y, getPlayerSize().z);
                }
                if (position == EnumSeatPlayerPosition.LYING) {
                    pose.mulPose(Axis.XP.rotation(FastMath.PI / 2));
                }

                float partialTicks = context.getPartialTicks();
                EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
                try {
                    dispatcher.render(seatRider, 0, 0, 0, seatRider.getYRot(), partialTicks, pose,
                            context.getBufferSource(), context.getPackedLight());
                } catch (Throwable t) {
                    DynamXMain.log.error("Failed to render seat passenger " + seatRider, t);
                }

                pose.popPose();
            }
            ClientEventHandler.renderingEntity = null;

            renderChildren(context, packInfo, transform);
        }
    }
}
