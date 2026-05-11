package fr.dynamx.common.entities;

import com.jme3.bullet.joints.Constraint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.entities.modules.AttachModule;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.network.sync.AttachedBodySynchronizer;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.common.network.sync.variables.EntityTransformsVariable;
import fr.dynamx.common.physics.entities.EntityPhysicsHandler;
import fr.dynamx.common.physics.entities.EnumRagdollBodyPart;
import fr.dynamx.common.physics.entities.RagdollPhysics;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.EntityJointsHandler;
import fr.dynamx.common.physics.joints.JointHandler;
import fr.dynamx.common.physics.joints.JointHandlerRegistry;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.common.physics.utils.SynchronizedRigidBodyTransform;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Player-shaped ragdoll entity.
 */
// TODO port:1.20.1 - EntityTransformsVariable is in common.network.sync.variables (Phase 5).
// SPacketEntityEquipment / WorldServer.getEntityTracker are gone in 1.20.1 - the equipment-sync
// code path has been removed because Player/Living entities now handle equipment broadcasting
// through ClientboundSetEquipmentPacket on the EntityType<? extends LivingEntity> path; this
// entity is a plain Entity, so the sync is left as TODO.
@SynchronizedEntityVariable.SynchronizedPhysicsModule(modid = DynamXConstants.ID)
public class RagdollEntity extends ModularPhysicsEntity<RagdollPhysics<?>> implements AttachedBodySynchronizer {
    private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(RagdollEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> HANDLED_PLAYER_ID = SynchedEntityData.defineId(RagdollEntity.class, EntityDataSerializers.INT);

    public static final Vector3f HEAD_BOX_SIZE = new Vector3f(0.25f, 0.25f, 0.25f);
    public static final Vector3f CHEST_BOX_SIZE = new Vector3f(0.24f, 0.375f, 0.129f);
    public static final Vector3f LIMB_BOX_SIZE = new Vector3f(0.129f, 0.375f, 0.129f);

    public static final Vector3f HEAD_ATTACH_POINT = new Vector3f(0f, -0.23f, 0);
    public static final Vector3f LIMB_ATTACH_POINT = new Vector3f(0f, 0.35f, 0);

    public static final Vector3f HEAD_BODY_ATTACH_POINT = new Vector3f(0, 0.41f, 0);
    public static final Vector3f RIGHT_ARM_ATTACH_POINT = new Vector3f(-0.369f, 0.375f, 0);
    public static final Vector3f LEFT_ARM_ATTACH_POINT = new Vector3f(0.369f, 0.375f, 0);
    public static final Vector3f RIGHT_LEG_ATTACH_POINT = new Vector3f(-0.13f, -0.42f, 0);
    public static final Vector3f LEFT_LEG_ATTACH_POINT = new Vector3f(0.13f, -0.42f, 0);

    private final List<MutableBoundingBox> rawBoxes = new ArrayList<>();
    private final HashMap<Byte, SynchronizedRigidBodyTransform> transforms = new HashMap<>();

    @SynchronizedEntityVariable(name = "parts_pos")
    private final EntityTransformsVariable synchronizedTransforms = new EntityTransformsVariable(this, this);

    private short handlingTime;
    private Player handledPlayer;

    private final EntityJointsHandler handler = new EntityJointsHandler(this);
    private final MovableModule movableModule = new MovableModule(this);

    private final NonNullList<ItemStack> inventoryArmor = NonNullList.withSize(4, ItemStack.EMPTY);
    private final NonNullList<ItemStack> armorArray = NonNullList.withSize(4, ItemStack.EMPTY);

    public RagdollEntity(EntityType<? extends RagdollEntity> type, Level level) {
        super(type, level);
        this.handlingTime = -1;
        this.handledPlayer = null;
    }

    public RagdollEntity(EntityType<? extends RagdollEntity> type, Level level, Vector3f pos, float spawnRotationAngle, String skin) {
        this(type, level, pos, spawnRotationAngle, skin, (short) -1, null);
    }

    public RagdollEntity(EntityType<? extends RagdollEntity> type, Level level, Vector3f pos, float spawnRotationAngle, String skin, short handlingTime, Player handledPlayer) {
        super(type, level, pos, spawnRotationAngle);
        this.handlingTime = handlingTime;
        this.handledPlayer = handledPlayer;
        setSkin(skin);
        if (handledPlayer == null) {
            return;
        }
        setHandledPlayer(handledPlayer.getId());
        // TODO port:1.20.1 - 1.12 used handledPlayer.inventory.armorInventory directly; in 1.20
        // Inventory.armor is private but Player#getArmorSlots() iterates the four armor slots.
        int i = 0;
        for (ItemStack s : handledPlayer.getArmorSlots()) {
            if (i >= inventoryArmor.size()) break;
            inventoryArmor.set(i, s);
            i++;
        }
    }

    // TODO port:1.20.1 - Legacy (Level) constructor.
    public RagdollEntity(Level level) {
        super(level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SKIN, "");
        entityData.define(HANDLED_PLAYER_ID, -1);
    }

    /**
     * @param skin A player name or a string resource location
     */
    public void setSkin(String skin) {
        entityData.set(SKIN, skin);
    }

    public String getSkin() {
        return entityData.get(SKIN);
    }


    public void setHandledPlayer(int id) {
        entityData.set(HANDLED_PLAYER_ID, id);
    }

    public int getHandledPlayer() {
        return entityData.get(HANDLED_PLAYER_ID);
    }

    @Override
    public int getSyncTickRate() {
        return 2;
    }

    @Override
    public boolean initEntityProperties() {
        super.initEntityProperties();
        Vector3fPool.openPool();
        QuaternionPool.openPool();
        for (EnumRagdollBodyPart part : EnumRagdollBodyPart.values()) {
            RigidBodyTransform transform = new RigidBodyTransform();
            Quaternion localQuat = QuaternionPool.get().fromAngleNormalAxis((float) Math.toRadians(-getYRot()), Vector3fPool.get(0, 1, 0));
            Vector3f pos = DynamXGeometry.rotateVectorByQuaternion(part.getChestAttachPoint(), localQuat);
            transform.setPosition(physicsPosition.add(pos));
            transform.setRotation(localQuat);

            transforms.put((byte) part.ordinal(), new SynchronizedRigidBodyTransform(transform));
        }
        Vector3fPool.closePool();
        QuaternionPool.closePool();
        return true;
    }

    @Override
    public void initPhysicsEntity(boolean usePhysics) {
        super.initPhysicsEntity(usePhysics);
        if (level().isClientSide) {
            return;
        }
        DynamXMain.proxy.scheduleTask(level(), () -> {
            for (EnumRagdollBodyPart enumBodyPart : EnumRagdollBodyPart.values()) {
                if (!enumBodyPart.equals(EnumRagdollBodyPart.CHEST)) {
                    JointHandlerRegistry.createJointWithSelf(RagdollJointsHandler.JOINT_HANDLER_NAME, this, (byte) enumBodyPart.ordinal());
                }
            }
        });
    }

    @Override
    protected RagdollPhysics<?> createPhysicsHandler() {
        return new RagdollPhysics<>(this);
    }

    private RagdollJointsHandler attachModule;

    @Override
    public void createModules(ModuleListBuilder modules) {
        attachModule = new RagdollJointsHandler(this);
        movableModule.initSubModules(modules, this);
    }

    @Override
    protected void fireCreateModulesEvent(Dist side) {
        //Don't simplify the generic type, for fml (kept for backwards-compatible call shape)
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.CreateModules<>(RagdollEntity.class, this, (java.util.List) moduleList, side));
    }

