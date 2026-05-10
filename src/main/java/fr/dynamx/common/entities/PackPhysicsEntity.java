package fr.dynamx.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.IPhysicsPackInfo;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.parts.BasePartSeat;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.physics.entities.PackEntityPhysicsHandler;
import fr.dynamx.common.physics.joints.EntityJointsHandler;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.SubClassPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation for all pack-based entities
 *
 * @param <T> The physics handler type
 * @see IPhysicsModule
 * @see PackEntityPhysicsHandler For the physics implementation
 */
// TODO port:1.20.1 - Forge DataParameter / DataSerializers / EntityDataManager replaced with
// EntityDataAccessor / EntityDataSerializers / SynchedEntityData. BasePartSeat is forward-ported
// (see contentpack.parts package, kept on legacy until Phase 4b ports it). RenderPhysicsEntity
// and ClientDynamXUtils.getLightNear are not ported yet (Phase 7); the override of
// getLightLevelDependentMagicValue / shouldRiderSit is stubbed accordingly.
public abstract class PackPhysicsEntity<T extends PackEntityPhysicsHandler<A, ?>, A extends IPhysicsPackInfo & IPartContainer<?>> extends ModularPhysicsEntity<T> implements IPackInfoReloadListener {
    private static final EntityDataAccessor<String> INFO_NAME = SynchedEntityData.defineId(PackPhysicsEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> METADATA = SynchedEntityData.defineId(PackPhysicsEntity.class, EntityDataSerializers.INT);
    private int lastMetadata = -1;

    /**
     * -- GETTER --
     * The texture id depends on the entity's metadata <br>
     * If -1 is returned, the entity will not be rendered
     *
     * @return The texture id to use for drawing chassis
     */
    @Getter
    private byte entityTextureId = -1;

    protected EntityJointsHandler jointsHandler = new EntityJointsHandler(this);

    @Getter
    private MovableModule movableModule;

    @Getter
    @Setter
    private A packInfo;

    /**
     * Cache for collision boxes without rotation
     */
    private final List<MutableBoundingBox> rawBoxes = new ArrayList<>();

    public PackPhysicsEntity(EntityType<? extends PackPhysicsEntity<?, ?>> type, Level level) {
        super(type, level);
    }

    public PackPhysicsEntity(EntityType<? extends PackPhysicsEntity<?, ?>> type, String infoName, Level level, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(type, level, pos, spawnRotationAngle);
        setInfoName(infoName);
        setMetadata(metadata);
    }

    // TODO port:1.20.1 - Legacy (Level) constructor.
    public PackPhysicsEntity(Level level) {
        super(level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(INFO_NAME, "");
        this.entityData.define(METADATA, -1);
    }

    public abstract A createInfo(String infoName);

    @Override
    public boolean initEntityProperties() {
        packInfo = createInfo(getInfoName());
        if (packInfo != null && packInfo.getCollisionsHelper().hasPhysicsCollisions())
            return super.initEntityProperties();
        DynamXMain.log.warn("Failed to find info of {}. Should be {}.", this, getInfoName());
        return false;
    }

    @Override
    public void onPackInfosReloaded() {
        A packInfo = createInfo(getInfoName());
        if (packInfo == null) {
            DynamXMain.log.warn("Failed to find info of {} after packs reload. Should be {}. Killing the entity.", this, getInfoName());
            discard();
            return;
        }
        setPackInfo(packInfo);

        if (physicsHandler != null) {
            physicsHandler.onPackInfosReloaded();
        }

        for (IPhysicsModule<?> module : moduleList) {
            if (module instanceof IPackInfoReloadListener) {
                ((IPackInfoReloadListener) module).onPackInfosReloaded();
            }
        }
        rawBoxes.clear(); //Clear collisions cache
    }

    @Override
    protected void createModules(ModuleListBuilder modules) {
        moduleList.add(jointsHandler);
        moduleList.add(movableModule = new MovableModule(this));
        movableModule.initSubModules(modules, this);
        getPackInfo().addModules(this, modules);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tagCompound) {
        //Read info name before entity init in super method
        setInfoName(tagCompound.getString("vehicleName"));
        setMetadata(tagCompound.getInt("Metadata"));
        super.readAdditionalSaveData(tagCompound);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tagCompound) {
        tagCompound.putString("vehicleName", getInfoName());
        tagCompound.putInt("Metadata", getMetadata());
        super.addAdditionalSaveData(tagCompound);
    }

    @Override
    public void tick() {
        if (getInfoName().isEmpty()) {
            discard();
            return;
        }
        Vector3fPool.openPool(SubClassPool.TICK_ENTITY_MC);
        Profiler.get().start(Profiler.Profiles.TICK_ENTITIES);
        super.tick();
        if (level().isClientSide && getMetadata() != lastMetadata && !isRemoved()) //Metadata has been sync, so update texture
        {
            lastMetadata = getMetadata();
            entityTextureId = (byte) getMetadata();
            getModules().forEach(m -> m.onTexturesChange(entityTextureId));
        }
        Profiler.get().end(Profiler.Profiles.TICK_ENTITIES);
        Vector3fPool.closePool();
    }

    @Override
    public ItemStack getPickResult() {
        // TODO port:1.20.1 - In 1.12 this was getPickedResult(RayTraceResult), now signature is getPickResult().
        if (packInfo == null) return ItemStack.EMPTY;
        return packInfo.getPickedResult(getMetadata());
    }

    /**
     * Legacy 1.12 form, kept as overload used by other modules.
     */
    public ItemStack getPickedResult(HitResult target) {
        return getPickResult();
    }

    /**
     * Cache
     */
    @Override
    public List<MutableBoundingBox> getCollisionBoxes() {
        if (getPackInfo() == null || physicsPosition == null)
            return new ArrayList<>(0);
        Vector3f pos = Vector3fPool.get((float) getX(), (float) getY(), (float) getZ());
        if (rawBoxes.size() != getPackInfo().getCollisionsHelper().getShapes().size()) {
            rawBoxes.clear();
            for (IShapeInfo shape : getPackInfo().getCollisionsHelper().getShapes()) {
                MutableBoundingBox boundingBox = new MutableBoundingBox(shape.getBoundingBox());
                boundingBox.offset(pos);
                rawBoxes.add(boundingBox);
            }
        } else {
            for (int i = 0; i < getPackInfo().getCollisionsHelper().getShapes().size(); i++) {
                MutableBoundingBox boundingBox = rawBoxes.get(i);
                boundingBox.setTo(getPackInfo().getCollisionsHelper().getShapes().get(i).getBoundingBox());
                boundingBox.offset(pos);
            }
        }
        return rawBoxes;
    }

    /**
     * Ray-traces to get hit part when interacting with the entity
     */
    public InteractivePart<?, ?> getHitPart(Entity entity) {
        if (getPackInfo() == null) {
            return null;
        }
        Vec3 lookVec = entity.getViewVector(1.0F);
        Vec3 hitVec = entity.position().add(0, entity.getEyeHeight(), 0);
        InteractivePart<?, ?> nearest = null;
        Vector3f nearestPos = null;
        Vector3f playerPos = Vector3fPool.get((float) entity.getX(), (float) entity.getY(), (float) entity.getZ());
        MutableBoundingBox box = new MutableBoundingBox();
        for (float f = 1.0F; f < 4.0F; f += 0.1F) {
            for (InteractivePart<?, ?> part : getPackInfo().getInteractiveParts()) {
                part.getBox(box);
                box = DynamXContext.getCollisionHandler().rotateBB(Vector3fPool.get(), box, physicsRotation);
                Vector3f partPos = DynamXGeometry.rotateVectorByQuaternion(part.getPosition(), physicsRotation);
                partPos.addLocal(physicsPosition);
                box.offset(partPos);
                if ((nearestPos == null || DynamXGeometry.distanceBetween(partPos, playerPos) < DynamXGeometry.distanceBetween(nearestPos, playerPos)) && box.contains(hitVec)) {
                    nearest = part;
                    nearestPos = partPos;
                }
            }
            hitVec = hitVec.add(lookVec.x * 0.1F, lookVec.y * 0.1F, lookVec.z * 0.1F);
        }
        return nearest;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        // TODO port:1.20.1 - canFitPassenger -> canAddPassenger.
        return this.getPassengers().size() < getPackInfo().getPartsByType(BasePartSeat.class).size();
    }

    @Override
    public boolean shouldRiderSit() {
        // TODO port:1.20.1 - RenderPhysicsEntity not ported yet (Phase 7).
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double range) {
        if (getPackInfo() != null && getPackInfo().getRenderDistanceSquared() != -1) {
            return range < getPackInfo().getRenderDistanceSquared();
        }
        return super.shouldRenderAtSqrDistance(range);
    }

    @Override
    public EntityJointsHandler getJointsHandler() {
        return jointsHandler;
    }

    public int getMetadata() {
        return this.entityData.get(METADATA);
    }

    public void setMetadata(int metadata) {
        this.entityData.set(METADATA, metadata);
    }

    /**
     * Legacy hook returning the light value for render. In 1.12 this was getBrightnessForRender();
     * 1.20.1 uses Entity#getLightLevelDependentMagicValue, but the legacy callers expect an int.
     *
     * TODO port:1.20.1 - ClientDynamXUtils.getLightNear not ported (Phase 7); fall back to the
     * vanilla light packed at the entity position.
     */
    public int getBrightnessForRender() {
        return level().getMaxLocalRawBrightness(blockPosition());
    }

    @Override
    public Component getName() {
        return Component.literal("DynamXEntity:" + getInfoName() + ":" + getId());
    }

    public String getInfoName() {
        return this.entityData.get(INFO_NAME);
    }

    private void setInfoName(String name) {
        this.entityData.set(INFO_NAME, name);
    }
}
