package dtm.stools.component.panels.graphics;

public interface GraphicsRender<C extends GraphicsContext> {
    default void initialize(C context) {}
    default void render(C context) {}
    default void resize(C context, int width, int height) {}
    default void dispose(C context) {}
}
