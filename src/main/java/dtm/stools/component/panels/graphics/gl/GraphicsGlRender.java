package dtm.stools.component.panels.graphics.gl;

import dtm.stools.component.panels.graphics.GraphicsRender;

public interface GraphicsGlRender extends GraphicsRender<GraphicsGlContext> {
    default void runOnUi(Runnable task) {
        runOnUiThread(task);
    }

    default void runOnUiThread(Runnable task) {
        GraphicsGlRenderContextRegistry.runOnUiThread(this, task);
    }
}
