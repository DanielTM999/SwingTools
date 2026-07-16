package dtm.stools.component.panels.graphics;

public interface GraphicsInput {
    boolean isKeyDown(int keyCode);
    boolean isAnyKeyDown();
    boolean isMouseButtonDown(int button);
    int getMouseX();
    int getMouseY();
    boolean isMouseInside();
    double getWheelRotation();
}
