package fr.dynamx.utils.debug.renderer;

import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.contentpack.parts.PartStorage;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.MutableBoundingBox;

import java.util.List;

/**
 * A debug renderer for a {@link fr.dynamx.client.renders.RenderPhysicsEntity}
 *
 * <p>TODO port:1.20.1 - All draws using {@code RenderGlobal.drawBoundingBox(...)} were core-profile
 * immediate-mode calls. In 1.20.1 use {@code LevelRenderer.renderLineBox(PoseStack, VertexConsumer, ...)}
 * with a {@code MultiBufferSource}. All draw bodies are stubbed for now.</p>
 *
 * @deprecated The debug should be rendered using the new {@link SceneNode}s system
 * @param <T> The entity type
 * @see VehicleDebugRenderer
 * @see BoatDebugRenderer
 */
@Deprecated
public interface DebugRenderer<T extends PhysicsEntity<?>> {
    /**
     * @return True to render at this frame
     */
    boolean shouldRender(T entity);

    /**
     * Return true to automatically render with the entity's rotation, optimized to keep the same transform for each DebugRenderer
     *
     * @return True by default
     */
    default boolean hasEntityRotation(T entity) {
        return true;
    }

    /**
     * Renders this debug
     */
    void render(T entity, RenderPhysicsEntity<T> renderer, double x, double y, double z, float partialTicks);

    /**
     * Entity collisions shapes render
     */
    class ShapesDebug implements DebugRenderer<PhysicsEntity<?>> {
        @Override
        public boolean shouldRender(PhysicsEntity<?> entity) {
            return entity instanceof PackPhysicsEntity && DynamXDebugOptions.PLAYER_TO_OBJECT_COLLISION_DEBUG.isActive();
        }

        @Override
        public void render(PhysicsEntity<?> entity, RenderPhysicsEntity<PhysicsEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            // TODO port:1.20.1 - replace RenderGlobal.drawBoundingBox with LevelRenderer.renderLineBox + PoseStack/VertexConsumer.
            for (IShapeInfo shapeInfo : ((PackPhysicsEntity<?, ?>) entity).getPackInfo().getCollisionsHelper().getShapes()) {
                // immediate-mode draw stubbed; coordinates kept for future port:
                //   (shapeInfo.getPosition() - shapeInfo.getSize()) to (shapeInfo.getPosition() + shapeInfo.getSize())
                //   color: cyan (0, 1, 1, 1)
                shapeInfo.getPosition();
                shapeInfo.getSize();
            }
        }
    }

    /**
     * Renders seats
     */
    class StoragesDebug implements DebugRenderer<PackPhysicsEntity<?, ?>> {
        @Override
        public boolean shouldRender(PackPhysicsEntity<?, ?> entity) {
            return DynamXDebugOptions.SEATS_AND_STORAGE.isActive();
        }

        @Override
        public void render(PackPhysicsEntity<?, ?> entity, RenderPhysicsEntity<PackPhysicsEntity<?, ?>> renderer, double x, double y, double z, float partialTicks) {
            // TODO port:1.20.1 - replace RenderGlobal.drawBoundingBox with LevelRenderer.renderLineBox.
            MutableBoundingBox box = new MutableBoundingBox();
            for (PartStorage storage : (List<PartStorage>) entity.getPackInfo().getPartsByType(PartStorage.class)) {
                storage.getBox(box);
                box.offset(storage.getPosition());
                // immediate-mode draw stubbed: orange (1, 0.7f, 0, 1)
            }
        }
    }
}
