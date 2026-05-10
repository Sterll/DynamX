package fr.dynamx.api.events;

import lombok.Getter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Physics entity events.
 *
 * TODO port:1.20.1 - PhysicsEntity / ModularPhysicsEntity / IPhysicsModule / DynamXItemSpawner /
 *   RenderPhysicsEntity / DebugRenderer / SceneNode all live in not-yet-ported packages
 *   (Phases 6 / 7). Their references are typed as Object to keep the API compilable; tighten
 *   when those packages are ported.
 * TODO port:1.20.1 - net.minecraftforge.fml.common.eventhandler.GenericEvent / IGenericEvent
 *   were removed in NeoForge 1.20.1; the InitRenderer / CreateModules classes that used them
 *   are kept as plain events here without the generic-type filtering. Bus listeners that relied
 *   on filtering will need to be updated.
 */
public class PhysicsEntityEvent extends Event {
    @Getter
    private final Dist side;
    private final Object physicsEntity;

    public PhysicsEntityEvent(Dist side, Object physicsEntity) {
        this.physicsEntity = physicsEntity;
        this.side = side;
    }

    public Object getEntity() {
        return physicsEntity;
    }

    /**
     * Fired when an entity is being spawned
     */
    public static class Spawn extends PhysicsEntityEvent implements ICancellableEvent {

        @Getter
        private final Object physicsEntity;
        @Getter
        private final Level world;
        @Getter
        @Nullable
        private final Player player;
        @Getter
        private final Object itemSpawner; // TODO port:1.20.1 - DynamXItemSpawner<?>
        @Getter
        private final Vec3 pos;

        public Spawn(Level world, Object physicsEntity, Player player, Object item, Vec3 pos) {
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
    public static class Attacked extends PhysicsEntityEvent implements ICancellableEvent {
        @Getter
        private final Entity sourceEntity;
        @Getter
        private final DamageSource damageSource;

        public Attacked(Object physicsEntity, Entity sourceEntity, DamageSource damageSource) {
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

        public Init(Dist side, Object physicsEntity, boolean usesPhysics) {
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

        public Update(Dist side, Object physicsEntity, UpdateType type, boolean simulatePhysics) {
            super(side, physicsEntity);
            this.type = type;
            this.simulatePhysics = simulatePhysics;
        }
    }

    /**
     * Called when the renderer on an entity is created.
     *
     * TODO port:1.20.1 - GenericEvent removed in NeoForge 1.20.1. Filtering by generic type is
     *   no longer supported; addons must filter manually.
     *
     * @deprecated The debug should be rendered using the new SceneNodes system
     */
    @Deprecated
    public static class InitRenderer extends Event {
        /**
         * The renderer for this type of entity. TODO port:1.20.1 - RenderPhysicsEntity is in Phase 7.
         */
        @Getter
        private final Object renderer;
        @Getter
        private final Class<?> type;

        public InitRenderer(Class<?> type, Object renderer) {
            this.type = type;
            this.renderer = renderer;
        }

        /**
         * Adds the debug renders to the list of the entity renderer.
         *
         * TODO port:1.20.1 - DebugRenderer is in fr.dynamx.utils.debug.renderer (not yet ported).
         *
         * @deprecated The debug should be rendered using the new SceneNodes system
         */
        @Deprecated
        public void addDebugRenderers(Object... renderers) {
            // TODO port:1.20.1 - this.renderer.addDebugRenderers(renderers);
        }
    }

    /**
     * Fired each tick when a physics entity is updated, on server side
     */
    public static class ServerUpdate extends Update {
        public ServerUpdate(Object physicsEntity, UpdateType type, boolean simulatePhysics) {
            super(Dist.DEDICATED_SERVER, physicsEntity, type, simulatePhysics);
        }
    }

    /**
     * Fired each tick when a physics entity is updated, on client side
     */
    public static class ClientUpdate extends Update {
        public ClientUpdate(Object physicsEntity, UpdateType type, boolean simulatePhysics) {
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
     * TODO port:1.20.1 - IGenericEvent removed in NeoForge 1.20.1; the &lt;T&gt; generic kept as
     *   plain type parameter, but no automatic filtering by type happens anymore.
     *   IPhysicsModule and ModularPhysicsEntity are in Phase 6, typed as Object until then.
     */
    public static class CreateModules<T> extends PhysicsEntityEvent {
        private final Class<T> type;
        @Getter
        private final List<Object> moduleList;

        public CreateModules(Class<T> type, T entity, List<Object> moduleList, Dist side) {
            super(side, entity);
            this.type = type;
            this.moduleList = moduleList;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T getEntity() {
            return (T) super.getEntity();
        }

        public Class<T> getGenericType() {
            return type;
        }
    }
}
