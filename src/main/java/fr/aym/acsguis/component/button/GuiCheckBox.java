// TODO port:1.20.1 stub - GuiCheckBox component
package fr.aym.acsguis.component.button;

import fr.aym.acsguis.component.GuiComponent;
import fr.aym.acsguis.event.listeners.mouse.IMouseMoveListener;

import java.util.List;
import java.util.function.IntConsumer;

public class GuiCheckBox extends GuiComponent {
    private boolean checked;

    public GuiCheckBox(String label) {}

    public boolean isChecked() { return checked; }
    public GuiCheckBox setChecked(boolean checked) { this.checked = checked; return this; }
    public GuiCheckBox setCheckedSymbol(String s) { return this; }
    public GuiCheckBox setHoveringText(List<String> texts) { return this; }

    @Override
    public GuiCheckBox setCssId(String id) { super.setCssId(id); return this; }

    @Override
    public GuiCheckBox setCssClass(String cls) { super.setCssClass(cls); return this; }

    @Override
    public GuiCheckBox addClickListener(ClickListener l) { super.addClickListener(l); return this; }

    @Override
    public GuiCheckBox addMoveListener(IMouseMoveListener l) { super.addMoveListener(l); return this; }
}
