package fr.dynamx.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.EnumPlayerStandOnTop;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Base implementation for all vehicles <br>
 * It's fully modular and allows to create very different vehicles
 *
 * @param <T> The physics handler type
 * @see IPhysicsModule
 * @see BaseVehiclePhysicsHandler For the physics implementation
 */
// TODO port:1.20.1 - ModularVehicleInfo is not yet ported (Phase 4b). Typed via the forward
// reference of the contentpack.type.vehicle package.
public abstract class BaseVehicleEntity<T extends BaseVehiclePhysicsHandler<?>> extends PackPhysicsEntity<T, ModularVehicleInfo> {
    public BaseVehicleEntity(EntityType<? extends BaseVehicleEntity<?>> type, Level level) {
        super(type, level);
    }

    public BaseVehicleEntity(EntityType<? extends BaseVehicleEntity<?>> type, String name, Level world, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(type, name, world, pos, spawnRotationAngle, metadata);
    }

    // TODO port:1.20.1 - Legacy (Level) constructor.
    public BaseVehicleEntity(Level level) {
        super(level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    protected final void fireCreateModulesEvent(Dist side) {
        //Don't simplify the generic type, for fml (kept for backwards-compatible call shape)
        NeoForge.EVENT_BUS.post(new PhysicsEntityEvent.CreateModules<>(BaseVehicleEntity.class, this, (java.util.List) moduleList, side));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tagCompound) {
        super.readAdditionalSaveData(tagCompound);

        setMetadata(tagCompound.getInt("Metadata"));
        NeoForge.EVENT_BUS.post(new VehicleEntityEvent.LoadFromNBT(tagCompound, this));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tagCompound) {
        super.addAdditionalSaveData(tagCompound);

        tagCompound.putInt("Metadata", getMetadata());
        NeoForge.EVENT_BUS.post(new VehicleEntityEvent.SaveToNBT(tagCompound, this));
    }

    @Override
    public Component getName() {
        return Component.literal("DynamXVehicle:" + getInfoName() + ":" + getId());
    }

    @Override
    public int getSyncTickRate() { //TODO aym EXPLORE THIS
        return DynamXConfig.mountedVehiclesSyncTickRate;
    }

    @Override
    public boolean canPlayerStandOnTop() {
        EnumPlayerStandOnTop playerStandOnTop = this.getPackInfo().getPlayerStandOnTop();
        switch (playerStandOnTop) {
            case NEVER:
                return false;
            case PROGRESSIVE:
                return DynamXUtils.getSpeed(this) <= 30;
            default:
                return true;
        }
    }
}
