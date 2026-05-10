package fr.dynamx.common.contentpack.type;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import lombok.Getter;
import net.minecraft.core.particles.ParticleOptions;

import java.util.Collections;
import java.util.List;

/**
 * Particle emitter sub info.
 *
 * TODO port:1.20.1 - Original stored a net.minecraft.util.EnumParticleTypes in the
 *   particleType field. In 1.20.1 the equivalent is net.minecraft.core.particles.ParticleOptions
 *   (or a ParticleType resolved from a ResourceLocation). The field type has been updated; the
 *   DefinitionType.DynamXDefinitionTypes.PARTICLE_TYPE parser must also be updated when
 *   DefinitionType is fully reworked for 1.20.1.
 */
@RegisteredSubInfoType(name = "emitter", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.HELICOPTER, SubInfoTypeRegistries.PROPS}, strictName = false)
public class ParticleEmitterInfo<T extends ISubInfoTypeOwner<T> & ParticleEmitterInfo.IParticleEmitterContainer> extends SubInfoType<T> {
    @Getter
    private final String emitterName;

    @PackFileProperty(configNames = "Type", type = DefinitionType.DynamXDefinitionTypes.PARTICLE_TYPE)
    public ParticleOptions particleType;
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    public Vector3f position = new Vector3f();
    @PackFileProperty(configNames = "Velocity", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, required = false)
    public Vector3f velocity = new Vector3f();

    public ParticleEmitterInfo(ISubInfoTypeOwner<T> owner, String emitterName) {
        super(owner);
        if (emitterName.startsWith("Emitter_")) {
            this.emitterName = emitterName.substring("Emitter_".length());
        } else {
            this.emitterName = emitterName.substring("Emitter".length());
        }
    }

    @Override
    public String getName() {
        return "ParticleEmitterInfo_" + emitterName;
    }

    @Override
    public void appendTo(T owner) {
        owner.addParticleEmitter(this);
    }

    public interface IParticleEmitterContainer {
        default void addParticleEmitter(ParticleEmitterInfo<?> emitterInfo) {
        }

        default List<ParticleEmitterInfo<?>> getParticleEmitters() {
            return Collections.emptyList();
        }
    }
}
