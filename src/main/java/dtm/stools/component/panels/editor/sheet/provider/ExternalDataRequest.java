package dtm.stools.component.panels.editor.sheet.provider;

import java.util.List;
import java.util.Objects;

public record ExternalDataRequest(String function, List<String> arguments, String sheet, String cell) {
    public ExternalDataRequest { Objects.requireNonNull(function); arguments = List.copyOf(arguments); }
}
