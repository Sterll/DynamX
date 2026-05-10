package fr.dynamx.api.entities;

import javax.annotation.Nullable;

/**
 * All built-in possible modules <br>
 * The IHaveModule interfaces should be implemented by the BaseVehicleEntity in order to make the modules work
 *
 * @see fr.dynamx.api.entities.modules.IPhysicsModule
 */
// TODO port:1.20.1 - References to BaseVehicleEntity / PackPhysicsEntity / SeatsModule / DoorsModule
// are not yet ported (Phase 6 entities). Method signatures use Object placeholders to preserve the
// public contract until those classes are ported.
public interface IModuleContainer {
    /**
     * Helper method to cast this IHaveModule to an entity
     */
    // TODO port:1.20.1 - return type should be PackPhysicsEntity<?, ?> once entities are ported.
    Object cast();

    interface ISeatsContainer extends IModuleContainer {
        // TODO port:1.20.1 - return type should be SeatsModule once entities/modules are ported.
        @Nullable
        Object getSeats();

        /**
         * Can be false if the seats aren't yet loaded
         *
         * @return true if the vehicle has seats and they are loaded
         */
        default boolean hasSeats() {
            return getSeats() != null;
        }
    }

    interface IDoorContainer extends IModuleContainer {
        // TODO port:1.20.1 - return type should be DoorsModule once entities/modules are ported.
        @Nullable
        Object getDoors();
    }
}
