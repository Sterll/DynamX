package fr.dynamx.client.renders.mesh;

/**
 * Enumerate options for auto-generating mesh normals from vertex positions.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum NormalsOption {
    /**
     * face normals (similar to Blender's "flat" shading)
     */
    Facet,
    /**
     * no normals (delete if present)
     */
    None,
    /**
     * smoothed normals
     */
    Smooth,
    /**
     * sphere normals (normal direction is determined by vertex position)
     */
    Sphere
}
