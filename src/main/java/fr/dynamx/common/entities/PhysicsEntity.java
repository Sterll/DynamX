package fr.dynamx.common.entities;

import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.api.network.sync.SynchronizedEntityVariableRegistry;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.api.physics.entities.EntityPhysicsState;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.common.network.sync.PhysicsEntitySynchronizer;
import fr.dynamx.common.network.sync.variables.EntityPosVariable;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.common.physics.joints.EntityJointsHandler;
import fr.dynamx.common.physics.player.WalkingOnPlayerController;
import fr.dynamx.common.physics.terrain.PhysicsEntityTerrainLoader;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.PhysicsEntityException;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.SubClassPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.LogicalSide;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for all entities using bullet to simulate their physics
 *
 * @param <T> The physics handler type
 */
// TODO port:1.20.1 - DynamXContext, DynamXMain, PhysicsEntitySynchronizer, EntityPosVariable,
// MessageJoints and the whole common.network.* tree are not ported yet (Phase 5). Imports above
// are forward references shared with other already-ported classes (EntityJointsHandler etc.).
// TODO port:1.20.1 - Forge IEntityAdditionalSpawnData -> NeoForge IEntityAdditionalSpawnData
// with FriendlyByteBuf in writeSpawnData/readSpawnData (was ByteBuf in 1.12).
// Legacy ByteBuf writeSpawnData/readSpawnData kept as helpers; the NeoForge contract is
// implemented by delegating to the legacy methods via Unpooled.wrappedBuffer.
@SynchronizedEntityVariable.SynchronizedPhysicsModule(modid = DynamXConstants.ID)
public abstract class PhysicsEntity<T extends AbstractEntityPhysicsHandler<?, ?>> extends Entity implements IDynamXObject, IEntityAdditionalSpawnData {

    /**
     * Entity network
     * -- GETTER --
     *
     * @return The entity network
     */
    @Getter
    private final PhysicsEntitySynchronizer<? extends PhysicsEntity<T>> synchronizer;

    /**
     * The entity physics handler
     */
    @Nullable
    @Getter
    public T physicsHandler;

    /**
     * The entity physics position
     */
    public final Vector3f physicsPosition = new Vector3f();

    /**
     * The entity physics rotation <br>
     * <strong>If you are rendering something, use renderRotation !</strong>
     */
    public final Quaternion physicsRotation = new Quaternion();

    /**
     * Rotation for render <br>
     * <strong>If you are not rendering something, use physicsRotation !</strong>
     */
    public final Quaternion renderRotation = new Quaternion();

    /**
     * Prev render rotation
     */
    public final Quaternion prevRenderRotation = new Quaternion();

    /**
     * Entity initialization state
     */
    public EnumEntityInitState initialized = EnumEntityInitState.NOT_INITIALIZED;

    /**
     * State of the entity inside the physics engine
     */
    public EnumEntityPhysicsRegistryState isRegistered = EnumEntityPhysicsRegistryState.NOT_REGISTERED;

    /**
     * Map of players walking on the top of this entity
     *
     * @see WalkingOnPlayerController
     */
    // TODO port:1.20.1 - EntityPlayer -> Player.
    public final Map<Player, WalkingOnPlayerController> walkingOnPlayers = new HashMap<>();

    /**
     * Permits the render of large entities that you are riding
     */
    public boolean wasRendered = false;

    /**
     * Cache to avoid many heavy calculus of the entity box
     */
    private AABB entityBoxCache;

    /**
     * True if the entity uses the physics world <br>
     * I.e. it's physics handler should not be null
     */
    private final boolean usesPhysicsWorld;

    /**
     * -- GETTER --
     *
     * @return The terrain loader of this entity
     */
    @Getter
    private final PhysicsEntityTerrainLoader terrainCache = new PhysicsEntityTerrainLoader(this);

