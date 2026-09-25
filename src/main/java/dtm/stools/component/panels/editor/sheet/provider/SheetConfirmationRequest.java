package dtm.stools.component.panels.editor.sheet.provider;

import java.awt.Component;
import java.util.List;
import java.util.Objects;

public record SheetConfirmationRequest(Component owner, String title, String message, List<String> options, int defaultOption, boolean warning) {
    public SheetConfirmationRequest { Objects.requireNonNull(owner); options = List.copyOf(options); }

    public static SheetConfirmationRequest yesNo(Component owner, String title, String message) { return new SheetConfirmationRequest(owner, title, message, List.of("Sim", "Não"), 0, false); }
    public static SheetConfirmationRequest info(Component owner, String title, String message) { return new SheetConfirmationRequest(owner, title, message, List.of("OK"), 0, false); }
    public static SheetConfirmationRequest warn(Component owner, String title, String message) { return new SheetConfirmationRequest(owner, title, message, List.of("OK"), 0, true); }
}
