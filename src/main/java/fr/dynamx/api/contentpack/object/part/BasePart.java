package fr.dynamx.api.contentpack.object.part;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.ICollisionsContainer;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;

/**
 * The base of a part. <br>
 * A part is a part of a vehicle, like a wheel, a seat, a door, etc... <br>
 * It has a position and a shape, it can generally be rendered and is sometimes interactive. <br>
 *
 * @param <T> The owner of this part. Should implement ISubInfoTypeOwner&lt;?&gt;.
 */
@Getter
public abstract class BasePart<T extends ISubInfoTypeOwner<T>> extends SubInfoType<T> {
    /**
     * Deprecated properties of BasePart.
     */
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.PROPS})
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        switch (key) {
            case "ShapePosition":
                return new IPackFilePropertyFixer.FixResult("Position", true);
            case "Size":
            case "ShapeScale":
            case "BoxDim":
                return new IPackFilePropertyFixer.FixResult("Scale", true);
        }
        return null;
    };

    /**
     * Unique id for this part (among the group of parts with the same id class).
     */
    @Setter
    private byte id;
    /**
     * The name of this part, given when creating it in the pack.
     */
    private final String partName;

    /**
     * The position of this part, relative to the 3D model.
     */
    @Setter
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, description = "common.position", required = false, defaultValue = "From model")
    protected Vector3f position;
    /**
     * Indicates if the position was read from the 3D model, or set by the user.
     */
    @Setter
    protected boolean isAutomaticPosition;
    /**
     * The scale (size) of this part, relative to the 3D model.
     */
    @Setter
    @PackFileProperty(configNames = "Scale", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED, required = false, description = "common.scale", defaultValue = "0.5 1 0.5")
    protected Vector3f scale = new Vector3f(0.5f, 1, 0.5f);

    public BasePart(ISubInfoTypeOwner<T> owner, String partName) {
        super(owner);
        this.partName = partName;
    }

    public BasePart(ISubInfoTypeOwner<T> owner, String partName, Vector3f scale) {
        this(owner, partName);
        this.scale = scale;
    }

    /**
     * @param owner The owner of the part
     * @return The scale modifier of the owner
     */
    public Vector3f getScaleModifier(T owner) {
        return ((ICollisionsContainer) owner).getScaleModifier();
    }

    /**
     * @return The debug option of this part, or null if there is none.
     *
     * TODO port:1.20.1 - DynamXDebugOption lives in fr.dynamx.utils.debug (not yet ported);
     *   return type relaxed to Object to keep the API surface stable.
     */
    public Object getDebugOption() {
        return null;
    }

    /**
     * If the configured position is null, this method reads the position from the 3D model owning this part.
     *
     * TODO port:1.20.1 - The OBJ loader (fr.dynamx.common.objloader) is being dropped for 1.20.1.
     *   DxModelData / DynamXContext.getDxModelDataFromCache / DynamXUtils.readPartPosition were the
     *   plumbing for the old loader. Method now logs a pack error and returns null.
     */
    public Quaternion readPositionFromModel(ResourceLocation model, String objectName, boolean allowPartCenter, boolean getRotation) {
        // TODO port:1.20.1 - Old OBJ-loader-based path dropped; position must be set explicitly in pack.
        if (getPosition() == null) {
            INamedObject parent = getRootOwner();
            DynamXErrorManager.addPackError(getPackName(), "position_not_found_in_model", ErrorLevel.HIGH, parent.getName(),
                    "3D object '" + objectName + "' of part " + getName() + " (for property 'Position') - OBJ loader removed in 1.20.1, set Position explicitly");
            setPosition(new Vector3f());
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void appendTo(T owner) {
        if (scale == null) {
            INamedObject parent = getRootOwner();
            DynamXErrorManager.addPackError(getPackName(), "required_property", ErrorLevel.HIGH, parent.getName(), "Scale in " + getName());
        } else {
            scale.multLocal(getScaleModifier(this.owner));
        }
        if (position == null) {
            INamedObject parent = getRootOwner();
            DynamXErrorManager.addPackError(getPackName(), "required_property", ErrorLevel.HIGH, parent.getName(), "Position in " + getName());
            position = new Vector3f();
        } else {
            position.multLocal(getScaleModifier(this.owner));
        }
        ((IPartContainer<T>) this.owner).addPart(this);
    }

    /**
     * @return The class used to generate the unique id of this part.
     */
    public Class<?> getIdClass() {
        return getClass();
    }
}
