// TODO port:1.20.1 stub - UpdatableGuiLabel with dynamic content
package fr.aym.acsguis.component.textarea;

import fr.aym.acsguis.event.listeners.mouse.IMouseMoveListener;

import java.util.List;
import java.util.function.IntConsumer;

public class UpdatableGuiLabel extends GuiLabel {
    public UpdatableGuiLabel(String format, LabelValueFunction valueFunction) {
        super(format);
    }

    @Override
    public UpdatableGuiLabel setCssId(String id) { super.setCssId(id); return this; }

    @Override
    public UpdatableGuiLabel setCssClass(String cls) { super.setCssClass(cls); return this; }

    @FunctionalInterface
    public interface LabelValueFunction {
        void apply(LabelValue val);
    }

    public static class LabelValue {
        public void set(Object value) {}
        public void set(Object value, Object extra) {}
    }
}
