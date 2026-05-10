// TODO port:1.20.1 stub - GuiPanel extends GuiComponent (container)
package fr.aym.acsguis.component.panel;

import fr.aym.acsguis.component.GuiComponent;
import fr.aym.acsguis.event.listeners.mouse.IMouseMoveListener;

import java.util.function.IntConsumer;

public class GuiPanel extends GuiComponent {
    public GuiPanel setLayout(Object layout) { return this; }

    @Override
    public GuiPanel setCssId(String id) { super.setCssId(id); return this; }

    @Override
    public GuiPanel setCssClass(String cls) { super.setCssClass(cls); return this; }

    @Override
    public GuiPanel setFocused(boolean focused) { super.setFocused(focused); return this; }

    @Override
    public GuiPanel setEnabled(boolean enabled) { super.setEnabled(enabled); return this; }

    @Override
    public GuiPanel setVisible(boolean visible) { super.setVisible(visible); return this; }

    @Override
    public GuiPanel addClickListener(ClickListener l) { super.addClickListener(l); return this; }

    @Override
    public GuiPanel addMoveListener(IMouseMoveListener l) { super.addMoveListener(l); return this; }

    @Override
    public GuiPanel addWheelListener(IntConsumer l) { super.addWheelListener(l); return this; }

    @Override
    public GuiPanel addTickListener(Runnable r) { super.addTickListener(r); return this; }
}
