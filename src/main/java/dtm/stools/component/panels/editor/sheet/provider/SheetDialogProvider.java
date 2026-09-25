package dtm.stools.component.panels.editor.sheet.provider;

import java.util.Optional;

public interface SheetDialogProvider extends SheetProvider {
    <T> Optional<T> show(SheetDialogRequest<T> request);
}
