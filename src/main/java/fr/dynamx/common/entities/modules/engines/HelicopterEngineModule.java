package fr.dynamx.common.entities.modules.engines;

import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.client.handlers.hud.HelicopterController;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.type.vehicle.BaseEngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.HelicopterPhysicsInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.network.sync.variables.EntityFloatArrayVariable;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.utils.DynamXConstants;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.LogicalSide;

/**
 * Helicopter engine: progressive startup, throttle, roll controls.
 */
// TODO port:1.20.1 - HelicopterController/HelicopterPhysicsInfo forward (Phase 4b/7). MathHelper -> Mth;
// EntityPlayer -> Player; capabilities.isCreativeMode -> getAbilities().instabuild.
@SynchronizedEntityVariable.SynchronizedPhysicsModule(modid = DynamXConstants.ID)
public class HelicopterEngineModule extends BasicEngineModule {
    @Getter
    @SynchronizedEntityVariable(name = "roll_controls")
    private final EntityFloatArrayVariable rollControls = new EntityFloatArrayVariable(SynchronizationRules.CONTROLS_TO_SPECTATORS, new float[2]);
    @SynchronizedEntityVariable(name = "power")
    private final EntityVariable<Float> power = new EntityVariable<Float>(SynchronizationRules.CONTROLS_TO_SPECTATORS, 0f);
    private int engineStartupTime;
    private int startupTimer;
    @Getter
    private BaseEngineInfo engineInfo;

    public HelicopterEngineModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity) {
        super(entity);
        onPackInfosReloaded();
    }

    @Override
    public void onPackInfosReloaded() {
        super.onPackInfosReloaded();
        engineInfo = entity.getPackInfo().getSubPropertyByType(BaseEngineInfo.class);
        engineStartupTime = entity.getPackInfo().getSubPropertyByType(HelicopterPhysicsInfo.class).getEngineStartupTime();
    }

    public void setPower(float power) {
        this.power.set(Mth.clamp(power, 0, 1));
    }

    public float getPower() {
        return power.get();
    }

    @Override
    public void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (entity.getControllingPassenger() == null && passenger instanceof Player && !((Player) passenger).getAbilities().instabuild) {
            power.set(0f);
        }
    }

    @Override
    public void onEngineSwitchedOn() {
        super.onEngineSwitchedOn();
        if (DynamXMain.proxy.ownsSimulation(entity)) {
            startupTimer = engineStartupTime;
            power.set(1f / engineStartupTime);
        }
    }

    @Override
    public void onEngineSwitchedOff() {
        super.onEngineSwitchedOff();
        startupTimer = (int) (-20 * power.get());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public IVehicleController createNewController() {
        return new HelicopterController(entity, this);
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        power.set(tag.getFloat("power"));
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        tag.putFloat("power", power.get());
    }

    @Override
    public boolean listenEntityUpdates(LogicalSide side) {
        return true;
    }

    @Override
    public void updateEntity() {
        if (DynamXMain.proxy.ownsSimulation(entity)) {
            if (startupTimer > 0) {
                startupTimer--;
                power.set(1f - (float) startupTimer / engineStartupTime);
            } else if (startupTimer < 0) {
                startupTimer++;
                power.set(-(float) startupTimer / 20);
            }
        }
        if (entity.level().isClientSide) { //sounds
            super.updateEntity();
        }
    }

    @Override
    public float getSoundPitch() {
        return power.get();
    }

    /**
     * Per-module spawn data hook invoked reflectively by {@link fr.dynamx.common.entities.ModularPhysicsEntity}.
     */
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeFloat(power.get());
    }

    public void readSpawnData(ByteBuf additionalData) {
        power.set(additionalData.readFloat());
    }
}
