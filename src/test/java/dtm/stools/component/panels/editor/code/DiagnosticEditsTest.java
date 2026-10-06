package dtm.stools.component.panels.editor.code;
import dtm.stools.component.panels.editor.code.diagnostics.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class DiagnosticEditsTest {
    @Test void retainsAndMovesUneditedErrors() {
        var errors = List.of(new Diagnostic(0, 0, 3, DiagnosticSeverity.ERROR, "one"),
                new Diagnostic(1, 0, 3, DiagnosticSeverity.ERROR, "two"));
        var shifted = DiagnosticEdits.rebase(errors, "bad\nbad", "okay\nbad");
        assertEquals(1, shifted.size()); assertEquals("two", shifted.getFirst().message());
        assertEquals(1, shifted.getFirst().startLine());
        shifted = DiagnosticEdits.rebase(shifted, "okay\nbad", "\nokay\nbad");
        assertEquals(2, shifted.getFirst().startLine());
    }
    @Test void insertionInsideErrorRemovesOnlyThatError() {
        var errors = List.of(new Diagnostic(0, 0, 3, DiagnosticSeverity.ERROR, "one"),
                new Diagnostic(0, 4, 7, DiagnosticSeverity.ERROR, "two"));
        var shifted = DiagnosticEdits.rebase(errors, "bad bad", "bXad bad");
        assertEquals(1, shifted.size()); assertEquals(5, shifted.getFirst().startCol());
    }
}
