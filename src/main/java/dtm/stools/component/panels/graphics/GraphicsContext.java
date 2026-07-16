package dtm.stools.component.panels.graphics;

public interface GraphicsContext {
    int getWidth();
    int getHeight();
    void runOnUiThread(Runnable task);
    GraphicsInput getInput();
}
