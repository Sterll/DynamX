package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import lombok.Getter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Wheel contained in a wheel file
 *
 * TODO port:1.20.1 - Original implemented IModelTextureVariantsSupplier (fr.dynamx.api.dxmodel)
 *   and referenced ObjObjectRenderer (fr.dynamx.client.renders.model.renderer) and SceneNode
 *   (fr.dynamx.client.renders.scene.node). All live in Phase 7. Implements relaxed; the
 *   texture variants accessor returns the MaterialVariantsInfo as Object.
 */
public class PartWheelInfo extends SubInfoTypeOwner<PartWheelInfo> implements IModelPackObject {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.WHEELS)
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("WheelRadius".equals(key))
            return new IPackFilePropertyFixer.FixResult("Radius", true);
        if ("Textures".equals(key))
            return new IPackFilePropertyFixer.FixResult("MaterialVariants", true, true);
        return null;
    };

    private final String packName;
    @Getter
    private final String partName;

    @PackFileProperty(configNames = "Model", description = "common.model", type = DefinitionType.DynamXDefinitionTypes.DYNX_RESOURCE_LOCATION, defaultValue = "obj/nom_du_vehicule/nom_du_modele.obj", required = false)
    private ResourceLocation model;
    @Getter
    @PackFileProperty(configNames = "Width")
    private float wheelWidth;
    @Getter
    @PackFileProperty(configNames = "Radius")
    private float wheelRadius;
    @Getter
    @PackFileProperty(configNames = "RimRadius")
    private float rimRadius;
    @Getter
    @PackFileProperty(configNames = "Friction")
    private float wheelFriction;
    @Getter
    @PackFileProperty(configNames = "BrakeForce")
    private float wheelBrakeForce;
    @Getter
    @PackFileProperty(configNames = "HandBrakeForce", required = false, defaultValue = "2*BrakeForce")
    private float handBrakeForce = -1;
    @Getter
    @PackFileProperty(configNames = "RollInInfluence")
    private float wheelRollInInfluence;
    @Getter
    @PackFileProperty(configNames = "SuspensionRestLength")
    private float suspensionRestLength;
    @Getter
    @PackFileProperty(configNames = "SuspensionStiffness")
    private float suspensionStiffness;
    @Getter
    @PackFileProperty(configNames = "SuspensionMaxForce")
    private float suspensionMaxForce;
    @Getter
    @PackFileProperty(configNames = "WheelDampingRelaxation")
    private float wheelsDampingRelaxation;
    @Getter
    @PackFileProperty(configNames = "WheelsDampingCompression")
    private float wheelsDampingCompression;
    @Getter
    @PackFileProperty(configNames = "SkidParticle", required = false)
    private ParticleOptions skidParticle = ParticleTypes.SMOKE;

    @Getter
    @PackFileProperty(configNames = "ScaleModifier", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false)
    private Vector3f scaleModifier = new Vector3f(1, 1, 1);

    @Deprecated
    @PackFileProperty(configNames = "Textures", required = false, type = DefinitionType.DynamXDefinitionTypes.STRING_ARRAY_2D, defaultValue = "\"Textures: DynamX\"")
    private String[][] texturesArray;

    public PartWheelInfo(String packName, String partName) {
        this.packName = packName;
        this.partName = partName;
    }

    @Override
    public String getName() {
        return getPartName();
    }

    @Override
    public String getPackName() {
        return packName;
    }

    @Override
    public String getFullName() {
        return getPackName() + "." + getPartName();
    }

    /**
     * Called when this info is fully loaded. Originally @Override of IModelPackObject (Phase 7);
     * not yet present on the ported interface. Kept as a public method.
     */
    public void onComplete(boolean hotReload) {
        if (handBrakeForce == -1)
            handBrakeForce = wheelBrakeForce * 2;
        wheelRadius = getWheelRadius() * getScaleModifier().z;
        wheelWidth = getWheelWidth() * getScaleModifier().x;
        if (texturesArray != null)
            new MaterialVariantsInfo<>(this, texturesArray).appendTo(this);
    }

    public MaterialVariantsInfo<?> getVariants() {
        return getSubPropertyByType(MaterialVariantsInfo.class);
    }

    public byte getIdForVariant(String textureName) {
        MaterialVariantsInfo<?> variantsInfo = getVariants();
        if (variantsInfo != null) {
            // TODO port:1.20.1 - Once TextureVariantData is ported, iterate over variantsMap.values()
            //   and compare with TextureVariantData#getName(). For now we cannot resolve names.
        }
        return 0;
    }

    @Override
    public fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier.IModelTextureVariants getTextureVariantsFor(fr.dynamx.client.renders.model.renderer.ObjObjectRenderer objObjectRenderer) {
        // TODO port:1.20.1 - Original returned getVariants() (tyre vs rim distinction). Returning null
        //   until MaterialVariantsInfo implements IModelTextureVariants (Phase 7).
        return null;
    }

    public boolean hasTextureVariants() {
        return getSubPropertyByType(MaterialVariantsInfo.class) != null;
    }

    public byte getMaxVariantId() {
        return (byte) (hasTextureVariants() ? getVariants().getVariantsMap().size() : 1);
    }

    @Override
    @Nullable
    public ResourceLocation getModel() {
        return model;
    }

    @Override
    public String toString() {
        return "PartWheelInfo named " + getFullName();
    }

    @Override
    public fr.dynamx.client.renders.scene.node.SceneNode<?, ?> getSceneGraph() {
        throw new UnsupportedOperationException();
    }
}