    @Override
    public List<MutableBoundingBox> getCollisionBoxes() {
        rawBoxes.clear();
        MutableBoundingBox b = new MutableBoundingBox(-.4, -1, -.4, .4, .9, .4);
        b.offset(physicsPosition);
        rawBoxes.add(b);
        return rawBoxes;
    }

    @Override
    public <A extends IPartContainer<?>> A getPackInfo() {
        return null;
    }

    @Override
    public <D extends IPhysicsModule<?>> D getModuleByType(Class<D> attachModuleClass) {
        if (attachModuleClass.hashCode() == RagdollJointsHandler.class.hashCode()) {
            return (D) attachModule;
        } else if (attachModuleClass == MovableModule.class)
            return (D) movableModule;
        return null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        handler.readFromNBT(compound);
        setSkin(compound.getString("skin"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        handler.writeToNBT(compound);
        compound.putString("skin", getSkin());
    }

    @Override
    public void onRemovedFromWorld() {
        if (handledPlayer != null) {
            // TODO port:1.20.1 - Player#eyeHeight is gone in 1.20.1 (eye height is computed from pose).
            // Player#sendPlayerAbilities -> ServerPlayer#onUpdateAbilities; we let the player resume normal eye-height
            // implicitly by removing it from the collision controller.
            if (handledPlayer.getVehicle() == null && !DynamXContext.getWalkingPlayers().containsKey(handledPlayer)
                    && DynamXContext.getPlayerToCollision().containsKey(handledPlayer) && DynamXContext.usesPhysicsWorld(level())) {
                DynamXContext.getPlayerToCollision().get(handledPlayer).ragdollEntity = null;
                DynamXContext.getPlayerToCollision().get(handledPlayer).addToWorld();
            }
        }
        handler.onRemovedFromWorld();
        super.onRemovedFromWorld();
    }

    @Override
    public EntityJointsHandler getJointsHandler() {
        return handler;
    }

    @Override
    public void tick() {
        super.tick();
        synchronizedTransforms.setChanged(true);
        handler.updateEntity();
        if (handledPlayer != null && handlingTime > 0) {
            handlingTime--;
            if (handlingTime == 0) {
                handlingTime = -1;
                discard();
            }
        }
        if (!level().isClientSide) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
                ItemStack itemstack1 = this.getItemBySlot(slot);
                ItemStack tracked = this.armorArray.get(slot.getIndex());
                if (ItemStack.isSameItemSameTags(itemstack1, tracked)) continue;
                // TODO port:1.20.1 - SPacketEntityEquipment / WorldServer#getEntityTracker were
                // removed; equipment sync for non-LivingEntity entities is not provided by vanilla
                // anymore. Left as TODO until a packet hook is wired up (Phase 5/7).
                this.armorArray.set(slot.getIndex(), itemstack1.isEmpty() ? ItemStack.EMPTY : itemstack1.copy());
            }
        }

        if (level().isClientSide) {
            if (handledPlayer == null) {
                Entity entityByID = level().getEntity(getHandledPlayer());
                if (entityByID instanceof Player && DynamXContext.getPlayerToCollision().containsKey(entityByID)) {
                    handledPlayer = (Player) entityByID;
                    DynamXContext.getPlayerToCollision().get(handledPlayer).ragdollEntity = this;
                    DynamXContext.getPlayerToCollision().get(handledPlayer).removeFromWorld(false, level());
                }
            }
        }


        if (handledPlayer != null) {
            handledPlayer.setDeltaMovement(0, 0, 0);
            handledPlayer.setPos(getX(), getY(), getZ());
        }
    }

