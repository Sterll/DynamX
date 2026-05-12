package fr.dynamx.common.contentpack.type;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.CylinderCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.utils.errors.DynamXErrorManager;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper that builds the physics collision shape of a pack object.
 *
 * TODO port:1.20.1 - Original referenced:
 *   - fr.dynamx.api.dxmodel.DxModelPath / EnumDxModelFormats (not yet ported)
 *   - fr.dynamx.common.DynamXContext / DxModelData (not yet ported)
 *   - fr.dynamx.utils.physics.ShapeUtils#generateComplexModelCollisions (not yet ported)
 *   The loadCollisions() implementation that read mesh dimensions/centers from the model is
 *   replaced with a stub that only consumes pre-defined PartShape entries until the obj/model
 *   pipeline lands. The signature is relaxed to use Object for the model path argument.
 */
public class ObjectCollisionsHelper {
    private static CompoundCollisionShape EMPTY_COLLISION_SHAPE;

    /**
     * The collision shape of this object, generated either form the partShapes list, or the obj model of the object (hull shape/complex collisions)
     */
    @Getter
    private CompoundCollisionShape physicsCollisionShape;

    /**
     * The shapes of this object, can be used for collisions
     */
    @Getter
    protected final List<IShapeInfo> shapes = new ArrayList<>();

    public void addCollisionShape(IShapeInfo partShape) {
        shapes.add(partShape);
    }

    /**
     * Builds the physics collision shape from configured part shapes.
     *
     * TODO port:1.20.1 - The modelPath parameter is typed as Object because DxModelPath lives in
     *   fr.dynamx.api.dxmodel (not yet ported). When the model pipeline lands, restore the original
     *   signature taking a DxModelPath and re-enable the auto/complex collision branches.
     */
    public void loadCollisions(INamedObject object, Object modelPath, String partName, Vector3f centerOfMass, float shapeYOffset, boolean useComplexCollisions, Vector3f scaleModifier, CollisionType type) {
        // TODO port:1.20.1 - The original method also handled:
        //   - useComplexCollisions=true: ShapeUtils.generateComplexModelCollisions(...)
        //   - empty shapes + obj model: auto-generated BoxCollisionShape per mesh
        //   Both branches depend on DxModelPath/DxModelData/ShapeUtils which are not yet ported.
        try {
            if (getShapes().isEmpty()) {
                // Without the obj pipeline we cannot synthesize shapes; leave physicsCollisionShape null.
                if (type == CollisionType.VEHICLE && !useComplexCollisions) {
                    throw new UnsupportedOperationException("Automatic physics collisions (UseComplexCollisions = false when no PartShape is added) are not supported for vehicles");
                }
                return;
            }
            // TODO port:1.20.1 - Original: !useComplexCollisions branch built shape from PartShapes;
            //  useComplexCollisions=true called ShapeUtils.generateComplexModelCollisions(...) which
            //  reads the OBJ model. Until the model pipeline lands, fall back to PartShapes whenever
            //  they are provided, even when useComplexCollisions is true. This unblocks pack vehicles
            //  that ship a Shape_* fallback (e.g. the karting test pack).
            if (!useComplexCollisions || !getShapes().isEmpty()) {
                physicsCollisionShape = new CompoundCollisionShape();
                getShapes().forEach(shape -> {
                    CollisionShape collisionShape;
                    switch (shape.getShapeType()) {
                        case BOX:
                            collisionShape = new BoxCollisionShape(shape.getSize());
                            break;
                        case CYLINDER:
                            collisionShape = new CylinderCollisionShape(shape.getSize(), 0);
                            break;
                        case SPHERE:
                            collisionShape = new SphereCollisionShape(shape.getSize().x);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + shape.getShapeType());
                    }
                    physicsCollisionShape.addChildShape(collisionShape, new Vector3f(centerOfMass.x, centerOfMass.y, centerOfMass.z).add(shape.getPosition()));
                });
            }
            if (type == CollisionType.BLOCK) {
                getShapes().replaceAll(sh -> new IShapeInfo() {
                    @Override
                    public Vector3f getPosition() {
                        return sh.getPosition().add(0.5f, 1.5f, 0.5f).addLocal(centerOfMass);
                    }

                    @Override
                    public Vector3f getSize() {
                        return sh.getSize();
                    }

                    @Override
                    public MutableBoundingBox getBoundingBox() {
                        return new MutableBoundingBox(sh.getBoundingBox()).offset(0.5, 1.5, 0.5).offset(centerOfMass);
                    }
                });
            }
        } catch (Exception e) {
            DynamXErrorManager.addError(object.getPackName(), DynamXErrorManager.PACKS_ERRORS, "collision_shape_error", ErrorLevel.FATAL, object.getName(), null, e);
            physicsCollisionShape = null;
        }
    }

    public boolean hasPhysicsCollisions() {
        return physicsCollisionShape != null;
    }

    public ObjectCollisionsHelper copy() {
        ObjectCollisionsHelper copy = new ObjectCollisionsHelper();
        copy.shapes.addAll(shapes);
        return copy;
    }

    public enum CollisionType {
        VEHICLE,
        BLOCK,
        PROP
    }

    public static CompoundCollisionShape getEmptyCollisionShape() {
        if (EMPTY_COLLISION_SHAPE == null)
            EMPTY_COLLISION_SHAPE = new CompoundCollisionShape();
        return EMPTY_COLLISION_SHAPE;
    }
}
