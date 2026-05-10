package fr.dynamx.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.common.blocks.TEDynamXBlock;
import fr.dynamx.common.contentpack.parts.PartBlockSeat;
import fr.dynamx.utils.EnumSeatPlayerPosition;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;

/**
 * Stand-alone seat entity attached to a DynamX block.
 */
// TODO port:1.20.1 - TEDynamXBlock & PartBlockSeat are not ported yet (Phase 4b / 8). The class
// references them forward. IEntityAdditionalSpawnData -> IEntityAdditionalSpawnData with FriendlyByteBuf.
public class SeatEntity extends Entity implements IEntityAdditionalSpawnData {
    protected TEDynamXBlock block;
    protected PartBlockSeat<?> mySeat;
    protected byte seatID;

    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public SeatEntity(EntityType<? extends SeatEntity> type, Level level, byte seatID) {
        this(type, level);
        this.seatID = seatID;
    }

    // TODO port:1.20.1 - Legacy ctor; supersededed by the EntityType form.
    public SeatEntity(Level level, byte seatID) {
        this((EntityType<? extends SeatEntity>) (EntityType<?>) EntityType.ARMOR_STAND, level, seatID);
        throw new UnsupportedOperationException("Legacy SeatEntity(Level, byte) constructor; migrate to (EntityType, Level, byte) (Phase 5/8).");
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void positionRider(Entity passenger, MoveFunction callback) {
        if (block == null || mySeat == null) {
            return;
        }
        Vector3fPool.openPool();
        Vector3f posVec = DynamXGeometry.rotateVectorByQuaternion(mySeat.getPosition(), block.getCollidableRotation());
        posVec.addLocal(block.getRelativeTranslation());
        callback.accept(passenger, getX() + posVec.x, getY() + posVec.y, getZ() + posVec.z);
        Vector3fPool.closePool();
    }

    /**
     * Rotates the passenger, limiting his field of view to avoid stiff necks
     */
    @Override
    public void onPassengerTurned(Entity passenger) {
        if (mySeat == null || !mySeat.shouldLimitFieldOfView()) {
            return;
        }
        float blockYaw = block.getPackInfo() == null ? 0 : (block.getPackInfo().getRotation().y - block.getRelativeRotation().y + block.getRotation() * 22.5f);
        passenger.setYBodyRot(blockYaw);
        float f = Mth.wrapDegrees(passenger.getYRot() - blockYaw);
        float f1 = Mth.clamp(f, mySeat.getMinYaw(), mySeat.getMaxYaw());
        passenger.yRotO += f1 - f;
        passenger.setYRot(passenger.getYRot() + f1 - f);
        passenger.setYHeadRot(passenger.getYRot());

        float f2 = Mth.wrapDegrees(passenger.getXRot());
        float f3 = Mth.clamp(f2, mySeat.getMinPitch(), mySeat.getMaxPitch());
        passenger.setXRot(f3);
        f2 = Mth.wrapDegrees(passenger.xRotO);
        f3 = Mth.clamp(f2, mySeat.getMinPitch(), mySeat.getMaxPitch());
        passenger.xRotO = f3;
    }

    @Override
    public boolean shouldRiderSit() {
        return mySeat == null || mySeat.getPlayerPosition() == EnumSeatPlayerPosition.SITTING;
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount % 20 != 0) {
            return;
        }
        BlockEntity te = level().getBlockEntity(blockPosition());
        if (te instanceof TEDynamXBlock) {
            block = (TEDynamXBlock) te;
            mySeat = (PartBlockSeat<?>) block.getPackInfo().getPartsByType(PartBlockSeat.class).stream().filter(s -> ((PartBlockSeat<?>) s).getId() == seatID).findFirst().orElse(null);
            if (mySeat != null)
                return;
        }
        discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbtTagCompound) {
        seatID = nbtTagCompound.getByte("SeatID");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbtTagCompound) {
        nbtTagCompound.putByte("SeatID", seatID);
    }

    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeByte(seatID);
    }

    public void readSpawnData(ByteBuf additionalData) {
        seatID = additionalData.readByte();
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf) {
        writeSpawnData((ByteBuf) buf);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf) {
        readSpawnData((ByteBuf) buf);
    }
}
