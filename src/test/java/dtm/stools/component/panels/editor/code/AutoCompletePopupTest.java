package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopup;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoCompletePopupTest {

    @Test
    void listTracksViewportWidthSoLongLspDetailsDoNotClipKindBadge() {
        TestPopup popup = new TestPopup();

        assertTrue(popup.listTracksViewportWidth());
    }

    private static final class TestPopup extends AutoCompletePopup {
        private TestPopup() {
            super(new JPanel());
        }

        private boolean listTracksViewportWidth() {
            return list.getScrollableTracksViewportWidth();
        }
    }
}
