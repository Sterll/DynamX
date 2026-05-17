package fr.dynamx.client.renders;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.entities.modules.movables.PickObjects;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/**
 * Rendu de la ligne de prise (player {@literal ->} entite physique attrapee).
 *
 * <p>Pipeline 1.20.1 : on emet une primitive LINES via {@link RenderType#lines()} sur le
 * {@link MultiBufferSource} fourni par {@code RenderLevelStageEvent}, en s'appuyant sur le
 * {@link PoseStack} deja translate dans l'espace camera. Plus aucun GlStateManager ni
 * immediate mode.
 */
public class RenderMovableLine {

    public static boolean hasMovableLines() {
        return !DynamXContext.getPlayerPickingObjects().isEmpty();
    }

    public static void renderLine(PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        var pose = poseStack.last();
        var matrix = pose.pose();
        var normalMatrix = pose.normal();

        for (Map.Entry<Integer, Integer> entry : DynamXContext.getPlayerPickingObjects().entrySet()) {
            int playerEntityId = entry.getKey();
            int physicsEntityId = entry.getValue();
            // Seule la ligne du joueur local est tracee : le pivot des autres n'est pas synchronise localement.
            if (playerEntityId != mc.player.getId()) {
                continue;
            }
            Entity rawPlayer = mc.level.getEntity(playerEntityId);
            Entity rawPhysics = mc.level.getEntity(physicsEntityId);
            if (!(rawPlayer instanceof Player player) || !(rawPhysics instanceof PhysicsEntity<?> physicsEntity)) {
                continue;
            }
            MovableModule movableModule = physicsEntity.getModuleByType(MovableModule.class);
            if (!(movableModule instanceof PickObjects pickObjects)) {
                continue;
            }
            PhysicsRigidBody hitBody = pickObjects.getHitBody();
            if (hitBody == null) {
                continue;
            }

            Vector3fPool.openPool();
            QuaternionPool.openPool();
            try {
                Vector3f physicsLocation = ClientDebugSystem.getInterpolatedTranslation(hitBody, partialTicks);
                Quaternion physicsRotation = ClientDebugSystem.getInterpolatedRotation(hitBody, partialTicks);

                Vector3f firstPersonOffset = Vector3fPool.get(0, 0, 0.35f);
                float interYaw = player.yRotO + (player.getYRot() - player.yRotO) * partialTicks;
                float interPitch = player.xRotO + (player.getXRot() - player.xRotO) * partialTicks;
                Vector3f firstPersonOffsetRot = DynamXGeometry.getRotatedPoint(firstPersonOffset, interPitch, interYaw, 0);

                Vector3f cam = DynamXUtils.getCameraTranslation(mc, partialTicks);
                Vector3f target = Vector3fPool.get(cam).add(firstPersonOffsetRot);
                target.subtractLocal(physicsLocation.x, physicsLocation.y - player.getEyeHeight(), physicsLocation.z);

                Vector3f pivot = DynamXGeometry.rotateVectorByQuaternion(pickObjects.getLocalPickPosition(), physicsRotation);

                float ox = physicsLocation.x;
                float oy = physicsLocation.y;
                float oz = physicsLocation.z;

                consumer.vertex(matrix, ox + target.x, oy + target.y, oz + target.z)
                        .color(0.2f, 0.9f, 1.0f, 1.0f)
                        .normal(normalMatrix, 0f, 1f, 0f)
                        .endVertex();
                consumer.vertex(matrix, ox + pivot.x, oy + pivot.y, oz + pivot.z)
                        .color(0.2f, 0.9f, 1.0f, 1.0f)
                        .normal(normalMatrix, 0f, 1f, 0f)
                        .endVertex();
            } finally {
                QuaternionPool.closePool();
                Vector3fPool.closePool();
            }
        }
    }
}
