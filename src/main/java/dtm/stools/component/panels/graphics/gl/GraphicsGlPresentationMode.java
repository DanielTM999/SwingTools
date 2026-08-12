package dtm.stools.component.panels.graphics.gl;

/**
 * Defines how a {@link GraphicsGlPanel} presents its OpenGL frames.
 */
public enum GraphicsGlPresentationMode {

    /**
     * Reads the rendered frame into a {@code BufferedImage} and paints it through
     * a lightweight Swing component. This is the default and avoids the usual
     * heavyweight {@code Canvas} composition issues.
     */
    BUFFERED,

    /**
     * Presents OpenGL directly on an AWT {@code Canvas}. This has no readback
     * overhead, but retains heavyweight component behavior.
     */
    HEAVYWEIGHT
}
