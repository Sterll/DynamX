package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.EnumSeatPlayerPosition;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

/**
 * A part that can be used as a seat for a player. <br>
 * Handles player rendering and mounting via the SeatsModule (Phase 6).
 *
 * @param <A> The vehicle entity type. TODO port:1.20.1 - originally bounded by IDynamXObject (Phase 6); relaxed.
 * @param <T> The owner of this part. Should implement ISubInfoTypeOwner&lt;?&gt;.
 * @see PartEntitySeat for a seat that can be used on vehicles
 * @see PartBlockSeat for a seat that can be used on block and props
 *
 * TODO port:1.20.1 - The mountEntity helper originally took a fr.dynamx.common.entities.modules.SeatsModule
 *   (Phase 6). Relaxed to Object until that module is ported.
 */
@Getter
@Setter
public abstract class BasePartSeat<A, T extends ISubInfoTypeOwner<T>> extends InteractivePart<A, T> {
    @Accessors(fluent = true)
    @PackFileProperty(configNames = "ShouldLimitFieldOfView", required = false, defaultValue = "true")
    protected boolean shouldLimitFieldOfView = true;

    @PackFileProperty(configNames = "MaxYaw", required = false, defaultValue = "105")
    protected float maxYaw = 105.0f;

    @PackFileProperty(configNames = "MinYaw", required = false, defaultValue = "-105")
    protected float minYaw = -105.0f;

    @PackFileProperty(configNames = "MaxPitch", required = false, defaultValue = "105")
    protected float maxPitch = 105.0f;

    @PackFileProperty(configNames = "MinPitch", required = false, defaultValue = "-105")
    protected float minPitch = -105.0f;

    @PackFileProperty(configNames = "Rotation", required = false, defaultValue = "1 0 0 0")
    protected Quaternion rotation;

    @PackFileProperty(configNames = "PlayerPosition", required = false, defaultValue = "SITTING")
    protected EnumSeatPlayerPosition playerPosition = EnumSeatPlayerPosition.SITTING;

    @PackFileProperty(configNames = "CameraRotation", required = false, defaultValue = "0")
    protected float rotationYaw;

    @PackFileProperty(configNames = "CameraPositionY", required = false, defaultValue = "0")
    protected float cameraPositionY;

    @PackFileProperty(configNames = "PlayerSize", required = false, defaultValue = "1 1 1")
    protected Vector3f playerSize;

    public BasePartSeat(T owner, String partName) {
        super(owner, partName, 0.4f, 1.8f);
    }

    @Override
    public Object getDebugOption() {
        // TODO port:1.20.1 - return DynamXDebugOptions.SEATS_AND_STORAGE once the debug package is ported.
        return null;
    }

    /**
     * TODO port:1.20.1 - seatsModule was a fr.dynamx.common.entities.modules.SeatsModule (Phase 6);
     *   relaxed to Object. The body that called seatsModule.getSeatToPassengerMap() is stubbed.
     *
     * @return true if the mount succeeded
     */
    public boolean mountEntity(A riddenEntity, Object seatsModule, Entity rider) {
        // TODO port:1.20.1 - Re-enable once SeatsModule (Phase 6) is ported:
        //   if (seatsModule.getSeatToPassengerMap().containsValue(rider)) return false;
        //   seatsModule.getSeatToPassengerMap().put(this, rider);
        //   if (!rider.startRiding((Entity) riddenEntity, false)) {
        //       seatsModule.getSeatToPassengerMap().remove(this);
        //       return false;
        //   }
        //   return true;
        if (riddenEntity instanceof Entity) {
            return rider.startRiding((Entity) riddenEntity, false);
        }
        return false;
    }

    @Override
    public ResourceLocation getHudCursorTexture() {
        return new ResourceLocation(DynamXConstants.ID, "textures/seat.png");
    }

    @Override
    public String getName() {
        return "PartSeat named " + getPartName() + " in " + getOwner().getName();
    }

    public boolean hasDoor() {
        return false;
    }

    @Nullable
    public PartDoor getLinkedPartDoor() {
        return null;
    }

    public boolean isDriver() {
        return false;
    }

    @Override
    public Class<?> getIdClass() {
        return BasePartSeat.class;
    }
}
