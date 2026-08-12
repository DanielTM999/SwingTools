package dtm.stools.component.panels.graphics.gl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphicsGlPanelPresentationModeTest {

    @Test
    void usesBufferedPresentationByDefault() {
        GraphicsGlPanel panel = new GraphicsGlPanel();

        assertEquals(GraphicsGlPresentationMode.BUFFERED, panel.getPresentationMode());
    }

    @Test
    void allowsDirectHeavyweightPresentation() {
        GraphicsGlPanel panel = new GraphicsGlPanel(GraphicsGlPresentationMode.HEAVYWEIGHT);

        assertEquals(GraphicsGlPresentationMode.HEAVYWEIGHT, panel.getPresentationMode());
    }

    @Test
    void supportsOnDemandRendering() {
        GraphicsGlPanel panel = new GraphicsGlPanel();

        panel.setRenderOnDemand(true);

        assertTrue(panel.isRenderOnDemand());
        panel.requestRender();
    }

    @Test
    void skipsUnchangedBufferedFramesByDefaultAndCanDisableIt() {
        GraphicsGlPanel panel = new GraphicsGlPanel();

        assertTrue(panel.isSkipUnchangedFrames());
        panel.setSkipUnchangedFrames(false);

        assertFalse(panel.isSkipUnchangedFrames());
    }
}
