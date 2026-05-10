package fr.dynamx.api.entities;

/**
 * References to all BaseVehicleEntity basic properties, adds methods to add custom properties
 */
// TODO port:1.20.1 - Forge's EnumHelper.addEnum was removed after 1.12; the dynamic addVisualProperty
// and addEngineProperty methods now throw at call sites (addons relying on them will need to migrate
// to a registry-based system in a later phase). The static enum values remain valid.
public class VehicleEntityProperties {
    /**
     * Float properties stored in all WheelsModule <br>
     * One wheel has all of these properties
     */
    public enum EnumVisualProperties {
        STEER_ANGLE(1),
        ROTATION_ANGLE(1),
        SUSPENSION_LENGTH(0),
        COLLISION_X(0),
        COLLISION_Y(0),
        COLLISION_Z(0);

        /**
         * Determines the interpolation type when synced <br>
         * 0 = linear interpolation, 1 = angular interpolation
         */
        public final byte type;

        EnumVisualProperties(int type) {
            this.type = (byte) type;
        }
    }

    /**
     * Float properties stored in all CarEngineModules <br>
     * Describes the engine state
     */
    public enum EnumEngineProperties {
        /**
         * Vehicle speed
         */
        SPEED,
        /**
         * Engine rpm
         */
        REVS,
        /**
         * The activer gear of the gearbox
         */
        ACTIVE_GEAR
    }

    /**
     * Returns the index of a wheel visual property in the visualProperties array, for WheelsModule
     *
     * @param partIndex            The wheel id
     * @param enumVisualProperties The visual property
     * @return The index of the visual property in the array containing all wheel's properties
     */
    public static int getPropertyIndex(int partIndex, EnumVisualProperties enumVisualProperties) {
        return EnumVisualProperties.values().length * partIndex + enumVisualProperties.ordinal();
    }

    /**
     * Returns a property from its index in the visualProperties array, not depending on the wheel, for WheelsModule
     *
     * @param index The property index in the visualProperties array
     * @return The property
     */
    public static EnumVisualProperties getPropertyByIndex(int index) {
        return EnumVisualProperties.values()[index % EnumVisualProperties.values().length];
    }

    /**
     * Adds a visual property, each wheel of each WheelsModule will contain this property, and it will be automatically synced over the network
     *
     * @param name              The property name, should be unique (add your modid)
     * @param interpolationType The interpolation type for sync : 0 = linear interpolation, 1 = angular interpolation
     * @return The property instance
     */
    public static EnumVisualProperties addVisualProperty(String name, byte interpolationType) {
        for (EnumVisualProperties prop : EnumVisualProperties.values()) {
            if (prop.name().equalsIgnoreCase(name))
                throw new IllegalArgumentException("Visual property with name " + name + " already exists !");
        }
        // TODO port:1.20.1 - EnumHelper.addEnum() was removed after Forge 1.12. Dynamic enum extension
        // needs replacement (e.g. registry of EnumVisualProperty-like objects). For now this throws.
        throw new UnsupportedOperationException(
                "Dynamic visual property registration is not yet supported on NeoForge 1.20.1 (was Forge EnumHelper.addEnum). Property requested: " + name);
    }

    /**
     * Adds an engine property, each CarEngineModule will contain this property, and it will be automatically synced over the network
     *
     * @param name The property name, should be unique (add your modid)
     * @return The property instance
     */
    public static EnumEngineProperties addEngineProperty(String name) {
        for (EnumEngineProperties prop : EnumEngineProperties.values()) {
            if (prop.name().equalsIgnoreCase(name))
                throw new IllegalArgumentException("Engine property with name " + name + " already exists !");
        }
        // TODO port:1.20.1 - EnumHelper.addEnum() was removed after Forge 1.12. See addVisualProperty.
        throw new UnsupportedOperationException(
                "Dynamic engine property registration is not yet supported on NeoForge 1.20.1 (was Forge EnumHelper.addEnum). Property requested: " + name);
    }
}
