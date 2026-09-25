package dtm.stools.component.panels.editor.sheet.provider;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;

public record SheetAiRequest(Kind kind, String instruction, String sheet, String range, List<List<String>> data, String formula, Locale locale, BooleanSupplier cancelled) {
    public enum Kind { SUGGEST_FORMULA, EXPLAIN_FORMULA, ANALYZE_DATA, FREE_FORM }

    public SheetAiRequest {
        Objects.requireNonNull(kind);
        instruction = Objects.requireNonNullElse(instruction, "");
        data = data == null ? List.of() : data.stream().map(List::copyOf).toList();
        cancelled = cancelled == null ? () -> false : cancelled;
    }
}
