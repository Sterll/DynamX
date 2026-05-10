package fr.dynamx.api.contentpack.object.part;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Interactive part of a pack object.
 *
 * TODO port:1.20.1 - The first generic A originally was IDynamXObject (fr.dynamx.common.entities,
 *   Phase 6). Relaxed to Object until that interface is ported.
 */
public abstract class InteractivePart<A, T extends ISubInfoTypeOwner<T>> extends BasePart<T> {
    /**
     * The box used for interaction and raytracing
     */
    @Setter
    private AABB box;

    public InteractivePart(T owner, String partName) {
        super(owner, partName);
    }

    public InteractivePart(T owner, String partName, float halfWidth, float halfHeight) {
        super(owner, partName, new Vector3f(halfWidth, halfHeight, halfWidth));
    }

    public InteractivePart(T owner, String partName, Vector3f halfBoxSize) {
        super(owner, partName, halfBoxSize);
    }

    /**
     * Fill the given box with the interaction and raytracing box of this part
     */
    public void getBox(MutableBoundingBox out) {
        out.setTo(box);
    }

    @Override
    public void appendTo(T owner) {
        super.appendTo(owner);
        box = new AABB(-getScale().x, 0, -getScale().z, getScale().x, getScale().y, getScale().z);
    }

    /**
     * @return The texture to display on the cursor when the player is looking at this part
     */
    public ResourceLocation getHudCursorTexture() {
        return null;
    }

    /**
     * Handles interaction with this part
     *
     * @return True if interacted with success
     */
    public abstract boolean interact(A with, Player player);

    /**
     * Checks if the player can interact with this part
     */
    public boolean canInteract(A with, Player player) {
        return true;
    }
}
