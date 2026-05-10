package fr.dynamx.common.entities.modules.movables;

import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.EnumBulletShapeType;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.DoorsModule;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.items.tools.ItemWrench;
import fr.dynamx.common.network.packets.MessageSyncPlayerPicking;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.PhysicsRaycastResult;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.function.Predicate;

/**
 * Static helper dispatching {@link MovableModule.Action} player inputs.
 */
// TODO port:1.20.1 - ItemWrench/MessageSyncPlayerPicking forward (Phase 4/5). getHeldItemMainhand ->
// getMainHandItem; capabilities.isCreativeMode -> getAbilities().instabuild; world -> level().
public class PickingObjectHelper {
    public static void handlePickingControl(MovableModule.Action moduleAction, Player player) {
        Level world = player.level();
        if (!player.getAbilities().instabuild && !(player.getMainHandItem().getItem() instanceof ItemWrench)
                && !DynamXConfig.allowPlayersToMoveObjects || moduleAction.getMovableAction() == MovableModule.EnumAction.ATTACH_OBJECTS) {
            return;
        }
        Vector3fPool.openPool();
        QuaternionPool.openPool();
        if (!DynamXContext.getPlayerPickingObjects().containsKey(player.getId())) {
            switch (moduleAction.getMovableAction()) {
                case PICK:
                    startPicking(moduleAction, player);
                    break;
                case TAKE:
                    startTaking(moduleAction, world, player);
                    break;
            }
        } else {
            Entity entity = world.getEntity(DynamXContext.getPlayerPickingObjects().get(player.getId()));
            if (entity instanceof PhysicsEntity) {
                PhysicsEntity<?> physicsEntity = (PhysicsEntity<?>) entity;
                MovableModule movableModule = physicsEntity.getModuleByType(MovableModule.class);
                if (movableModule != null) {
                    switch (movableModule.usingAction) {
                        case PICK:
                            controlPicking(moduleAction, movableModule);
                            break;
                        case TAKE:
                            controlTaking(moduleAction, movableModule);
                            break;
                    }
                }
            } else {
                DynamXContext.getPlayerPickingObjects().remove(player.getId());
            }
        }
        DynamXContext.getNetwork().sendToClientFromOtherThread(new MessageSyncPlayerPicking(new HashMap<>(DynamXContext.getPlayerPickingObjects())), EnumPacketTarget.ALL, null);
        Vector3fPool.closePool();
        QuaternionPool.closePool();
    }

    public static void handlePlayerDisconnection(Player player) {
        Level world = player.level();
        if (!player.getAbilities().instabuild && !(player.getMainHandItem().getItem() instanceof ItemWrench)
                && !DynamXConfig.allowPlayersToMoveObjects) {
            return;
        }
        Entity entity = world.getEntity(DynamXContext.getPlayerPickingObjects().get(player.getId()));
        if (entity instanceof PhysicsEntity) {
            PhysicsEntity<?> physicsEntity = (PhysicsEntity<?>) entity;
            MovableModule movableModule = physicsEntity.getModuleByType(MovableModule.class);
            if (movableModule != null) {
                switch (movableModule.usingAction) {
                    case PICK:
                        controlPicking(new MovableModule.Action(MovableModule.EnumAction.UNPICK), movableModule);
                        break;
                    case TAKE:
                        controlTaking(new MovableModule.Action(MovableModule.EnumAction.UNTAKE), movableModule);
                        break;
                }
            }
        } else {
            DynamXContext.getPlayerPickingObjects().remove(player.getId());
        }
        DynamXContext.getNetwork().sendToClientFromOtherThread(new MessageSyncPlayerPicking(new HashMap<>(DynamXContext.getPlayerPickingObjects())), EnumPacketTarget.ALL, null);
    }

    private static void startPicking(MovableModule.Action moduleAction, Player player) {
        int distanceMax = (int) moduleAction.getInfo()[0];

        Predicate<EnumBulletShapeType> predicateShape = p -> !p.isTerrain() && !p.isPlayer();

        PhysicsRaycastResult result = DynamXUtils.castRayFromEntity(player, distanceMax, predicateShape);

        if (result != null) {
            BulletShapeType<?> shapeType = (BulletShapeType<?>) result.hitBody.getUserObject();
            PhysicsEntity<?> physicsEntity = null;
            if (shapeType.getObjectIn() instanceof PhysicsEntity) {
                physicsEntity = (PhysicsEntity<?>) shapeType.getObjectIn();
            } else if (shapeType.getObjectIn() instanceof DoorsModule.DoorPhysics) {
                physicsEntity = ((DoorsModule.DoorPhysics) shapeType.getObjectIn()).getModule().vehicleEntity;
            }
            if (physicsEntity == null)
                return;
            MovableModule movableModule = physicsEntity.getModuleByType(MovableModule.class);
            if (movableModule != null
                    && (!DynamXContext.getWalkingPlayers().containsKey(player)
                    || physicsEntity.getId() != DynamXContext.getWalkingPlayers().get(player).getId())) {
                movableModule.usingAction = MovableModule.EnumAction.PICK;
                movableModule.pickObjects.pickObject(player, physicsEntity, result.hitBody, result.hitPos,
                        result.hitPos.subtract(result.fromVec).length());
            }
        }
    }

    private static void startTaking(MovableModule.Action moduleAction, Level world, Player player) {
        Entity targetEntity = world.getEntity((int) moduleAction.getInfo()[0]);
        if (targetEntity instanceof PhysicsEntity) {
            MovableModule movableModule = ((PhysicsEntity<?>) targetEntity).getModuleByType(MovableModule.class);
            if (movableModule != null) {
                movableModule.usingAction = MovableModule.EnumAction.TAKE;
                movableModule.moveObjects.pickObject(player, (PhysicsEntity<?>) targetEntity);
            }
        }
    }

    private static void controlPicking(MovableModule.Action moduleAction, MovableModule movableModule) {
        switch (moduleAction.getMovableAction()) {
            case UNPICK:
                movableModule.pickObjects.unPickObject();
                break;
            case LENGTH_CHANGE:
                boolean mouseWheelInc = (boolean) moduleAction.getInfo()[0];
                int distanceMax = (int) moduleAction.getInfo()[1];
                movableModule.pickObjects.getPickDistance().set(Mth.clamp(
                        movableModule.pickObjects.getPickDistance().get() + (mouseWheelInc ? 1 : -1), 1.5f, distanceMax));
                break;
            case FREEZE_OBJECT:
                if (movableModule.pickObjects.getHitBody().getMass() > 0)
                    movableModule.pickObjects.getHitBody().setMass(0);
                break;
        }
    }

    private static void controlTaking(MovableModule.Action moduleAction, MovableModule movableModule) {
        switch (moduleAction.getMovableAction()) {
            case UNTAKE:
                movableModule.moveObjects.unPickObject();
                break;
            case THROW:
                int force = (int) moduleAction.getInfo()[0] / 2;
                force = Math.min(force, 20);
                movableModule.moveObjects.throwObject(force);
                break;
        }
    }
}
