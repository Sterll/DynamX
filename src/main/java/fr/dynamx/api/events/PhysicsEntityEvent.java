package fr.dynamx.api.events;

import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.scene.node.SceneNode;
import fr.dynamx.common.entities.ModularPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.items.DynamXItemSpawner;
import fr.dynamx.utils.debug.renderer.DebugRenderer;
import lombok.Getter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.GenericEvent;
import net.minecraftforge.eventbus.api.IGenericEvent;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Physics entity events.
 */
public class PhysicsEntityEvent extends Event {
    @Getter
    private final Dist side;
    private final PhysicsEntity<?> physicsEntity;

    public PhysicsEntityEvent(Dist side, PhysicsEntity<?> physicsEntity) {
        this.physicsEntity = physicsEntity;
        this.side = side;
    }

    public PhysicsEntity<?> getEntity() {
        return physicsEntity;
    }

    /**
     * Fired when an entity is being spawned
     */
    @Cancelable
    public static class Spawn extends PhysicsEntityEvent {

        @Getter
        private final PhysicsEntity<?> physicsEntity;
        @Getter
        private final Level world;
        @Getter
        @Nullable
        private final Player player;
        @Getter
        private final DynamXItemSpawner<?> itemSpawner;
        @Getter
        private final Vec3 pos;

        public Spawn(Level world, PhysicsEntity<?> physicsEntity, Player player, DynamXItemSpawner<?> item, Vec3 pos) {
            super(Dist.DEDICATED_SERVER, physicsEntity);
            this.world = world;
            this.physicsEntity = physicsEntity;
            this.player = player;
            this.itemSpawner = item;
            this.pos = pos;
        }
    }

    /**
     * Fired on server side when a player tries to kill a physics entity
     */
    @Cancelable
    public static class Attacked extends PhysicsEntityEvent {
        @Getter
        private final Entity sourceEntity;
        @Getter
        private final DamageSource damageSource;

        public Attacked(PhysicsEntity<?> physicsEntity, Entity sourceEntity, DamageSource damageSource) {
            super(Dist.DEDICATED_SERVER, physicsEntity);
            this.sourceEntity = sourceEntity;
            this.damageSource = damageSource;
        }
    }

    /**
     * Fired when a physics entity has just initialized its properties and its physic
     */
    public static class Init extends PhysicsEntityEvent {
        @Getter
        private final boolean usesPhysics;

        public Init(Dist side, PhysicsEntity<?> physicsEntity, boolean usesPhysics) {
            super(side, physicsEntity);
            this.usesPhysics = usesPhysics;
        }
    }

    /**
     * Fired each tick when a physics entity is updated.
     */
    public static class Update extends PhysicsEntityEvent {
        @Getter
        private final UpdateType type;
        @Getter
        private final boolean simulatePhysics;

        public Update(Dist side, PhysicsEntity<?> physicsEntity, UpdateType type, boolean simulatePhysics) {
            super(side, physicsEntity);
            this.type = type;
            this.simulatePhysics = simulatePhysics;
        }
    }

    /**
     * Called when the renderer on an entity is created.
     *
     * @see DebugRenderer
     * @see RenderPhysicsEntity
     * @deprecated The debug should be rendered using the new {@link SceneNode}s system
     */
    @Deprecated
    public static class InitRenderer<T extends PhysicsEntity> extends GenericEvent<T> {
        @Getter
        private final RenderPhysicsEntity<?> renderer;

        public InitRenderer(Class<T> type, RenderPhysicsEntity<?> renderer) {
            super(type);
            this.renderer = renderer;
        }

        /**
         * @deprecated The debug should be rendered using the new {@link SceneNode}s system
         */
        @Deprecated
        public void addDebugRenderers(DebugRenderer<?>... renderers) {
            this.renderer.addDebugRenderers(renderers);
        }
    }

    /**
     * Fired each tick when a physics entity is updated, on server side
     */
    public static class ServerUpdate extends Update {
        public ServerUpdate(PhysicsEntity<?> physicsEntity, UpdateType type, boolean simulatePhysics) {
            super(Dist.DEDICATED_SERVER, physicsEntity, type, simulatePhysics);
        }
    }

    /**
     * Fired each tick when a physics entity is updated, on client side
     */
    public static class ClientUpdate extends Update {
        public ClientUpdate(PhysicsEntity<?> physicsEntity, UpdateType type, boolean simulatePhysics) {
            super(Dist.CLIENT, physicsEntity, type, simulatePhysics);
        }
    }

    /**
     * {@link Update} types
     */
    public enum UpdateType {
        POST_ENTITY_UPDATE,
        PRE_PHYSICS_UPDATE,
        POST_PHYSICS_UPDATE
    }

    /**
     * Called when the module list of a vehicle is created.
     *
     * @see IPhysicsModule
     * @see ModularPhysicsEntity
     */
    public static class CreateModules<T extends ModularPhysicsEntity> extends PhysicsEntityEvent implements IGenericEvent<T> {
        private final Class<T> type;
        @Getter
        private final List<IPhysicsModule<?>> moduleList;

        public CreateModules(Class<T> type, T entity, List<IPhysicsModule<?>> moduleList, Dist side) {
            super(side, entity);
            this.type = type;
            this.moduleList = moduleList;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T getEntity() {
            return (T) super.getEntity();
        }

        @Override
        public Type getGenericType() {
            return type;
        }
    }
}