    // TODO port:1.20.1 - New 1.20.1 constructor (EntityType, Level). Required by NeoForge.
    public PhysicsEntity(EntityType<? extends PhysicsEntity<?>> type, Level level) {
        super(type, level);

        noPhysics = true;
        blocksBuilding = true;

        // Network Init
        // TODO port:1.20.1 - proxy.getNetHandlerForEntity belongs to common.network (Phase 5).
        synchronizer = DynamXMain.proxy.getNetHandlerForEntity(this);
        usesPhysicsWorld = DynamXContext.usesPhysicsWorld(level);
    }

    // TODO port:1.20.1 - Legacy (World) constructor. Kept to preserve call sites until they are
    // migrated to the (EntityType, Level) form. Unsupported until the EntityType registry lands
    // in Phase 5/8 entry-point wiring.
    public PhysicsEntity(Level level) {
        this((EntityType<? extends PhysicsEntity<?>>) (EntityType<?>) EntityType.AREA_EFFECT_CLOUD, level);
        throw new UnsupportedOperationException("Legacy PhysicsEntity(Level) constructor; migrate to (EntityType, Level) (Phase 5/8 will register concrete EntityTypes).");
    }

    public PhysicsEntity(EntityType<? extends PhysicsEntity<?>> type, Level level, Vector3f pos, float spawnRotationAngle) {
        this(type, level);
        setPos(pos.x, pos.y, pos.z);
        setYRot(spawnRotationAngle);
    }

    @Override
    protected void defineSynchedData() {
    }

    @SynchronizedEntityVariable(name = "pos")
    public final EntityPosVariable synchronizedPosition = new EntityPosVariable(this);

    public void registerSynchronizedVariables() {
        SynchronizedEntityVariableRegistry.addVarsOf(this.getSynchronizer(), this);
    }

