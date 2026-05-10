// TODO port:1.20.1 stub - Size helper for CSS-like sizing
package fr.aym.acsguis.cssengine.positionning;

import fr.aym.acsguis.utils.GuiConstants;

public class Size {
    public static class SizeValue {
        private final float value;
        private final GuiConstants.ENUM_SIZE type;

        public SizeValue(float value, GuiConstants.ENUM_SIZE type) {
            this.value = value;
            this.type = type;
        }

        public float getValue() { return value; }
        public GuiConstants.ENUM_SIZE getType() { return type; }
    }
}
