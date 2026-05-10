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
     *
     * TODO port:1.20.1 - PackPhysicsEntity / ModuleListBuilder live in not-yet-ported packages
     *   (Phase 6); typed as Object until those are ported.
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
