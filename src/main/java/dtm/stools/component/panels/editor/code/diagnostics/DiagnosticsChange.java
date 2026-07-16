package dtm.stools.component.panels.editor.code.diagnostics;

import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

import java.util.List;

public record DiagnosticsChange(
        TextBuffer buffer,
        String oldText,
        String newText,
        int changeOffset,
        int removedLength,
        String insertedText,
        List<Diagnostic> previousDiagnostics
) {
}
