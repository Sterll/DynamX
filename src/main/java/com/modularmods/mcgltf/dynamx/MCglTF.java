// TODO port:1.20.1 stub - MCglTF main facade
package com.modularmods.mcgltf.dynamx;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class MCglTF {
    private static final MCglTF INSTANCE = new MCglTF();
    public static final Map<ResourceLocation, Object> lookup = new HashMap<>();

    public static MCglTF getInstance() { return INSTANCE; }

    public void registerModel(ResourceLocation location) {
        // TODO port:1.20.1 stub
    }

    public void addGltfModelReceiver(IGltfModelReceiver receiver) {
        // TODO port:1.20.1 stub
    }

    public void createShaderSkinningProgram() {
        // TODO port:1.20.1 stub
    }

    public void reloadModels() {
        // TODO port:1.20.1 stub
    }
}
