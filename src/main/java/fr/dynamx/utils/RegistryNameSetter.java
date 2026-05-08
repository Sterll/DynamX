package fr.dynamx.utils;

import net.minecraft.resources.ResourceLocation;

/**
 * ResourceLocation helpers.
 *
 * NOTE port:1.20.1 - In 1.20.1 the registry system uses DeferredRegister which
 * sets registry names automatically; the old IForgeRegistryEntry.Impl#registryName
 * reflection hack is no longer needed and has been removed. The
 * setRegistryName(...) helpers are dropped accordingly. Callers should be
 * migrated to DeferredRegister/Holder<T> when items/blocks are ported.
 */
public class RegistryNameSetter {

    public static ResourceLocation getResourceLocationWithDynamXDefault(String resourceName) {
        return getDynamXResourceLocation("", resourceName);
    }

    public static ResourceLocation getDynamXModelResourceLocation(String resourceName) {
        return getDynamXResourceLocation("models/", resourceName);
    }

    public static ResourceLocation getDynamXResourceLocation(String pathPrefix, String resourceName) {
        String[] astring = new String[]{DynamXConstants.ID, pathPrefix + resourceName};
        int i = resourceName.indexOf(58);
        if (i >= 0) {
            astring[1] = resourceName.substring(i + 1);
            if (i > 1) {
                astring[0] = resourceName.substring(0, i);
            }
        }
        return new ResourceLocation(astring[0], astring[1]);
    }
}
