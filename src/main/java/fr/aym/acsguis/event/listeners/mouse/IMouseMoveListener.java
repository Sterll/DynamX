// TODO port:1.20.1 stub - mouse move/hover listener
package fr.aym.acsguis.event.listeners.mouse;

public interface IMouseMoveListener {
    void onMouseMoved(int mouseX, int mouseY);

    default void onMouseHover(int mouseX, int mouseY) {}

    default void onMouseUnhover(int mouseX, int mouseY) {}
}
