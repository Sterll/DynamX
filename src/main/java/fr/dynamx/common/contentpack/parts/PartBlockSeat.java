package fr.dynamx.common.contentpack.parts;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * A seat that can be used on block and props.
 *
 * TODO port:1.20.1 - Original referenced:
 *   - fr.dynamx.api.entities.IModuleContainer (Phase 6)
 *   - fr.dynamx.api.entities.modules.ModuleListBuilder (Phase 6)
 *   - fr.dynamx.common.blocks.TEDynamXBlock (Phase 4/6)
 *   - fr.dynamx.common.entities.{IDynamXObject, PackPhysicsEntity, PropsEntity, SeatEntity} (Phase 6)
 *   - fr.dynamx.common.entities.modules.SeatsModule (Phase 6)
 *   - net.minecraft.util.text.TextComponentString -> net.minecraft.network.chat.Component.literal
 *   The interact() body cannot be implemented before Phase 6; it returns false and the
 *   `with.sendSystemMessage(Component.literal(...))` call is kept for the Player branch only.
 *
 * @param <T> The owner type of this part. The vehicle-entity generic A is relaxed to Object.
 */
@RegisteredSubInfoType(name = "seat", registries = {SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS}, strictName = false)
public class PartBlockSeat<T extends ISubInfoTypeOwner<T>> extends BasePartSeat<Object, T> {
    public PartBlockSeat(T owner, String partName) {
        super(owner, partName);
    }

    @Override
    public boolean interact(Object entity, Player with) {
        // TODO port:1.20.1 - Original handled TEDynamXBlock and PropsEntity branches:
        //   if (entity instanceof TEDynamXBlock) { ... with.startRiding(seatEntity); }
        //   if (entity instanceof PropsEntity) { ... mountEntity(vehicleEntity, seats, with); }
        //   Both branches depend on Phase 6 entities; return false until then.
        if (with != null) {
            // Keep a placeholder system message so the call site is observable in client code.
            with.sendSystemMessage(Component.literal("The seat is not available (Phase 6 not ported)"));
        }
        return false;
    }

    @Override
    public void addModules(Object entity, Object modules) {
        // TODO port:1.20.1 - Original:
        //   if (!(entity instanceof IModuleContainer.ISeatsContainer))
        //       throw new IllegalStateException(...);
        //   if (!modules.hasModuleOfClass(SeatsModule.class))
        //       modules.add(new SeatsModule(entity));
        //   SeatsModule / IModuleContainer live in Phase 6.
    }
}
