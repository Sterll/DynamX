// TODO port:1.20.1 stub - IGltfModelReceiver interface
package com.modularmods.mcgltf.dynamx;

import net.minecraft.resources.ResourceLocation;

public interface IGltfModelReceiver {
    ResourceLocation getModelLocation();

    default void onReceiveSharedModel(RenderedGltfScene scene) {
        // TODO port:1.20.1 stub
    }
}
