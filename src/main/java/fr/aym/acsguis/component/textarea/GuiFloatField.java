// TODO port:1.20.1 stub - GuiFloatField numeric input
package fr.aym.acsguis.component.textarea;

public class GuiFloatField extends GuiTextArea {
    private final float min;
    private final float max;
    private float value;

    public GuiFloatField(float min, float max) {
        this.min = min;
        this.max = max;
    }

    public float getMin() { return min; }
    public float getMax() { return max; }
    public float getValue() { return value; }
    public GuiFloatField setValue(float v) { this.value = v; return this; }

    @Override
    public GuiFloatField setCssId(String id) { super.setCssId(id); return this; }

    @Override
    public GuiFloatField setCssClass(String cls) { super.setCssClass(cls); return this; }
}
