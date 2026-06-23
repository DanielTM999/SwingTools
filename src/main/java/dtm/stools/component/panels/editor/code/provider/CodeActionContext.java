package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.api.Range;
import dtm.stools.component.panels.editor.code.diagnostics.Diagnostic;

import java.util.Collections;
import java.util.List;


public record CodeActionContext(
        String buffer,
        Range range,
        List<Diagnostic> diagnostics
) {
    public CodeActionContext {
        if (diagnostics == null) diagnostics = Collections.emptyList();
        else diagnostics = List.copyOf(diagnostics);
    }
}
