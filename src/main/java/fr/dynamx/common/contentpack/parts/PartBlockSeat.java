package fr.dynamx.common.contentpack.parts;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.PropsEntity;
import fr.dynamx.common.entities.modules.SeatsModule;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * A seat that can be used on block and props.
 *
 * TODO port:1.20.1 - TEDynamXBlock.getSeatEntities() not yet exposed; the block-seat branch
 * stays disabled until the SeatEntity wiring on TEDynamXBlock is ported.
 */
@RegisteredSubInfoType(name = "seat", registries = {SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS}, strictName = false)
public class PartBlockSeat<T extends ISubInfoTypeOwner<T>> extends BasePartSeat<Object, T> {
    public PartBlockSeat(T owner, String partName) {
        super(owner, partName);
    }

    @Override
    public boolean interact(Object entity, Player with) {
        if (entity instanceof PropsEntity) {
            PropsEntity<?> vehicleEntity = (PropsEntity<?>) entity;
            if (!(vehicleEntity instanceof IModuleContainer.ISeatsContainer)) return false;
            SeatsModule seats = (SeatsModule) ((IModuleContainer.ISeatsContainer) vehicleEntity).getSeats();
            if (seats == null) return false;
            Entity seatRider = seats.getSeatToPassengerMap().get(this);
            if (seatRider != null && seatRider != with) {
                with.sendSystemMessage(Component.literal("The seat is already taken"));
                return false;
            }
            return mountEntity(vehicleEntity, seats, with);
        }
        // TODO port:1.20.1 - re-enable TEDynamXBlock branch once TEDynamXBlock.getSeatEntities()
        // is ported (uses SeatEntity list indexed by seat id).
        return false;
    }

    @Override
    public void addModules(Object entity, Object modules) {
        if (!(entity instanceof IModuleContainer.ISeatsContainer)) return;
        if (modules instanceof ModuleListBuilder && entity instanceof PackPhysicsEntity) {
            ModuleListBuilder list = (ModuleListBuilder) modules;
            if (!list.hasModuleOfClass(SeatsModule.class)) {
                list.add(new SeatsModule((PackPhysicsEntity<?, ?>) entity));
            }
        }
    }
}
