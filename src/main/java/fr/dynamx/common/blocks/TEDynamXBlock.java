package fr.dynamx.common.blocks;

import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.blocks.IBlockEntityModule;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.common.contentpack.parts.PartBlockSeat;
import fr.dynamx.common.contentpack.type.ObjectCollisionsHelper;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.entities.IDynamXObject;
import fr.dynamx.common.entities.SeatEntity;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 1.20.1 NeoForge port of DynamX's tile entity (block entity).
 *
 * <p>TODO port:1.20.1 - {@code TileEntity} -> {@link BlockEntity}; constructor takes a
 * {@link BlockEntityType} + {@link BlockPos} + {@link BlockState}. The 1.12 implicit no-arg ctor
 * registered via {@code GameRegistry.registerTileEntity} is gone; a {@code DeferredRegister<BlockEntityType<?>>}
 * must register a static {@code BlockEntityType<TEDynamXBlock>} (Phase 4b/registry wiring).
 *
 * <p>{@code ITickable.update()} -> {@code BlockEntityTicker} returned from the parent {@code EntityBlock};
 * the legacy {@link #update()} method is preserved as a normal method called from the ticker.
 *
 * <p>Chunk-collision storage now lives in the {@code DynamXChunkData} attachment (see
 * {@link fr.dynamx.common.capability.DynamXChunkDataProvider}); the legacy capability lookup is stubbed.
 */
public class TEDynamXBlock extends BlockEntity implements IDynamXObject, IPackInfoReloadListener {

    /**
     * TODO port:1.20.1 - registered via DeferredRegister<BlockEntityType<?>> in a follow-up patch.
     */
    public static BlockEntityType<TEDynamXBlock> TYPE;

    @Getter
    private BlockObject<?> packInfo;
    @Getter
    private int rotation;
    @Getter
    @Setter
    private Vector3f relativeTranslation = new Vector3f();
    @Getter
    @Setter
    private Vector3f relativeRotation = new Vector3f();
    @Getter
    @Setter
    private Vector3f relativeScale = new Vector3f(1, 1, 1);

    private boolean hasSeats;
    @Getter
    private List<SeatEntity> seatEntities;

    /**
     * The cache of the block collisions, with position offset but no rotation.
     */
    protected final List<MutableBoundingBox> unrotatedCollisionsCache = new ArrayList<>();

    /**
     * The hitbox for (mouse) interaction with the block.
     */
    protected AABB boundingBoxCache;

    private EnumBlockEntityInitState initialized = EnumBlockEntityInitState.NOT_INITIALIZED;
    protected final List<IBlockEntityModule> moduleList = new ArrayList<>();
    protected final List<IBlockEntityModule.IBlockEntityUpdateListener> updateEntityListeners = new ArrayList<>();

    // TODO port:1.20.1 - DxAnimator depends on client renderer (Phase 7); reference type relaxed to Object.
    @Getter
    private final Object animator = null;

    public TEDynamXBlock(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    /**
     * Convenience ctor preserved for 1:1 legacy call sites. Note that {@code BlockEntityType}
     * resolution depends on registration order; this should usually be created by the type itself.
     */
    public TEDynamXBlock(BlockObject<?> packInfo) {
        super(TYPE, BlockPos.ZERO, null);
        setPackInfo(packInfo);
        this.hasSeats = packInfo != null && !packInfo.getPartsByType(PartBlockSeat.class).isEmpty();
    }

    public void setPackInfo(BlockObject<?> packInfo) {
        this.packInfo = packInfo;
        if (level != null) {
            // TODO port:1.20.1 - markBlockRangeForRenderUpdate -> Level.sendBlockUpdated
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
        if (packInfo == null) return;
        this.hasSeats = !packInfo.getPartsByType(PartBlockSeat.class).isEmpty();
        if (!hasSeats && seatEntities != null) {
            seatEntities.forEach(Entity::discard);
            seatEntities = null;
        }
    }

    /**
     * Computes listeners of update methods.
     */
    protected void getListenerModules() {
        updateEntityListeners.clear();
        moduleList.forEach(m -> {
            if (m instanceof IBlockEntityModule.IBlockEntityUpdateListener listener) {
                boolean isClient = level != null && level.isClientSide;
                if (listener.listenBlockEntityUpdates(isClient
                        ? net.neoforged.fml.LogicalSide.CLIENT
                        : net.neoforged.fml.LogicalSide.SERVER))
                    updateEntityListeners.add(listener);
            }
        });
        initialized = EnumBlockEntityInitState.ALL;
    }

    @SuppressWarnings("unchecked")
    public <Y extends IPhysicsModule<?>> Y getModuleByType(Class<Y> moduleClass) {
        return (Y) moduleList.stream().filter(m -> moduleClass.isAssignableFrom(m.getClass())).findFirst().orElse(null);
    }

    public boolean hasModuleOfType(Class<? extends IPhysicsModule<?>> moduleClass) {
        return moduleList.stream().anyMatch(m -> moduleClass.isAssignableFrom(m.getClass()));
    }

    public void initBlockEntityModules() {
        if (packInfo == null) return;
        // TODO port:1.20.1 - ModuleListBuilder + addModules wiring (Phase 3b/contentpack modules).
        moduleList.sort(Comparator.comparingInt(m -> -m.getInitPriority()));
        moduleList.forEach(IBlockEntityModule::initBlockEntityProperties);
        if (level != null) {
            getListenerModules();
        } else {
            initialized = EnumBlockEntityInitState.MODULES_CREATED;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        if (packInfo != null)
            compound.putString("BlockInfo", packInfo.getFullName());
        compound.putInt("Rotation", rotation);
        compound.putFloat("TranslationX", relativeTranslation.x);
        compound.putFloat("TranslationY", relativeTranslation.y);
        compound.putFloat("TranslationZ", relativeTranslation.z);
        compound.putFloat("ScaleX", relativeScale.x);
        compound.putFloat("ScaleY", relativeScale.y);
        compound.putFloat("ScaleZ", relativeScale.z);
        compound.putFloat("RotationX", relativeRotation.x);
        compound.putFloat("RotationY", relativeRotation.y);
        compound.putFloat("RotationZ", relativeRotation.z);
        // TODO port:1.20.1 - moduleList.forEach(m -> m.writeToNBT(compound)) when modules are ported.
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        if (!compound.contains("BlockInfo")) {
            return;
        }
        // TODO port:1.20.1 - resolve BlockObject via DynamXObjectLoaders.BLOCKS.findInfo(...) (contentpack porting).
        rotation = compound.getInt("Rotation");
        relativeTranslation = new Vector3f(
                compound.getFloat("TranslationX"),
                compound.getFloat("TranslationY"),
                compound.getFloat("TranslationZ"));
        relativeScale = new Vector3f(
                compound.getFloat("ScaleX"),
                compound.getFloat("ScaleY"),
                compound.getFloat("ScaleZ"));
        relativeRotation = new Vector3f(
                compound.getFloat("RotationX"),
                compound.getFloat("RotationY"),
                compound.getFloat("RotationZ"));
        initBlockEntityModules();
        markCollisionsDirty(false);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
        markCollisionsDirty(true);
    }

    /**
     * Opens the (client-side) customization GUI.
     * <p>TODO port:1.20.1 - ACsGuiApi-based custom screen; depends on Phase 7/8.
     */
    public void openConfigGui() {
        // TODO port:1.20.1 - reopen via ACsGuiApi.asyncLoadThenShowGui once client GUIs are ported.
    }

    /**
     * @return The block collision box used for raytracing / interaction.
     */
    public AABB computeBoundingBox() {
        if (boundingBoxCache == null) {
            QuaternionPool.openPool();
            Vector3fPool.openPool();
            List<IShapeInfo> boxes = getUnrotatedCollisionBoxes();
            if (boxes.isEmpty()) {
                boundingBoxCache = new AABB(0, 0, 0, 1, 1, 1);
            } else {
                MutableBoundingBox container;
                if (boxes.size() == 1)
                    container = new MutableBoundingBox(boxes.get(0).getBoundingBox());
                else {
                    container = new MutableBoundingBox(boxes.get(0).getBoundingBox());
                    for (int i = 1; i < boxes.size(); i++) {
                        container.growTo(boxes.get(i).getBoundingBox());
                    }
                }
                Quaternion physicsRotation = getCollidableRotation();
                container.scale(getRelativeScale().x != 0 ? getRelativeScale().x : 1,
                        getRelativeScale().y != 0 ? getRelativeScale().y : 1,
                        getRelativeScale().z != 0 ? getRelativeScale().z : 1);
                container.grow(0.1, 0.0, 0.1);
                // TODO port:1.20.1 - DynamXContext.getCollisionHandler().rotateBB(...) once context is ported.
                container.offset(getRelativeTranslation().x, getRelativeTranslation().y, getRelativeTranslation().z);
                boundingBoxCache = container.toBB();
            }
            Vector3fPool.closePool();
            QuaternionPool.closePool();
        }
        return boundingBoxCache;
    }

    @Override
    public List<MutableBoundingBox> getCollisionBoxes() {
        if (packInfo != null && unrotatedCollisionsCache.size() != getUnrotatedCollisionBoxes().size()) {
            synchronized (unrotatedCollisionsCache) {
                for (IShapeInfo shape : getUnrotatedCollisionBoxes()) {
                    MutableBoundingBox b = new MutableBoundingBox(shape.getBoundingBox());
                    b.scale(relativeScale.x != 0 ? relativeScale.x : 1, relativeScale.y != 0 ? relativeScale.y : 1, relativeScale.z != 0 ? relativeScale.z : 1);
                    b.offset(getBlockPos().getX() - 0.5, getBlockPos().getY(), getBlockPos().getZ() - 0.5);
                    unrotatedCollisionsCache.add(b);
                }
            }
        }
        return unrotatedCollisionsCache;
    }

    public List<IShapeInfo> getUnrotatedCollisionBoxes() {
        if (packInfo == null) return Collections.emptyList();
        return packInfo.getCollisionsHelper().getShapes();
    }

    public CompoundCollisionShape getPhysicsCollision() {
        if (packInfo == null) {
            throw new IllegalStateException("BlockObjectInfo is null for te " + this + " at " + getBlockPos());
        }
        if (!packInfo.getCollisionsHelper().hasPhysicsCollisions()) {
            return ObjectCollisionsHelper.getEmptyCollisionShape();
        }
        return packInfo.getCollisionsHelper().getPhysicsCollisionShape();
    }

    public void markCollisionsDirty(boolean updatePhysicsTerrain) {
        if (level != null) {
            removeChunkCollisions();
        }
        boundingBoxCache = null;
        unrotatedCollisionsCache.clear();
        // TODO port:1.20.1 - physics-terrain update path requires DynamXContext + IPhysicsWorld (Phase 5/6).
        if (level != null) {
            addChunkCollisions();
        }
    }

    @Override
    public Quaternion getCollidableRotation() {
        return packInfo == null
                ? QuaternionPool.get(0, 0, 0, 1)
                : DynamXGeometry.eulerToQuaternion(
                        (packInfo.getRotation().z - relativeRotation.z),
                        ((packInfo.getRotation().y - relativeRotation.y + getRotation() * 22.5f) % 360),
                        (packInfo.getRotation().x + relativeRotation.x));
    }

    @Override
    public Vector3f getCollisionOffset() {
        return Vector3fPool.get(0.5f, 0, 0.5f).addLocal(relativeTranslation);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        addChunkCollisions();
    }

    /**
     * Adds the block collisions to the chunk data {@link fr.dynamx.common.capability.DynamXChunkData}.
     * Used for large blocks that may span multiple chunks.
     */
    protected void addChunkCollisions() {
        // TODO port:1.20.1 - re-implement via DynamXChunkDataProvider.get(chunk) once chunk access is wired.
    }

    /**
     * Removes the block collisions from the chunk data.
     */
    protected void removeChunkCollisions() {
        // TODO port:1.20.1 - re-implement via DynamXChunkDataProvider.get(chunk).
    }

    @Override
    public void onPackInfosReloaded() {
        // TODO port:1.20.1 - DynamXObjectLoaders.BLOCKS.findInfo lookup when contentpack is ported.
        for (IBlockEntityModule module : moduleList) {
            if (module instanceof IPackInfoReloadListener listener) {
                listener.onPackInfosReloaded();
            }
        }
    }

    /**
     * Tick method — preserved from {@code ITickable.update()}. In 1.20.1 must be wired through a
     * {@code BlockEntityTicker} returned from the {@code EntityBlock}.
     */
    public void update() {
        if (level == null) return;
        if (hasSeats && (seatEntities == null) && !level.isClientSide) {
            // TODO port:1.20.1 - re-spawn seats; depends on SeatEntity ctor + Level.addFreshEntity flow.
        }
        if (initialized != EnumBlockEntityInitState.ALL) {
            if (initialized == EnumBlockEntityInitState.MODULES_CREATED) {
                getListenerModules();
            } else {
                initBlockEntityModules();
            }
        }
        if (!updateEntityListeners.isEmpty()) {
            updateEntityListeners.forEach(IBlockEntityModule.IBlockEntityUpdateListener::updateBlockEntity);
        }
    }

    /**
     * Ray-traces to get hit part when interacting with the entity.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public InteractivePart<?, ?> getHitPart(Entity entity) {
        if (getPackInfo() == null) return null;
        Vec3 lookVec = entity.getViewVector(1.0F);
        Vec3 hitVec = entity.position().add(0, entity.getEyeHeight(), 0);
        InteractivePart<?, ?> nearest = null;
        Vector3f nearestPos = null;
        Vector3f playerPos = Vector3fPool.get((float) entity.getX(), (float) entity.getY(), (float) entity.getZ());
        MutableBoundingBox box = new MutableBoundingBox();
        for (float f = 1.0F; f < 4.0F; f += 0.1F) {
            for (InteractivePart<?, ?> part : getPackInfo().getInteractiveParts()) {
                part.getBox(box);
                // TODO port:1.20.1 - DynamXContext.getCollisionHandler().rotateBB(...) once available.
                Vector3f partPos = DynamXGeometry.rotateVectorByQuaternion(part.getPosition(), getCollidableRotation());
                partPos.addLocal(getBlockPos().getX() + getPackInfo().getTranslation().x + getCollisionOffset().x,
                        getBlockPos().getY() + getPackInfo().getTranslation().y + getCollisionOffset().y,
                        getBlockPos().getZ() + getPackInfo().getTranslation().z + getCollisionOffset().z);
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

    public List<IBlockEntityModule> getModules() {
        return moduleList;
    }

    public enum EnumBlockEntityInitState {
        NOT_INITIALIZED, MODULES_CREATED, ALL
    }
}
