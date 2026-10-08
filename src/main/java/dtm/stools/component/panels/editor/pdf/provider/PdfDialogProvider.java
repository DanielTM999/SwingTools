package dtm.stools.component.panels.editor.pdf.provider;

import java.awt.Component;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface PdfDialogProvider extends PdfProvider {
    Optional<String> input(Component parent, String title, String prompt);
    Optional<char[]> password(Component parent, String title, String prompt);
    boolean confirm(Component parent, String title, String message);
    void message(Component parent, String title, String message);
    default int choose(Component parent, String title, String message, String... options) {
        if (options.length == 0) return -1;
        return confirm(parent, title, message) ? 0 : -1;
    }
    default Optional<String> input(Component parent, String title, String prompt, String initial) {
        return input(parent, title, prompt);
    }
    default Optional<List<String>> editList(Component parent, String title, String prompt, List<String> values) {
        return input(parent, title, prompt + " (separe por vírgula)", String.join(", ", values))
                .map(text -> Arrays.stream(text.split(",")).map(String::strip).filter(value -> !value.isEmpty()).toList());
    }
}
