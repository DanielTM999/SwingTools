package dtm.stools.component.panels.graphics.gl;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

final class GraphicsGlRenderContextRegistry {

    private static final Map<GraphicsGlRender, GraphicsGlContext> CONTEXTS = Collections.synchronizedMap(new WeakHashMap<>());

    private GraphicsGlRenderContextRegistry() {
    }

    static void bind(GraphicsGlRender renderer, GraphicsGlContext context) {
        if (renderer != null && context != null) {
            CONTEXTS.put(renderer, context);
        }
    }

    static void unbind(GraphicsGlRender renderer) {
        if (renderer != null) {
            CONTEXTS.remove(renderer);
        }
    }

    static void runOnUiThread(GraphicsGlRender renderer, Runnable task) {
        if (task == null) return;

        GraphicsGlContext context = CONTEXTS.get(renderer);
        if (context == null) {
            throw new IllegalStateException("GraphicsGlRender is not attached to a GraphicsGlContext");
        }
        context.runOnUiThread(task);
    }
}
