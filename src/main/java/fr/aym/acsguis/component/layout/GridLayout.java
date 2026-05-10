// TODO port:1.20.1 stub - GridLayout helper
package fr.aym.acsguis.component.layout;

import fr.aym.acsguis.cssengine.positionning.Size;

public class GridLayout {
    public enum GridDirection { HORIZONTAL, VERTICAL }

    public GridLayout(Size.SizeValue a, Size.SizeValue b, Size.SizeValue c, GridDirection direction, int gap) {}
    public GridLayout(int rowH, int colW, int gap) {}

    public static GridLayout columnLayout(int rowH, int columns) {
        return new GridLayout(rowH, columns, 0);
    }
}
