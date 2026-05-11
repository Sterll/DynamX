// TODO port:1.20.1 stub - GuiComponent base class with chainable API
package fr.aym.acsguis.component;

import fr.aym.acsguis.cssengine.style.EnumCssStyleProperty;
import fr.aym.acsguis.event.listeners.mouse.IMouseMoveListener;
import fr.aym.acsguis.utils.ComponentRenderContext;

import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

public class GuiComponent {
    private boolean enabled = true;
    private boolean visible = true;
    private boolean focused;
    private String cssId = "";
    private String cssClass = "";
    private final StyleCustomizer styleCustomizer = new StyleCustomizer(this);

    public GuiComponent setEnabled(boolean enabled) { this.enabled = enabled; return this; }
    public boolean isEnabled() { return enabled; }

    public boolean isVisible() { return visible; }
    public GuiComponent setVisible(boolean visible) { this.visible = visible; return this; }

    public GuiComponent setFocused(boolean focused) { this.focused = focused; return this; }
    public boolean isFocused() { return focused; }

    public GuiComponent setCssId(String id) { this.cssId = id; return this; }
    public String getCssId() { return cssId; }

    public GuiComponent setCssClass(String cls) { this.cssClass = cls; return this; }
    public String getCssClass() { return cssClass; }

    public StyleCustomizer getStyleCustomizer() { return styleCustomizer; }
    public StyleCustomizer getStyle() { return styleCustomizer; }

    public GuiComponent addClickListener(ClickListener l) { return this; }
    public GuiComponent addMoveListener(IMouseMoveListener l) { return this; }
    public GuiComponent addWheelListener(IntConsumer l) { return this; }
    public GuiComponent addTickListener(Runnable r) { return this; }

    public void drawForeground(int mouseX, int mouseY, float partialTicks, ComponentRenderContext renderContext) {}
    public void drawBackground(int mouseX, int mouseY, float partialTicks, ComponentRenderContext renderContext) {}

    public void onKeyTyped(char typedChar, int keyCode) {}

    public void remove(GuiComponent component) {}
    public void add(GuiComponent component) {}
    public void removeAllChildren() {}

    public StubLayout getLayout() { return new StubLayout(); }

    // TODO port:1.20.1 stub - tick hook used by HUD panels.
    public boolean tick() { return true; }

    @FunctionalInterface
    public interface ClickListener {
        void onClick(int mouseX, int mouseY, int button);
    }

    public static class StubLayout {
        public void clear() {}
    }
}
