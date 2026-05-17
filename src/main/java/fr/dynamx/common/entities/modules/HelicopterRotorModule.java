package fr.dynamx.common.entities.modules;

import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.engines.HelicopterEngineModule;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.LogicalSide;

/**
 * Rotor visuals + downwash particles.
 */
// TODO port:1.20.1 - IEntityAdditionalSpawnData replaced with ByteBuf reflection helpers on
// ModularPhysicsEntity.
public class HelicopterRotorModule implements IPhysicsModule<BaseVehiclePhysicsHandler<?>>, IPhysicsModule.IEntityUpdateListener {
    protected final BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity;
    private HelicopterEngineModule engine;

    @Getter
    private float curPower, curAngle;

    public HelicopterRotorModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity) {
        this.entity = entity;
    }

    @Override
    public void initEntityProperties() {
        engine = entity.getModuleByType(HelicopterEngineModule.class);
    }

    @Override
    public boolean listenEntityUpdates(LogicalSide side) {
        return side.isClient();
    }

    @Override
    public void updateEntity() {
        if (engine != null) {
            float targetPower = engine.getPower();
            curPower = curPower + (targetPower - curPower) / 60; //3-seconds interpolation
            curAngle += curPower;
        }
        if (entity.level().isClientSide) {
            BlockPos pos = entity.blockPosition();
            int height = pos.getY() - entity.level().getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
            if (height < 10) {
                renderParticles(entity, height);
            }
        }
    }

    private void renderParticles(BaseVehicleEntity<?> entity, int height) {
        Level world = entity.level();
        for (int i = 0; i < 360; i += 2) {
            int power = (int) (engine.getPower() * 10);

            if (world.random.nextInt(100) < power) {
                float minRadius = 5.5f - height * 0.5f;
                float radius = world.random.nextFloat() * 4;

                double x = Math.cos(Math.toRadians(i)) * (minRadius + radius);
                double z = Math.sin(Math.toRadians(i)) * (minRadius + radius);

                double y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) (entity.blockPosition().getX() + x), (int) (entity.blockPosition().getZ() + z));
                double zSpeed = Math.sin(Math.toRadians(i)) * 0.9;
                double xSpeed = Math.cos(Math.toRadians(i)) * 0.9;

                if (world.isEmptyBlock(new BlockPos((int) (entity.blockPosition().getX() + x), (int) (y), (int) (entity.blockPosition().getZ() + z)))) {
                    world.addParticle(ParticleTypes.EXPLOSION, entity.getX() + x, y, entity.getZ() + z, xSpeed, 0, zSpeed);
                }
            }
        }
    }

    /**
     * Per-module spawn data hook invoked reflectively by {@link fr.dynamx.common.entities.ModularPhysicsEntity}.
     */
    public void writeSpawnData(ByteBuf buffer) {
        // curPower isn't computed on server side
        buffer.writeFloat(engine != null ? engine.getPower() : 0);
    }

    public void readSpawnData(ByteBuf additionalData) {
        curPower = additionalData.readFloat();
    }
}
