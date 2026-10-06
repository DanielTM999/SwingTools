package dtm.stools.component.panels.editor.code;
import dtm.stools.component.panels.editor.code.prototype.folding.FoldRange;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class CodeEditorImportFoldingTest {
    @Test void openedImportsDoNotCollapseAfterRangesDisappearAndReturn() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var editor = new CodeEditorTextArea("import a.A;\nimport b.B;\n\nclass C {}\n");
            editor.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 14));
            editor.setCaretPosition(3, 0);
            editor.setFoldingEnabled(true);
            editor.setFoldRanges(List.of(new FoldRange(0, 1, "imports", true)));
            assertTrue(editor.getFoldRegions().getFirst().folded());
            editor.toggleFold(0);
            assertFalse(editor.getFoldRegions().getFirst().folded());
            editor.setFoldRanges(List.of());
            editor.setFoldRanges(List.of(new FoldRange(0, 1, "imports", true)));
            assertFalse(editor.getFoldRegions().getFirst().folded());
        });
    }
}
