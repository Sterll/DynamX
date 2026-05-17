package fr.dynamx.utils.debug.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.client.renders.RenderFrame;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.contentpack.parts.PartStorage;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;

import java.util.List;

/**
 * A debug renderer for a {@link fr.dynamx.client.renders.RenderPhysicsEntity}
 *
 * @deprecated The debug should be rendered using the new {@link SceneNode}s system
 * @param <T> The entity type
 * @see VehicleDebugRenderer
 * @see BoatDebugRenderer
 */
@Deprecated
public interface DebugRenderer<T extends PhysicsEntity<?>> {
    boolean shouldRender(T entity);

    default boolean hasEntityRotation(T entity) {
        return true;
    }

    void render(T entity, RenderPhysicsEntity<T> renderer, double x, double y, double z, float partialTicks);

    class ShapesDebug implements DebugRenderer<PhysicsEntity<?>> {
        @Override
        public boolean shouldRender(PhysicsEntity<?> entity) {
            return entity instanceof PackPhysicsEntity && DynamXDebugOptions.PLAYER_TO_OBJECT_COLLISION_DEBUG.isActive();
        }

        @Override
        public void render(PhysicsEntity<?> entity, RenderPhysicsEntity<PhysicsEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            RenderFrame.Frame frame = RenderFrame.current();
            if (frame == null) return;
            PoseStack pose = frame.poseStack();
            VertexConsumer consumer = frame.bufferSource().getBuffer(RenderType.lines());
            for (IShapeInfo shapeInfo : ((PackPhysicsEntity<?, ?>) entity).getPackInfo().getCollisionsHelper().getShapes()) {
                com.jme3.math.Vector3f pos = shapeInfo.getPosition();
                com.jme3.math.Vector3f size = shapeInfo.getSize();
                LevelRenderer.renderLineBox(pose, consumer,
                        pos.x - size.x, pos.y - size.y, pos.z - size.z,
                        pos.x + size.x, pos.y + size.y, pos.z + size.z,
                        0f, 1f, 1f, 1f);
            }
        }
    }

    class StoragesDebug implements DebugRenderer<PackPhysicsEntity<?, ?>> {
        @Override
        public boolean shouldRender(PackPhysicsEntity<?, ?> entity) {
            return DynamXDebugOptions.SEATS_AND_STORAGE.isActive();
        }

        @Override
        public void render(PackPhysicsEntity<?, ?> entity, RenderPhysicsEntity<PackPhysicsEntity<?, ?>> renderer, double x, double y, double z, float partialTicks) {
            RenderFrame.Frame frame = RenderFrame.current();
            if (frame == null) return;
            PoseStack pose = frame.poseStack();
            VertexConsumer consumer = frame.bufferSource().getBuffer(RenderType.lines());
            MutableBoundingBox box = new MutableBoundingBox();
            for (PartStorage storage : (List<PartStorage>) entity.getPackInfo().getPartsByType(PartStorage.class)) {
                storage.getBox(box);
                box.offset(storage.getPosition());
                LevelRenderer.renderLineBox(pose, consumer,
                        box.minX, box.minY, box.minZ,
                        box.maxX, box.maxY, box.maxZ,
                        1f, 0.7f, 0f, 1f);
            }
        }
    }
}
