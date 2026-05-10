// TODO port:1.20.1 stub - GuiTextArea base text component
package fr.aym.acsguis.component.textarea;

import fr.aym.acsguis.component.GuiComponent;
import fr.aym.acsguis.event.listeners.mouse.IMouseMoveListener;

import java.util.List;
import java.util.function.IntConsumer;

public class GuiTextArea extends GuiComponent {
    protected String text = "";

    public GuiTextArea() {}

    public GuiTextArea setText(String text) { this.text = text; return this; }
    public String getText() { return text; }

    public GuiTextArea setMaxTextLength(int len) { return this; }
    public GuiTextArea setEditable(boolean editable) { return this; }
    public GuiTextArea setHoveringText(List<String> texts) { return this; }

    @Override
    public GuiTextArea setCssId(String id) { super.setCssId(id); return this; }

    @Override
    public GuiTextArea setCssClass(String cls) { super.setCssClass(cls); return this; }

    @Override
    public GuiTextArea setFocused(boolean focused) { super.setFocused(focused); return this; }

    @Override
    public GuiTextArea setEnabled(boolean enabled) { super.setEnabled(enabled); return this; }

    @Override
    public GuiTextArea setVisible(boolean visible) { super.setVisible(visible); return this; }

    @Override
    public GuiTextArea addClickListener(ClickListener l) { super.addClickListener(l); return this; }

    @Override
    public GuiTextArea addMoveListener(IMouseMoveListener l) { super.addMoveListener(l); return this; }

    @Override
    public GuiTextArea addWheelListener(IntConsumer l) { super.addWheelListener(l); return this; }

    @Override
    public GuiTextArea addTickListener(Runnable r) { super.addTickListener(r); return this; }
}
