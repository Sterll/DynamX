package fr.dynamx.common.entities.modules;

import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IPackInfoReloadListener;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.sound.VehicleSound;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.engines.CarEngineModule;
import fr.dynamx.common.network.sync.variables.EntityFloatArrayVariable;
import fr.dynamx.common.network.sync.variables.EntityMapVariable;
import fr.dynamx.common.physics.entities.BaseWheeledVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.WheelsPhysicsHandler;
import fr.dynamx.common.physics.entities.parts.wheel.WheelPhysics;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.maths.DynamXMath;
import fr.dynamx.utils.optimization.Vector3fPool;
import jme3utilities.Validate;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Wheel module: tracks per-wheel state, friction, and visual properties.
 *
 * @see WheelsPhysicsHandler
 */
// TODO port:1.20.1 - PartWheel/PartWheelInfo/RenderPhysicsEntity/VehicleSound forward-referenced (Phase 4b/7).
// FMLCommonHandler.instance().getEffectiveSide() -> EffectiveSide.get().
@SynchronizedEntityVariable.SynchronizedPhysicsModule(modid = DynamXConstants.ID)
public class WheelsModule implements IPhysicsModule<BaseWheeledVehiclePhysicsHandler<?>>, IPhysicsModule.IPhysicsUpdateListener, IPackInfoReloadListener {
    @SynchronizedEntityVariable(name = "wheel_infos")
    protected final EntityMapVariable<Map<Byte, String>, Byte, String> synchronizedWheelInfos = new EntityMapVariable<>((variable, value) -> {
        value.forEach((wheelIndex, wheelInfoName) -> {
            PartWheelInfo wheelInfo = DynamXObjectLoaders.WHEELS.findInfo(wheelInfoName);
            if (wheelInfo == null) {
                throw new IllegalStateException("Cannot synchronize wheel " + wheelIndex + " to " + wheelInfoName + ": wheel not found in infos registry.");
            }
            setWheelInfo(wheelIndex, wheelInfo);
        });
    }, SynchronizationRules.CONTROLS_TO_SPECTATORS);

    @Getter
    protected final Map<Byte, PartWheelInfo> wheelInfos = new HashMap<>();

    @SynchronizedEntityVariable(name = "wheel_states")
    protected EntityVariable<WheelState[]> wheelsStates;

    @SynchronizedEntityVariable(name = "skid_infos")
    public EntityFloatArrayVariable skidInfos = new EntityFloatArrayVariable(SynchronizationRules.PHYSICS_TO_SPECTATORS, null);

    @Getter
    protected byte[] wheelsTextureId;

    public float[] visualProperties;
    public float[] prevVisualProperties;

    protected final BaseVehicleEntity<? extends BaseWheeledVehiclePhysicsHandler<?>> entity;
    protected WheelsPhysicsHandler wheelsPhysics;

    public WheelsModule(BaseVehicleEntity<? extends BaseWheeledVehiclePhysicsHandler<?>> entity) {
        this.entity = entity;
        wheelsStates = new EntityVariable<>((variable, value) -> {
            if (entity.getSynchronizer().getSimulationHolder().ownsControls(EffectiveSide.get())) {
                return;
            }
            if (!DynamXMain.proxy.shouldUseBulletSimulation(entity.level())) {
                return;
            }
            for (int i = 0; i < value.length; i++) {
                if (variable.get()[i] != value[i]) {
                    if (value[i] == WheelState.REMOVED) {
                        getPhysicsHandler().removeWheel((byte) i);
                    } else {
                        getPhysicsHandler().getWheel(i).setFlattened(value[i] == WheelState.ADDED_FLATTENED);
                    }
                }
            }
        }, SynchronizationRules.CONTROLS_TO_SPECTATORS);
    }

    @Override
    public void onPackInfosReloaded() {
        for (PartWheel part : entity.getPackInfo().getPartsByType(PartWheel.class)) {
            if (!synchronizedWheelInfos.get().containsKey(part.getId())) {
                // Changing number of wheels on spawned vehicles isn't supported
                continue;
            }

            String wheelInfoName = synchronizedWheelInfos.get().get(part.getId());
            PartWheelInfo wheelInfo = DynamXObjectLoaders.WHEELS.findInfo(wheelInfoName);
            if (wheelInfo == null) {
                DynamXMain.log.warn("[Pack Reload] Replacing missing wheel info {} by default wheel info {}", wheelInfoName, part.getDefaultWheelName());
                wheelInfo = part.getDefaultWheelInfo();
            }
            setWheelInfo(part.getId(), wheelInfo);
        }
    }

