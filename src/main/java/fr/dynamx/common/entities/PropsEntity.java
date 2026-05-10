package fr.dynamx.common.entities;

import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.physics.entities.PackEntityPhysicsHandler;
import fr.dynamx.common.physics.entities.PropPhysicsHandler;
import fr.dynamx.utils.DynamXConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;

/**
 * Pack-based "props" entity (movable objects).
 */
// TODO port:1.20.1 - PropObject and SeatsModule are forward references (Phase 4b / 6).
public class PropsEntity<T extends PackEntityPhysicsHandler<PropObject<?>, ?>> extends PackPhysicsEntity<T, PropObject<?>> implements IModuleContainer.ISeatsContainer {
    private SeatsModule seats;

    public PropsEntity(EntityType<? extends PropsEntity<?>> type, Level level) {
        super(type, level);
    }

    public PropsEntity(EntityType<? extends PropsEntity<?>> type, String infoName, Level level, Vector3f pos, float spawnRotationAngle, int metadata) {
        super(type, infoName, level, pos, spawnRotationAngle, metadata);
    }

    // TODO port:1.20.1 - Legacy (Level) constructor.
    public PropsEntity(Level level) {
        super(level);
    }

    @Override
    public PropObject<?> createInfo(String infoName) {
        return DynamXObjectLoaders.PROPS.findInfo(infoName);
    }

    @Override
    public void tick() {
        super.tick();
        if (getPackInfo() == null) {
            return;
        }
        if (getPackInfo().getDespawnTime() != -1) {
            if ((tickCount % getPackInfo().getDespawnTime()) == 0) {
                discard();
            }
        }
    }

    @Override
    public T createPhysicsHandler() {
        return (T) new PropPhysicsHandler(this);
    }

    @Override
    protected final void fireCreateModulesEvent(Dist side) {
        //Don't simplify the generic type, for fml (kept for backwards-compatible call shape)
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.CreateModules<>(PropsEntity.class, this, (java.util.List) moduleList, side));
    }

    @Override
    public int getSyncTickRate() {
        return DynamXConfig.propsSyncTickRate;
    }

    @Override
    protected void getListenerModules() {
        super.getListenerModules();
        seats = getModuleByType(SeatsModule.class);
    }

    @Nonnull
    @Override
    public SeatsModule getSeats() {
        return seats;
    }

    @Override
    public PackPhysicsEntity<?, ?> cast() {
        return this;
    }
}
