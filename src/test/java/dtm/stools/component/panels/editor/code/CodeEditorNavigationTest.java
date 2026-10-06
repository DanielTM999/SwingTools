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
    @Test void fileRemainsVisibleAndSegmentsNavigate() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CodeEditor editor = new CodeEditor("class A {\n void run() {}\n}");
            var file = java.nio.file.Path.of("src", "A.java").toAbsolutePath();
            var range = new Range(new Position(1, 1), new Position(1, 14));
            editor.setBreadcrumbSymbols(file, List.of(DocumentSymbol.leaf("run", SymbolKind.METHOD, range)));
            JPanel header = (JPanel) ((BorderLayout) editor.getLayout()).getLayoutComponent(BorderLayout.NORTH);
            JLabel label = (JLabel) header.getComponent(header.getComponentCount() - 2);
            label.setSize(500, 25);
            assertEquals("A.java \u203a run", label.getText());
            var fileEvent = new MouseEvent(label, MouseEvent.MOUSE_CLICKED, 0, 0, 10, 5, 1, false);
            assertEquals(file.toString(), label.getToolTipText(fileEvent));
            int methodX = 9 + label.getFontMetrics(label.getFont()).stringWidth("A.java \u203a ");
            label.dispatchEvent(new MouseEvent(label, MouseEvent.MOUSE_CLICKED, 0, 0, methodX, 5, 1, false));
            assertEquals(1, editor.getCaretLine());
            label.dispatchEvent(fileEvent);
            assertEquals(0, editor.getCaretLine());
            int height = header.getPreferredSize().height;
            editor.setBreadcrumbSymbols(file, List.of());
            assertEquals("A.java", label.getText());
            assertEquals(height, header.getPreferredSize().height);
            assertEquals(file.toString(), label.getToolTipText(fileEvent));
        });
    }

    @Test void clippedSymbolsCannotBeClicked() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CodeEditor editor = new CodeEditor("class A {\n void run() {}\n}");
            var range = new Range(new Position(1, 1), new Position(1, 14));
            editor.setBreadcrumbSymbols(java.nio.file.Path.of("LongFileName.java"),
                    List.of(DocumentSymbol.leaf("run", SymbolKind.METHOD, range)));
            JPanel header = (JPanel) ((BorderLayout) editor.getLayout()).getLayoutComponent(BorderLayout.NORTH);
            JLabel label = (JLabel) header.getComponent(header.getComponentCount() - 2);
            label.setSize(60, 25);
            editor.setCaretPosition(2, 0);
            label.dispatchEvent(new MouseEvent(label, MouseEvent.MOUSE_CLICKED, 0, 0, 55, 5, 1, false));
            assertEquals(2, editor.getCaretLine());
            assertTrue(label.getToolTipText().contains("run"));
        });
    }
}
