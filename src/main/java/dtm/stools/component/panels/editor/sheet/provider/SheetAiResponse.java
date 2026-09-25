package dtm.stools.component.panels.editor.sheet.provider;

import java.util.Objects;

public record SheetAiResponse(String text, String formula, String targetCell) {
    public SheetAiResponse { text = Objects.requireNonNullElse(text, ""); }
}