    @Override
    public void updateMinecraftPos() {
        super.updateMinecraftPos();
        for (EnumRagdollBodyPart part : EnumRagdollBodyPart.values()) {
            transforms.get((byte) part.ordinal()).updatePos();
        }
    }

    @Override
    public void preUpdatePhysics(boolean simulatingPhysics) {
        super.preUpdatePhysics(simulatingPhysics);
        movableModule.preUpdatePhysics(simulatingPhysics);
        if (!simulatingPhysics) {
            return;
        }
        for (EnumRagdollBodyPart part : EnumRagdollBodyPart.values()) {
            PhysicsRigidBody body = physicsHandler.getBodyParts().get(part);
            transforms.get((byte) part.ordinal()).getPhysicTransform().set(body);
        }
    }

    @Override
    public Component getName() {
        return Component.literal("DynamXRagdoll." + getId());
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        handler.onSetDead();
    }

    @Override
    public Map<Byte, SynchronizedRigidBodyTransform> getTransforms() {
        return transforms;
    }

    @Override
    public void setPhysicsTransform(byte jointId, RigidBodyTransform transform) {
        physicsHandler.getBodyParts().get(EnumRagdollBodyPart.values()[jointId]).setPhysicsLocation(transform.getPosition());
        physicsHandler.getBodyParts().get(EnumRagdollBodyPart.values()[jointId]).setPhysicsRotation(transform.getRotation());
    }

    public Iterable<ItemStack> getArmorInventoryList() {
        return this.inventoryArmor;
    }

    // TODO port:1.20.1 - Entity (not LivingEntity) in 1.20 has no equipment slots; these accessors
    // are kept for legacy callers but are no longer @Overrides.
    public ItemStack getItemBySlot(EquipmentSlot slotIn) {
        if (slotIn.getType() == EquipmentSlot.Type.ARMOR) {
            return this.inventoryArmor.get(slotIn.getIndex());
        }
        return ItemStack.EMPTY;
    }

    public void setItemSlot(EquipmentSlot slotIn, ItemStack stack) {
        if (slotIn.getType() == EquipmentSlot.Type.ARMOR) {
            this.inventoryArmor.set(slotIn.getIndex(), stack);
        }
    }

    public Iterable<ItemStack> getArmorSlots() {
        return inventoryArmor;
    }

    public static class RagdollJointsHandler implements AttachModule.AttachToSelfModule, IPhysicsModule<EntityPhysicsHandler<?>> {
        public static final ResourceLocation JOINT_HANDLER_NAME = new ResourceLocation(DynamXConstants.ID, "ragdoll_parts");
        protected final RagdollEntity entity;

        static {
            JointHandlerRegistry.register(new JointHandler(JOINT_HANDLER_NAME, RagdollEntity.class, RagdollEntity.class, RagdollJointsHandler.class));
        }

        public RagdollJointsHandler(RagdollEntity entity) {
            this.entity = entity;
        }

        @Override
        public boolean canCreateJoint(PhysicsEntity<?> withEntity, byte jointId) {
            return true;
        }

        @Override
        public void onJointDestroyed(EntityJoint<?> joint) {
        }

        @Override
        public Constraint createJoint(byte jointId) {
            return RagdollPhysics.createBodyPartJoint(entity, EnumRagdollBodyPart.values()[jointId]);
        }
    }
}
