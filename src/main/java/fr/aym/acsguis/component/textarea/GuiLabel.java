// TODO port:1.20.1 stub - GuiLabel for text content
package fr.aym.acsguis.component.textarea;

import fr.aym.acsguis.component.GuiComponent;
import fr.aym.acsguis.event.listeners.mouse.IMouseMoveListener;

import java.util.List;
import java.util.function.IntConsumer;

public class GuiLabel extends GuiTextArea {
    public GuiLabel(String text) {
        setText(text);
    }

    @Override
    public GuiLabel setCssId(String id) { super.setCssId(id); return this; }

    @Override
    public GuiLabel setCssClass(String cls) { super.setCssClass(cls); return this; }

    @Override
    public GuiLabel setText(String text) { super.setText(text); return this; }

    @Override
    public GuiLabel addClickListener(ClickListener l) { super.addClickListener(l); return this; }

    @Override
    public GuiLabel addMoveListener(IMouseMoveListener l) { super.addMoveListener(l); return this; }

    @Override
    public GuiLabel addWheelListener(IntConsumer l) { super.addWheelListener(l); return this; }

    @Override
    public GuiLabel addTickListener(Runnable r) { super.addTickListener(r); return this; }

    @Override
    public GuiLabel setEnabled(boolean enabled) { super.setEnabled(enabled); return this; }

    @Override
    public GuiLabel setHoveringText(List<String> texts) { super.setHoveringText(texts); return this; }
}
