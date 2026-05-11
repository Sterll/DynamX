package fr.dynamx.common.contentpack.type.objects;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.ICollisionsContainer;
import fr.dynamx.api.contentpack.object.part.InteractivePart;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.common.contentpack.type.ObjectCollisionsHelper;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO port:1.20.1 - Original referenced:
 *   - fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier (Phase 7, not yet ported) - used as
 *     interface marker for getTextureVariantsFor()/hasTextureVariants()/getMaxVariantId().
 *     The methods are kept but relaxed/typed against Object so we stay source-compatible.
 *   - fr.dynamx.client.renders.model.renderer.ObjObjectRenderer (Phase 7) - parameter relaxed to Object.
 */
public abstract class AbstractProp<T extends AbstractProp<T>> extends AbstractItemObject<T, T> implements ICollisionsContainer, ParticleEmitterInfo.IParticleEmitterContainer {
    @Getter
    @Setter
    @PackFileProperty(configNames = "Translate", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0 0")
    protected Vector3f translation = new Vector3f(0, 0, 0);
    @Getter
    @Setter
    @PackFileProperty(configNames = "Scale", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "1 1 1")
    protected Vector3f scaleModifier = new Vector3f(1, 1, 1);
    @Getter
    @Setter
    @PackFileProperty(configNames = "RenderDistanceSquared", required = false, defaultValue = "4096")
    protected float renderDistanceSquared = 4096;

    @Getter
    @Accessors(fluent = true)
    @PackFileProperty(configNames = "UseComplexCollisions", required = false, defaultValue = "false", description = "common.UseComplexCollisions")
    protected boolean useComplexCollisions = false;
    @Getter
    protected ObjectCollisionsHelper collisionsHelper;

    @Deprecated
    @PackFileProperty(configNames = "Textures", required = false, type = DefinitionType.DynamXDefinitionTypes.STRING_ARRAY_2D)
    protected String[][] texturesArray;

    private final List<ParticleEmitterInfo<?>> particleEmitters = new ArrayList<>();

    public AbstractProp(String packName, String fileName) {
        super(packName, fileName);
    }

    abstract MaterialVariantsInfo<?> getVariants();

    /**
     * TODO port:1.20.1 - Original signature:
     *   IModelTextureVariantsSupplier.IModelTextureVariants getTextureVariantsFor(ObjObjectRenderer objObjectRenderer)
     *   Both types live in Phase 7. Parameter relaxed to Object; return relaxed to Object.
     */
    public Object getTextureVariantsFor(Object objObjectRenderer) {
        return getVariants();
    }

    public boolean hasTextureVariants() {
        return getVariants() != null;
    }

    public byte getMaxVariantId() {
        return (byte) (hasTextureVariants() ? getVariants().getVariantsMap().size() : 1);
    }

    @Override
    public void addParticleEmitter(ParticleEmitterInfo<?> emitterInfo) {
        particleEmitters.add(emitterInfo);
    }

    @Override
    public List<ParticleEmitterInfo<?>> getParticleEmitters() {
        return particleEmitters;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <A extends InteractivePart<?, ?>> List<A> getInteractiveParts() {
        // TODO port:1.20.1 - go through raw List for capture compatibility under 1.20.1 javac.
        return (List<A>) (List) getPartsByType(InteractivePart.class);
    }

    @Override
    public float getBaseItemScale() {
        return 0.3f;
    }
}
