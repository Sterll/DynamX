package fr.dynamx.common.objloader.data;

import com.jme3.math.Vector3f;
import fr.dynamx.api.dxmodel.DxModelPath;
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.common.objloader.OBJLoader;
import fr.dynamx.utils.DynamXUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.fml.loading.FMLLoader;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An OBJ model loaded from a pack. Used both for collisions (server side) and rendering data (client side).
 */
public class ObjModelData extends DxModelData {

    @Getter
    private final List<ObjObjectData> objObjects = new ArrayList<>();
    @Getter
    private final Map<String, Material> materials = new HashMap<>();

    public ObjModelData(DxModelPath path) {
        super(path);
        try {
            InputStream stream;
            ResourceLocation location = path.getModelPath();
            ResourceLocation startPath = new ResourceLocation(location.getNamespace(),
                    location.getPath().substring(0, location.getPath().lastIndexOf("/") + 1));
            boolean clientSide = !FMLLoader.getDist().isDedicatedServer();
            OBJLoader.MtlResolver resolver = null;
            if (clientSide) {
                stream = openClient(path);
                resolver = mtlName -> openClientRelative(path, startPath, mtlName);
            } else {
                stream = openServer(path);
                resolver = null; // server doesn't need mtl/textures
            }
            byte[] bytes = DynamXUtils.readInputStream(stream);
            String content = new String(bytes, StandardCharsets.UTF_8);
            // TODO port:1.20.1 - TEMP DIAGNOSTIC: hash + size of the loaded OBJ bytes per cache key
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
                byte[] dig = md.digest(bytes);
                StringBuilder hex = new StringBuilder();
                for (int i = 0; i < Math.min(8, dig.length); i++) hex.append(String.format("%02x", dig[i]));
                org.apache.logging.log4j.LogManager.getLogger("DynamX-ObjDump")
                    .info("    +--[ OBJ LOAD ]-- path={} bytes={} sha1[0..8]={}",
                          location, bytes.length, hex.toString());
            } catch (Exception ignored) {}
            new OBJLoader(objObjects, materials).readAndLoadModel(clientSide ? startPath : null, resolver, content);
        } catch (Exception e) {
            throw new RuntimeException("OBJ model " + path + " cannot be loaded: " + e.getMessage(), e);
        }
    }

    private static InputStream openClient(DxModelPath path) throws Exception {
        // First try Minecraft resource manager (works for builtin / resource pack)
        try {
            java.util.Optional<Resource> r = Minecraft.getInstance().getResourceManager().getResource(path.getModelPath());
            if (r.isPresent()) {
                return r.get().open();
            }
        } catch (Exception ignored) {
        }
        return openServer(path);
    }

    private static InputStream openServer(DxModelPath path) throws Exception {
        for (PackInfo packInfo : path.getPackLocations()) {
            InputStream is = packInfo.readFile(path.getModelPath());
            if (is != null) return is;
        }
        throw new java.io.FileNotFoundException("Model not found: " + path);
    }

    private static InputStream openClientRelative(DxModelPath path, ResourceLocation startPath, String fileName) throws Exception {
        ResourceLocation rl = new ResourceLocation(startPath.getNamespace(), startPath.getPath() + fileName);
        try {
            java.util.Optional<Resource> r = Minecraft.getInstance().getResourceManager().getResource(rl);
            if (r.isPresent()) return r.get().open();
        } catch (Exception ignored) {
        }
        for (PackInfo packInfo : path.getPackLocations()) {
            InputStream is = packInfo.readFile(rl);
            if (is != null) return is;
        }
        return null;
    }

    @Override
    public float[] getVerticesPos(String objectName) {
        for (ObjObjectData o : objObjects) {
            if (!o.getName().toLowerCase().contains(objectName.toLowerCase())) continue;
            float[] pos = new float[o.getVertices().length * 3];
            for (int i = 0; i < o.getVertices().length; i++) {
                pos[i * 3] = o.getVertices()[i].getPos().x;
                pos[i * 3 + 1] = o.getVertices()[i].getPos().y;
                pos[i * 3 + 2] = o.getVertices()[i].getPos().z;
            }
            return pos;
        }
        return new float[0];
    }

    @Override
    public int[] getMeshIndices(String objectName) {
        ObjObjectData o = getObjObject(objectName);
        return o == null ? new int[0] : o.getIndices();
    }

    public Vector3f getMeshMin(String name, @Nullable Vector3f result) {
        ObjObjectData o = getObjObject(name);
        return o == null ? null : o.min(result);
    }

    public Vector3f getMeshMax(String name, @Nullable Vector3f result) {
        ObjObjectData o = getObjObject(name);
        return o == null ? null : o.max(result);
    }

    public Vector3f getMinOfModel(@Nullable Vector3f result) {
        if (objObjects.isEmpty()) return result == null ? new Vector3f() : result.set(0, 0, 0);
        Vector3f first = objObjects.get(0).min(result);
        float minX = first.x, minY = first.y, minZ = first.z;
        for (int i = 1; i < objObjects.size(); i++) {
            Vector3f m = objObjects.get(i).min(result);
            if (m.x < minX) minX = m.x;
            if (m.y < minY) minY = m.y;
            if (m.z < minZ) minZ = m.z;
        }
        return result == null ? new Vector3f(minX, minY, minZ) : result.set(minX, minY, minZ);
    }

    public Vector3f getMaxOfModel(@Nullable Vector3f result) {
        if (objObjects.isEmpty()) return result == null ? new Vector3f() : result.set(0, 0, 0);
        Vector3f first = objObjects.get(0).max(result);
        float maxX = first.x, maxY = first.y, maxZ = first.z;
        for (int i = 1; i < objObjects.size(); i++) {
            Vector3f m = objObjects.get(i).max(result);
            if (m.x > maxX) maxX = m.x;
            if (m.y > maxY) maxY = m.y;
            if (m.z > maxZ) maxZ = m.z;
        }
        return result == null ? new Vector3f(maxX, maxY, maxZ) : result.set(maxX, maxY, maxZ);
    }

    public ObjObjectData getObjObject(String objectName) {
        return objObjects.stream().filter(o -> o.getName().equalsIgnoreCase(objectName)).findFirst().orElse(null);
    }

    @Override
    public List<String> getMeshNames() {
        return objObjects.stream().map(o -> o.getName().toLowerCase()).collect(Collectors.toList());
    }
}
