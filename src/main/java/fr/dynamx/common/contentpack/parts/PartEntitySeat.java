package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A seat that can be used on vehicles.
 *
 * TODO port:1.20.1 - Original referenced (Phase 6/7):
 *   - fr.dynamx.api.entities.IModuleContainer (ISeatsContainer/IDoorContainer)
 *   - fr.dynamx.api.entities.modules.ModuleListBuilder
 *   - fr.dynamx.client.renders.scene.* (SceneBuilder/SceneNode/SimpleNode/BaseRenderContext/IRenderContext)
 *   - fr.dynamx.client.renders.RenderPhysicsEntity
 *   - fr.dynamx.client.handlers.ClientEventHandler
 *   - fr.dynamx.common.entities.{BaseVehicleEntity, PackPhysicsEntity}
 *   - fr.dynamx.common.entities.modules.{DoorsModule, SeatsModule}
 *   - fr.dynamx.common.entities.vehicles.{CarEntity, HelicopterEntity}
 *   - GlStateManager / RenderGlobal / RenderPlayer / MinecraftForgeClient (removed in 1.20.1)
 *   The interact() / addModules() / createSceneGraph() / inner PartEntitySeatNode are stubbed.
 *   The first generic A of BasePartSeat was BaseVehicleEntity; relaxed to Object.
 */
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
        // TODO port:1.20.1 - Original interacted with:
        //   - IModuleContainer.ISeatsContainer to get/set the SeatsModule passenger map
        //   - IModuleContainer.IDoorContainer to coordinate door open/close on mount
        //   - CarEntity-specific door logic
        //   All depend on Phase 6 modules/entities. Returning false until then.
        if (player != null) {
            player.sendSystemMessage(Component.literal("Seat interact not available (Phase 6 not ported)"));
        }
        return false;
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
    public void addModules(Object entity, Object modules) {
        // TODO port:1.20.1 - Original:
        //   if (!(entity instanceof IModuleContainer.ISeatsContainer)) throw ...
        //   if (entity instanceof HelicopterEntity) return;
        //   if (!modules.hasModuleOfClass(SeatsModule.class)) modules.add(new SeatsModule(entity));
    }

    @Override
    public void addToSceneGraph(IPhysicsPackInfo packInfo, Object sceneBuilder) {
        // TODO port:1.20.1 - Original delegated to SceneBuilder.addNode(packInfo, this, nodeDependingOnName?).
    }

    @Override
    public Object createSceneGraph(Vector3f modelScale, List<Object> childGraph) {
        // TODO port:1.20.1 - Original returned new PartEntitySeatNode<>(this, modelScale, (List) childGraph).
        return null;
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
}
