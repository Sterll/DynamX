package fr.dynamx.common.objloader;

import fr.dynamx.client.renders.model.texture.MaterialTexture;
import fr.dynamx.common.objloader.data.Material;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses .mtl files into {@link Material} objects.
 * <p>
 * Port note: only texture references and base colors are parsed. Specular/normal maps are
 * accepted but rendered as no-ops by the minimal 1.20.1 pipeline.
 */
public class MTLLoader {
    public static final String COMMENT = "#";
    public static final String NEW_MATERIAL = "newmtl";
    public static final String AMBIENT_COLOR = "Ka";
    public static final String DIFFUSE_COLOR = "Kd";
    public static final String SPECULAR_COLOR = "Ks";
    public static final String TRANSPARENCY_D = "d";
    public static final String TRANSPARENCY_TR = "Tr";
    public static final String ILLUMINATION = "illum";

    public static final String TEXTURE_AMBIENT = "map_Ka";
    public static final String TEXTURE_DIFFUSE = "map_Kd";
    public static final String TEXTURE_SPECULAR = "map_Ks";
    public static final String TEXTURE_NORMAL = "map_Bump";
    public static final String TEXTURE_TRANSPARENCY = "map_d";

    @Getter
    private final List<Material> materials = new ArrayList<>();

    public void parse(ResourceLocation location, String content) {
        String[] lines = content.split("\n");
        Material current = null;
        for (String s : lines) {
            String line = s.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            if (parts.length == 0) continue;
            String name = parts.length >= 3 ? parts[2].toLowerCase() : "default";
            try {
                switch (parts[0]) {
                    case COMMENT:
                        break;
                    case NEW_MATERIAL:
                        current = new Material(parts[1].toLowerCase());
                        materials.add(current);
                        break;
                    case AMBIENT_COLOR:
                        if (current != null && parts.length >= 4)
                            current.ambientColor = new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
                        break;
                    case DIFFUSE_COLOR:
                        if (current != null && parts.length >= 4)
                            current.diffuseColor = new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
                        break;
                    case TEXTURE_DIFFUSE: {
                        if (current == null || parts.length < 2) break;
                        String fileName = parts[1];
                        String texturePath = fileName.equalsIgnoreCase("white")
                                ? "textures/white.png"
                                : (location.getPath() + fileName).toLowerCase();
                        current.diffuseTexture.put(name,
                                new MaterialTexture(new ResourceLocation(location.getNamespace(), texturePath), name));
                        break;
                    }
                    case TEXTURE_AMBIENT:
                        if (current != null && parts.length >= 2)
                            current.ambientTexture.put(name, new MaterialTexture(
                                    new ResourceLocation(location.getNamespace(), (location.getPath() + parts[1]).toLowerCase()), name));
                        break;
                    case TEXTURE_SPECULAR:
                        if (current != null && parts.length >= 2)
                            current.specularTexture.put(name, new MaterialTexture(
                                    new ResourceLocation(location.getNamespace(), (location.getPath() + parts[1]).toLowerCase()), name));
                        break;
                    case TEXTURE_NORMAL:
                        if (current != null && parts.length >= 2)
                            current.normalTexture.put(name, new MaterialTexture(
                                    new ResourceLocation(location.getNamespace(), (location.getPath() + parts[1]).toLowerCase()), name));
                        break;
                    case TRANSPARENCY_D:
                    case TRANSPARENCY_TR:
                        if (current != null && parts.length >= 2)
                            current.transparency = (float) Double.parseDouble(parts[1]);
                        break;
                    default:
                        break;
                }
            } catch (Exception ignored) {
            }
        }
    }

    public void loadTextures() {
        for (Material material : materials) {
            material.diffuseTexture.forEach((k, t) -> t.loadTexture());
        }
    }

    public void uploadTextures() {
        for (Material material : materials) {
            material.diffuseTexture.forEach((k, t) -> t.uploadTexture());
        }
    }
}
