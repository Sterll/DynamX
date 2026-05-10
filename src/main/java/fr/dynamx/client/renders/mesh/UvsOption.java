package fr.dynamx.client.renders.mesh;

/**
 * Enumerate options for auto-generating texture coordinates for vertices.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum UvsOption {
    /**
     * linear transformation of vertex positions
     */
    Linear,
    /**
     * no texture coordinates (delete if present)
     */
    None,
    /**
     * convert vertex positions to spherical coordinates (r, theta, phi) and
     * then apply a linear transformation
     */
    Spherical
}
