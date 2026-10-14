package dtm.stools.component.inputfields.textarea.diagnostic;

import java.util.List;

@FunctionalInterface
public interface TextDiagnosticProvider {

    List<TextDiagnostic> diagnose(String text);
}
