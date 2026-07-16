package dtm.stools.component.panels.graphics;

import java.awt.*;

public interface GraphicsHost<C extends GraphicsContext> {
    Component getComponent();
    void setRenderer(GraphicsRender<C> renderer);
    void dispose();
}
