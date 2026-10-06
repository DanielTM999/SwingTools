package dtm.stools.component.panels.editor.code;
import dtm.stools.component.panels.editor.code.api.*;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class CodeEditorNavigationTest {
    @Test void stableHeaderAndClickableSymbols() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CodeEditor editor = new CodeEditor();
            editor.setText("class A {\n void run() {}\n}");
            var range = new Range(new Position(1, 1), new Position(1, 14));
            editor.setBreadcrumbSymbols(List.of(DocumentSymbol.leaf("run", SymbolKind.METHOD, range)));
            JPanel header = (JPanel) ((BorderLayout) editor.getLayout()).getLayoutComponent(BorderLayout.NORTH);
            JLabel label = (JLabel) header.getComponent(header.getComponentCount() - 2);
            int height = header.getPreferredSize().height;
            assertTrue(label.isVisible());
            label.dispatchEvent(new MouseEvent(label, MouseEvent.MOUSE_CLICKED, 0, 0, 10, 5, 1, false));
            assertEquals(1, editor.getCaretLine());
            editor.setBreadcrumbSymbols(List.of());
            assertEquals(height, header.getPreferredSize().height);
            assertFalse(header.getComponent(header.getComponentCount() - 1).isVisible());
        });
    }
}
