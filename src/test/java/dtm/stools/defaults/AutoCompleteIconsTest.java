package dtm.stools.defaults;

import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteItem;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AutoCompleteIconsTest {

    @Test
    void providesAResourceIconForEveryCompletionKind() {
        for (AutoCompleteItem.Kind kind : AutoCompleteItem.Kind.values()) {
            Icon icon = AutoCompleteIcons.forKind(kind);
            assertNotNull(icon, () -> "Missing icon for " + kind);
            assertEquals(16, icon.getIconWidth(), () -> "Unexpected icon width for " + kind);
            assertEquals(16, icon.getIconHeight(), () -> "Unexpected icon height for " + kind);
        }
    }

    @Test
    void nullKindFallsBackToTextIcon() {
        assertNotNull(AutoCompleteIcons.forKind(null));
    }
}
