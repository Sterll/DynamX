package fr.dynamx.api.contentpack.object;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * Describes an ObjectInfo which can be used in a PackPhysicsEntity
 */
public interface IPhysicsPackInfo extends ICollisionsContainer, IModelPackObject {
    /**
     * @return The center of mass of the object
     */
    Vector3f getCenterOfMass();

    /**
     * Adds modules to the given entity.
     * TODO port:1.20.1 - kept loose Object signature for now to match ISubInfoType.addModules; tighten in Phase 6 entity port.
     */
    void addModules(Object entity, Object modules);

    default List<IDrawablePart<?>> getDrawableParts() {
        return Collections.emptyList();
    }

    ItemStack getPickedResult(int metadata);

    float getLinearDamping();

    float getAngularDamping();

    default float getInWaterLinearDamping() {
        return 0.6f;
    }

    default float getInWaterAngularDamping() {
        return 0.6f;
    }

    float getRenderDistanceSquared();
}
