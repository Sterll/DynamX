// TODO port:1.20.1 OBJ loader stub - OBJLoader static helper
package fr.dynamx.common.objloader;

import java.util.ArrayList;
import java.util.List;

public class OBJLoader {
    private static final List<MTLLoader> MTL_LOADERS = new ArrayList<>();

    public static List<MTLLoader> getMtlLoaders() {
        return MTL_LOADERS;
    }
}