    public void setWheelInfo(byte partIndex, PartWheelInfo info) {
        Validate.nonNull(info, "Wheel info can't be null");

        if (wheelInfos.get(partIndex) == info) {
            return;
        }

        VehicleEntityEvent.ChangeWheel event = new VehicleEntityEvent.ChangeWheel(EffectiveSide.get(), entity, this, wheelInfos.get(partIndex), info, partIndex);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return;
        }

        // TODO port:1.20.1 - VehicleEntityEvent.ChangeWheel.getNewWheel() returns Object pending Phase 6.
        PartWheelInfo newWheel = (PartWheelInfo) event.getNewWheel();
        synchronizedWheelInfos.put(partIndex, newWheel.getFullName());
        wheelInfos.put(partIndex, newWheel);

        if (wheelsPhysics != null) {
            wheelsPhysics.getWheelByPartIndex(partIndex).setWheelInfo(newWheel);
        }

        if (entity.level().isClientSide) {
            onTexturesChange(entity.getEntityTextureId());
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onTexturesChange(byte newMetadata) {
        if (newMetadata == -1 || entity.getPackInfo() == null) {
            return;
        }
        String chassisVariant = entity.getPackInfo().getVariantName(newMetadata);
        for (byte i = 0; i < wheelsTextureId.length; i++) {
            wheelsTextureId[i] = getWheelInfo(i).getIdForVariant(chassisVariant);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public byte getWheelsTextureId(int wheelPartId) {
        return wheelsTextureId[wheelPartId];
    }

    public PartWheelInfo getWheelInfo(byte partIndex) {
        return wheelInfos.get(partIndex);
    }

    public WheelState[] getWheelsStates() {
        return wheelsStates.get();
    }

    @Override
    public void initEntityProperties() {
        // Load pack wheel infos
        for (PartWheel part : entity.getPackInfo().getPartsByType(PartWheel.class)) {
            synchronizedWheelInfos.put(part.getId(), part.getDefaultWheelName());
            wheelInfos.put(part.getId(), part.getDefaultWheelInfo());
        }

        int wheelCount = entity.getPackInfo().getPartsByType(PartWheel.class).size();

        skidInfos.set(new float[wheelCount]);
        wheelsStates.set(new WheelState[wheelCount]);

        this.wheelsTextureId = new byte[wheelCount];
        for (int i = 0; i < wheelCount; i++) {
            wheelsStates.get()[i] = WheelState.ADDED;
            wheelsTextureId[i] = -1;
        }

        visualProperties = new float[wheelCount * VehicleEntityProperties.EnumVisualProperties.values().length];
        prevVisualProperties = new float[visualProperties.length];
    }

    @Override
    public void initPhysicsEntity(@Nullable BaseWheeledVehiclePhysicsHandler<?> handler) {
        if (handler != null) {
            wheelsPhysics = new WheelsPhysicsHandler(this, handler);
            wheelsPhysics.init();
            for (byte i = 0; i < wheelsStates.get().length; i++) {
                if (wheelsStates.get()[i] == WheelState.REMOVED)
                    wheelsPhysics.removeWheel(i);
                else
                    wheelsPhysics.getWheelByPartIndex(i).setFlattened(wheelsStates.get()[i] == WheelState.ADDED_FLATTENED);
            }
        }
    }

    @Override
    public void preUpdatePhysics(boolean simulatePhysics) {
        if (!simulatePhysics) {
            return;
        }
        if (entity.tickCount > 10) {
            for (int i = 0; i < wheelsPhysics.vehicleWheelData.size(); i++) {
                WheelPhysics w = wheelsPhysics.vehicleWheelData.get(i);
                if (w == null) {
                    continue;
                }
                Vector3f pos = Vector3fPool.get();
                w.getPhysicsWheel().getCollisionLocation(pos);
                BlockPos bp = new BlockPos((int) pos.x, (int) (Math.ceil(pos.y) - 1), (int) pos.z);
                BlockState blockState = entity.level().getBlockState(bp);
                float[] frictionValues = ContentPackLoader.getBlockFriction(blockState.getBlock());
                // TODO port:1.20.1 - Biome.canRain() -> getPrecipitationAt(pos) != NONE.
                boolean isBlockWet = entity.level().getBiome(bp).value().getPrecipitationAt(bp) == Biome.Precipitation.RAIN && entity.level().isRaining() && entity.level().canSeeSky(bp);
                float frictionValue = isBlockWet ? frictionValues[1] : frictionValues[0];
                w.setGrip((w.isFlattened() ? 0.16f : 1) * frictionValue);

                WheelState wheelState = wheelsStates.get()[i];
                if (wheelState == WheelState.ADDED && w.isFlattened()) {
                    wheelsStates.get()[i] = WheelState.ADDED_FLATTENED;
                    wheelsStates.setChanged(true);
                } else if (wheelState == WheelState.ADDED_FLATTENED && !w.isFlattened()) {
                    wheelsStates.get()[i] = WheelState.ADDED;
                    wheelsStates.setChanged(true);
                }
            }
        }
        wheelsPhysics.update();
    }

    @Override
    public void postUpdatePhysics(boolean simulatingPhysics) {
        System.arraycopy(visualProperties, 0, prevVisualProperties, 0, prevVisualProperties.length);
        if (simulatingPhysics) {
            updateVisualProperties();
        }
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        int wheelCount = Math.min(tag.getByte("WheelCount"), synchronizedWheelInfos.get().size());
        for (byte i = 0; i < wheelCount; i++) {
            PartWheelInfo info = DynamXObjectLoaders.WHEELS.findInfo(tag.getString("WheelInfo" + i));
            if (info != null) {
                setWheelInfo(i, info);
            }
            wheelsStates.get()[i] = WheelState.values()[tag.getByte("WheelState" + i)];
        }
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        tag.putByte("WheelCount", (byte) wheelsStates.get().length);
        for (byte i = 0; i < wheelsStates.get().length; i++) {
            tag.putByte("WheelState" + i, (byte) wheelsStates.get()[i].ordinal());
            tag.putString("WheelInfo" + i, synchronizedWheelInfos.get().get(i));
        }
    }

    public void updateVisualProperties() {
        if (wheelsPhysics == null) {
            return;
        }
        byte numWheels = wheelsPhysics.getNumWheels();

        for (int i = 0; i < numWheels; i++) {
            VehicleWheel info = wheelsPhysics.getHandler().getPhysicsVehicle().getWheel(i);
            if (info.isFrontWheel()) {
                visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.STEER_ANGLE)] = (float) Math.toDegrees(info.getSteerAngle());
            }

            int indexRotationAngle = VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.ROTATION_ANGLE);
            float[] angles = DynamXMath.interpolateAngle((float) (Math.toDegrees(info.getRotationAngle()) % 360), visualProperties[indexRotationAngle], 1);
            prevVisualProperties[indexRotationAngle] = angles[0];
            visualProperties[indexRotationAngle] = angles[1];

            visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.SUSPENSION_LENGTH)] = info.getSuspensionLength();
            Vector3f pos = Vector3fPool.get();
            info.getCollisionLocation(pos);
            visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.COLLISION_X)] = pos.x;
            visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.COLLISION_Y)] = pos.y;
            visualProperties[VehicleEntityProperties.getPropertyIndex(i, VehicleEntityProperties.EnumVisualProperties.COLLISION_Z)] = pos.z;
        }
        for (byte b = 0; b < numWheels; b++) {
            WheelPhysics w = wheelsPhysics.getWheel(b);
            if (w != null) {
                skidInfos.set(b, w.getSkidInfo());
            }
        }
    }

    public WheelsPhysicsHandler getPhysicsHandler() {
        return wheelsPhysics;
    }

    public float[] getSkidInfos() {
        return skidInfos.get();
    }

    @OnlyIn(Dist.CLIENT)
    public void spawnPropulsionParticles(RenderPhysicsEntity<?> render, float partialTicks) {
        //Dust particles when the vehicle friction is very low
        entity.getPackInfo().getPartsByType(PartWheel.class).forEach(partWheel -> {
            PartWheelInfo info = getWheelInfo(partWheel.getId());
            if (info == null || !info.isModelValid() || info.getSkidParticle() == null) {
                return;
            }
            if (!(skidInfos.get()[partWheel.getId()] < 0.1f)) {
                return;
            }
            // TODO port:1.20.1 - World#spawnParticle(EnumParticleTypes, ...) -> Level#addParticle(ParticleOptions, ...).
            entity.level().addParticle(info.getSkidParticle(),
                    visualProperties[VehicleEntityProperties.getPropertyIndex(partWheel.getId(), VehicleEntityProperties.EnumVisualProperties.COLLISION_X)],
                    visualProperties[VehicleEntityProperties.getPropertyIndex(partWheel.getId(), VehicleEntityProperties.EnumVisualProperties.COLLISION_Y)],
                    visualProperties[VehicleEntityProperties.getPropertyIndex(partWheel.getId(), VehicleEntityProperties.EnumVisualProperties.COLLISION_Z)],
                    0, 0, 0);
        });
    }

    @Override
    public byte getInitPriority() {
        //Take care to add wheels module BEFORE engine module (an engine needs a propulsion)
        return 10;
    }

    public final Map<Integer, VehicleSound> sounds = new HashMap<>();

    public enum WheelState {
        ADDED,
        ADDED_FLATTENED,
        REMOVED
    }
}