    /**
     * Checks if the entity has been initialized and initializes it if required
     */
    protected void checkEntityInit() {
        switch (initialized) {
            case NOT_INITIALIZED:
                physicsPosition.set((float) getX(), (float) getY(), (float) getZ());
                if (physicsRotation.equals(Quaternion.IDENTITY)) {
                    physicsRotation.set(DynamXGeometry.rotationYawToQuaternion(getYRot()));
                }
                if (!initEntityProperties()) {
                    discard();
                    return;
                }
            case ONLY_ENTITY_PROPERTIES:
                initPhysicsEntity(usesPhysicsWorld);
                // Will refresh simulation holders on joint entities
                getSynchronizer().setSimulationHolder(getSynchronizer().getSimulationHolder(), getSynchronizer().getSimulationPlayerHolder());
                registerSynchronizedVariables();
                MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.Init(level().isClientSide ? net.minecraftforge.api.distmarker.Dist.CLIENT : net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER, this, usesPhysicsWorld));
                initialized = EnumEntityInitState.ALL;
                break;
        }
    }

    /**
     * Legacy ByteBuf spawn data writer. Kept for module compatibility (modules still use ByteBuf
     * via legacy IEntityAdditionalSpawnData semantics, see ModularPhysicsEntity).
     */
    public void writeSpawnData(ByteBuf buffer) {
    }

    /**
     * Legacy ByteBuf spawn data reader.
     */
    public void readSpawnData(ByteBuf additionalData) {
        // Fix: since initEntityProperties was added here, checkEntityInit wasn't called anymore on client, and so physicsRotation wasn't correct
        if (physicsRotation.equals(Quaternion.IDENTITY)) {
            physicsRotation.set(DynamXGeometry.rotationYawToQuaternion(getYRot()));
        }
        if (!initEntityProperties()) {
            discard();
            return;
        }
        initialized = EnumEntityInitState.ONLY_ENTITY_PROPERTIES;
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        writeSpawnData((ByteBuf) buffer);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        readSpawnData((ByteBuf) additionalData);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        DynamXUtils.writeQuaternionNBT(compound, physicsRotation);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        //Force init here, we have all the info needed
        QuaternionPool.openPool();
        physicsPosition.set((float) getX(), (float) getY(), (float) getZ());
        physicsRotation.set(DynamXUtils.readQuaternionNBT(compound));
        QuaternionPool.closePool();
        if (!initEntityProperties()) {
            discard();
            return;
        }
        initialized = EnumEntityInitState.ONLY_ENTITY_PROPERTIES;
    }

    /**
     * Fired by the minecraft entity update method
     */
    protected void mcThreadUpdate() {
        //Init
        checkEntityInit();

        //Prepare/request physics update
        if (usesPhysicsWorld()) {
            if (physicsHandler.getPhysicsState() == EntityPhysicsState.FROZEN) {
                physicsHandler.setPhysicsState(EntityPhysicsState.UNFREEZE);
            } else {
                physicsHandler.setPhysicsState(EntityPhysicsState.ENABLE);
            }
            if (isRegistered == EnumEntityPhysicsRegistryState.NOT_REGISTERED) {
                DynamXContext.getPhysicsWorld(level()).addBulletEntity(this);
            }
        }

        //Tick physics if we don't use a physics world
        if (!usesPhysicsWorld) {
            getSynchronizer().onPrePhysicsTick(Profiler.get());
            getSynchronizer().onPostPhysicsTick(Profiler.get());
        }

        //Update visual pos
        updateMinecraftPos();

        //Post the update event
        PhysicsEntityEvent.Update update;
        if (level().isClientSide) {
            update = new PhysicsEntityEvent.ClientUpdate(this,
                    PhysicsEntityEvent.UpdateType.POST_ENTITY_UPDATE,
                    isRegistered == EnumEntityPhysicsRegistryState.REGISTERED && usesPhysicsWorld);
        } else {
            update = new PhysicsEntityEvent.ServerUpdate(this, PhysicsEntityEvent.UpdateType.POST_ENTITY_UPDATE,
                    isRegistered == EnumEntityPhysicsRegistryState.REGISTERED && usesPhysicsWorld);
        }
        MinecraftForge.EVENT_BUS.post(update);
    }

    /**
     * Called in minecraft thread to update vanilla position and rotation fields, also used for render and updating "prev" fields
     */
    protected void updateMinecraftPos() {
        // xo/yo/zo are 1.20's prevPos
        xo = getX();
        yo = getY();
        zo = getZ();

        double prevX = getX();
        double prevY = getY();
        double prevZ = getZ();

        setPos(physicsPosition.x, physicsPosition.y, physicsPosition.z);

        double mX = (getX() - prevX);
        double mY = (getY() - prevY);
        double mZ = (getZ() - prevZ);

        setDeltaMovement(mX, mY, mZ);

        onMove(mX, mY, mZ);

        // Push the physics-derived AABB to the vanilla bounding box so entity raycasts (mounting,
        // interaction) hit the actual vehicle volume rather than the tiny default EntityDimensions.
        AABB dynamxBox = getDynamxBoundingBox();
        if (dynamxBox != null) {
            setBoundingBox(dynamxBox);
        }

        prevRenderRotation.set(renderRotation);
        renderRotation.set(physicsRotation);

        yRotO = getYRot();
        xRotO = getXRot();

        alignRotation(renderRotation);
    }

    /**
     * Fired on entity move to move walking players
     *
     * @param x x move
     * @param y y move
     * @param z z move
     */
    public void onMove(double x, double y, double z) {
        entityBoxCache = null; //The entity box has changed, mark it as dirty
        //TODO WIP
        for (Map.Entry<Player, WalkingOnPlayerController> e : walkingOnPlayers.entrySet()) {
            Player entity = e.getKey();
            Direction f = e.getValue().face;
            {
                Vector3f vh = DynamXContext.getCollisionHandler().rotate(Vector3fPool.get((float) getDeltaMovement().x, (float) getDeltaMovement().y, (float) getDeltaMovement().z), physicsRotation);
                float projVehicMotion = Vector3fPool.get(vh.x, vh.y, vh.z).dot(Vector3fPool.get(f.getStepX(), f.getStepY(), f.getStepZ()));
                if (projVehicMotion != 0) //We push the player
                {
                    e.getValue().applyOffset();
                }
            }
        }
    }

    /**
     * Called before ticking the physics world (can be in an external thread) <br>
     * Here we give the "input" to the physics world, i.e. the controls, the forces, etc <br>
     * Wrapper for events and profiling, please override preUpdatePhysics
     *
     * @param profiler        The current profiler
     * @param simulatePhysics If physics should be simulated in this update <br> If false, the physics handler may be null
     */
    public final void prePhysicsUpdateWrapper(Profiler profiler, boolean simulatePhysics) {
        profiler.start(Profiler.Profiles.PHY2);

        simulatePhysics = simulatePhysics && isRegistered == EnumEntityPhysicsRegistryState.REGISTERED;
        preUpdatePhysics(simulatePhysics);

        MinecraftForge.EVENT_BUS.post(level().isClientSide ? new PhysicsEntityEvent.ClientUpdate(this, PhysicsEntityEvent.UpdateType.PRE_PHYSICS_UPDATE, simulatePhysics) :
                new PhysicsEntityEvent.ServerUpdate(this, PhysicsEntityEvent.UpdateType.PRE_PHYSICS_UPDATE, simulatePhysics));
        profiler.end(Profiler.Profiles.PHY2);
    }

    /**
     * Called before ticking the physics world (can be in an external thread) <br>
     * Here we give the "input" to the physics world, i.e. the controls, the forces, etc
     *
     * @param simulatingPhysics If physics should be simulated in this update <br> If false, the physics handler may be null
     */
    public void preUpdatePhysics(boolean simulatingPhysics) {
        if (simulatingPhysics) {
            physicsHandler.update();
        }
    }

    /**
     * Called after ticking the physics world (can be in an external thread) <br>
     * Here we get the results of the "input" : the new position, the new rotation, etc <br>
     * Wrapper for events and profiling, please override postUpdatePhysics
     *
     * @param profiler        The current profiler
     * @param simulatePhysics If physics should be simulated in this update <br> If false, the physics handler may be null
     */
    public final void postUpdatePhysicsWrapper(Profiler profiler, boolean simulatePhysics) {
        profiler.start(Profiler.Profiles.PHY2P);

        simulatePhysics = simulatePhysics && isRegistered == EnumEntityPhysicsRegistryState.REGISTERED;
        postUpdatePhysics(simulatePhysics);

        MinecraftForge.EVENT_BUS.post(level().isClientSide ? new PhysicsEntityEvent.ClientUpdate(this, PhysicsEntityEvent.UpdateType.POST_PHYSICS_UPDATE, simulatePhysics) :
                new PhysicsEntityEvent.ServerUpdate(this, PhysicsEntityEvent.UpdateType.POST_PHYSICS_UPDATE, simulatePhysics));
        profiler.end(Profiler.Profiles.PHY2P);
    }


    /**
     * Called after ticking the physics world (can be in an external thread) <br>
     * Here we get the results of the "input" : the new position, the new rotation, etc
     *
     * @param simulatingPhysics If physics should be simulated in this update <br> If false, the physics handler may be null
     */
    public void postUpdatePhysics(boolean simulatingPhysics) {
        if (simulatingPhysics)
            physicsHandler.postUpdate();
    }

    /**
     * Inits the entity, for example pack properties <br>
     * Fired on the first update of the entity
     *
     * @return False to kill the entity (failed init)
     */
    public abstract boolean initEntityProperties();

    /**
     * Inits the entity physics handler <br>
     * Fired on the first update of the entity, only if this side uses physics
     *
     * @param usePhysics True if the entity is registered in a running physics world
     */
    public abstract void initPhysicsEntity(boolean usePhysics);

    /**
     * Computes yaw and pitch from the given quaternion
     */
    private void alignRotation(Quaternion localQuat) {
        Vector3f rotatedForwardDirection = Vector3fPool.get();
        rotatedForwardDirection = localQuat.mult(DynamXGeometry.FORWARD_DIRECTION, rotatedForwardDirection);

        setXRot(DynamXGeometry.getPitchFromRotationVector(rotatedForwardDirection) % 360);

        float yaw = DynamXGeometry.getYawFromRotationVector(rotatedForwardDirection) % 360;
        setYRot(yaw);
        if (yaw - yRotO > 270)
            yRotO += 360;
        else if (yRotO - yaw > 270)
            yRotO -= 360;
    }

    /**
     * Forces activation, called when the collisions of the chunk of the entity changes
     */
    public void forcePhysicsActivation() {
        if (physicsHandler != null) {
            physicsHandler.activate();
        }
    }

    /**
     * @return the number of ticks between each sync of this entity from server to client, if on dedicated server <br>
     * This SHOULD return the same value on client and server sides
     */
    public abstract int getSyncTickRate();

    public boolean usesPhysicsWorld() {
        return usesPhysicsWorld && physicsHandler != null;
    }

    //Vanilla functions

    /**
     * Minecraft's entity update
     */
    @Override
    public void tick() {
        Vector3fPool.openPool(SubClassPool.TICK_ENTITY_MC);
        double d1 = xo;
        double d2 = yo;
        double d3 = zo;
        super.tick();
        xo = d1;
        yo = d2;
        zo = d3;
        try {
            mcThreadUpdate();
        } catch (Exception ex) {
            throw new PhysicsEntityException(this, "mcThreadUpdate", ex);
        }
        Vector3fPool.closePool();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    // TODO port:1.20.1 - lerpTo signature in 1.20.1 is 5 args (no posRotationIncrements/teleport).
    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        // 1.20.1 has lerpTo(double, double, double, float, float, int, boolean) - keep override sig.
    } //Avoid vanilla sync

    @Override
    public boolean shouldRenderAtSqrDistance(double range) {
        double d = getBoundingBox().getSize() * 4.0D * 64.0D;
        return range < d * d;
    }

    /**
     * Computes the DynamX bounding box. In 1.20.1 {@code Entity.getBoundingBox()} is final, so
     * this is no longer an override: the box is recomputed lazily via {@link #refreshDynamxBoundingBox()}
     * and pushed to the vanilla cache via {@code setBoundingBox(AABB)} in {@code makeBoundingBox}.
     *
     * TODO port:1.20.1 - Original code overrode getBoundingBox(); migrate callers (currently kept
     * compatible by exposing this method under the same name).
     */
    public AABB getDynamxBoundingBox() {
        if (entityBoxCache != null) {
            return entityBoxCache;
        }
        if (physicsPosition.length() == 0) {
            physicsPosition.set((float) getX(), (float) getY(), (float) getZ());
        }
        Vector3fPool.openPool();
        // Prefer the pack-derived collision boxes when available: Bullet's runtime AABB includes
        // broadphase margin and wheel sweeps, making the box much larger than the visible vehicle.
        // Always copy the source boxes - getCollisionBoxes() returns the shared rawBoxes cache that
        // rotateBB would mutate in-place.
        List<MutableBoundingBox> packBoxes = getCollisionBoxes();
        if (!packBoxes.isEmpty()) {
            MutableBoundingBox container = new MutableBoundingBox(packBoxes.get(0));
            for (int i = 1; i < packBoxes.size(); i++) {
                container.growTo(packBoxes.get(i));
            }
            container = DynamXContext.getCollisionHandler().rotateBB(physicsPosition, container, physicsRotation);
            container.grow(0.5, 0.0, 0.5);
            entityBoxCache = container.toBB();
        } else if (physicsHandler != null) {
            Vector3f min = Vector3fPool.get();
            Vector3f max = Vector3fPool.get();
            BoundingBox boundingBox = physicsHandler.getBoundingBox();
            if (boundingBox != null) {
                boundingBox.getMin(min);
                boundingBox.getMax(max);
                entityBoxCache = new AABB(min.x, min.y, min.z, max.x, max.y, max.z);
            } else {
                entityBoxCache = super.getBoundingBox();
            }
        } else {
            List<MutableBoundingBox> boxes = getCollisionBoxes(); //Get PartShape boxes
            if (boxes.isEmpty()) { //If there is no boxes, create a default one
                Vector3f min = Vector3fPool.get((float) getX(), (float) getY(), (float) getZ()).subtractLocal(2, 1, 2);
                Vector3f max = Vector3fPool.get((float) getX(), (float) getY(), (float) getZ()).addLocal(2, 2, 2);
                entityBoxCache = new AABB(min.x, min.y, min.z, max.x, max.y, max.z);
            } else {
                MutableBoundingBox container;
                if (boxes.size() == 1) { //If there is one, no more calculus to do !
                    container = boxes.get(0);
                } else {
                    container = new MutableBoundingBox(boxes.get(0));
                    for (int i = 1; i < boxes.size(); i++) { //Else create a bigger box containing all the boxes
                        container.growTo(boxes.get(i));
                    }
                }
                //The container box corresponding to an unrotated entity, so rotate it !
                container = DynamXContext.getCollisionHandler().rotateBB(physicsPosition, container, physicsRotation);
                container.grow(0.5, 0.0, 0.5); //Grow it to avoid little glitches on the corners of the car
                entityBoxCache = container.toBB();
            }
        }
        Vector3fPool.closePool();
        return entityBoxCache;
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(level());
        if (usesPhysicsWorld && physicsWorld != null) //may be called before physicsWorld is loaded
        {
            physicsWorld.removeBulletEntity(this);
            terrainCache.onRemoved(physicsWorld.getTerrainManager());
        }
        if (physicsHandler != null) {
            physicsHandler.removeFromWorld();
        }
    }

    @Override
    public Component getName() {
        return Component.literal("DynamXEntity." + getId());
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        // TODO port:1.20.1 - EVENT_BUS.post returns boolean (cancelled flag); previously chained .isCanceled().
        PhysicsEntityEvent.Attacked attackedEvent = new PhysicsEntityEvent.Attacked(this, damageSource.getEntity(), damageSource);
        if (MinecraftForge.EVENT_BUS.post(attackedEvent)) {
            return false;
        }
        // TODO port:1.20.1 - DamageSource#isExplosion replaced by tag-based check.
        if (damageSource.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }
        if (!this.level().isClientSide && !this.isRemoved()
                && damageSource.getDirectEntity() instanceof Player
                && damageSource.getEntity() != null
                && damageSource.getEntity().getVehicle() != this) {
            Player player = (Player) damageSource.getDirectEntity();
            if (player.getAbilities().instabuild
                    || player.getMainHandItem().getItem().equals(DynamXItemRegistry.getItemWrench())) {
                discard();
                return true;
            }
        }
        return false;
    }

    /**
     * @return The entity joint handler, null by default
     */
    @Nullable
    public EntityJointsHandler getJointsHandler() {
        return null;
    }

    /**
     * @return The module of the specified type
     */
    public abstract <D extends IPhysicsModule<?>> D getModuleByType(Class<D> moduleClass);

    /**
     * @return True if this entity has this module
     */
    public boolean hasModuleOfType(Class<? extends IPhysicsModule<?>> moduleClass) {
        return getModuleByType(moduleClass) != null;
    }

    /**
     * Method called when the entity's rigidbody enter in collision with something else
     */
    public void onCollisionEnter(PhysicsCollisionEvent collisionEvent, BulletShapeType<?> entityA, BulletShapeType<?> entityB) {
    }

    /**
     * @return True if the player should have the motion of this entity when walking on the top of any collision box
     * @see WalkingOnPlayerController
     */
    public boolean canPlayerStandOnTop() {
        return false;
    }

    @Override
    public Quaternion getCollidableRotation() {
        return physicsRotation;
    }

    @Override
    public Vector3f getCollisionOffset() {
        return Vector3fPool.get();
    }

    @Override
    public void moveTo(double x, double y, double z, float yaw, float pitch) {
        QuaternionPool.openPool();
        physicsRotation.set(DynamXGeometry.rotationYawToQuaternion(yaw));
        QuaternionPool.closePool();
        super.moveTo(x, y, z, yaw, pitch);
    }

    /**
     * @return The current LogicalSide of the entity's world.
     */
    public LogicalSide getLogicalSide() {
        return level().isClientSide ? LogicalSide.CLIENT : LogicalSide.SERVER;
    }

    public enum EnumEntityInitState {
        NOT_INITIALIZED, ONLY_ENTITY_PROPERTIES, ALL
    }

    public enum EnumEntityPhysicsRegistryState {
        NOT_REGISTERED, REGISTERING, REGISTERED
    }
}
