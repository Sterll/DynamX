package fr.dynamx.api.dxmodel;

/**
 * NOTE port:1.20.1 - The OBJ format is being dropped for the 1.20.1 port.
 * The enum keeps OBJ for now so legacy content packs still parse, but the
 * loader will reject them and the rendering pipeline is GLTF only.
 */
public enum EnumDxModelFormats {

    OBJ, GLTF, JSON;

    public static boolean isDxModel(String path) {
        return path.endsWith(OBJ.name().toLowerCase()) || path.endsWith(GLTF.name().toLowerCase());
    }
}
