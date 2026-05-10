// TODO port:1.20.1 stub - StyleCustomizer chainable wrapper
package fr.aym.acsguis.component;

import fr.aym.acsguis.cssengine.style.EnumCssStyleProperty;
import fr.aym.acsguis.utils.GuiConstants;

public class StyleCustomizer {
    private final GuiComponent owner;
    private int foregroundColor;
    private int backgroundColor;
    private float height;
    private final Position xPos = new Position();
    private final Position yPos = new Position();

    public StyleCustomizer(GuiComponent owner) { this.owner = owner; }

    public GuiComponent getOwner() { return owner; }

    public StyleCustomizer setPaddingLeft(int v) { return this; }
    public StyleCustomizer setPaddingRight(int v) { return this; }
    public StyleCustomizer setPaddingTop(int v) { return this; }
    public StyleCustomizer setPaddingBottom(int v) { return this; }
    public StyleCustomizer setPaddingX(int v) { return this; }
    public StyleCustomizer setPaddingY(int v) { return this; }
    public StyleCustomizer setWidth(float v) { return this; }
    public StyleCustomizer setHeight(float v) { this.height = v; return this; }
    public StyleCustomizer setSize(float w, float h) { return this; }
    public StyleCustomizer setX(float v) { return this; }
    public StyleCustomizer setY(float v) { return this; }
    public StyleCustomizer setForegroundColor(int c) { this.foregroundColor = c; return this; }
    public StyleCustomizer setBackgroundColor(int c) { this.backgroundColor = c; return this; }

    public Position getXPos() { return xPos; }
    public Position getYPos() { return yPos; }

    public void refreshStyle() {}

    public StyleCustomizer withAutoStyles(AutoStyleHandler handler, EnumCssStyleProperty... properties) {
        return this;
    }

    @FunctionalInterface
    public interface AutoStyleHandler {
        boolean handle(EnumCssStyleProperty property, Object context, StyleCustomizer target);
    }

    public static class Position {
        public void setAbsolute(float v) {}
        public void setAbsolute(float v, GuiConstants.ENUM_RELATIVE_POS pos) {}
    }
}
