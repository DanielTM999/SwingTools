package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.provider.SheetDialogProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetDialogRequest;

import java.util.Optional;

public final class DefaultDialogProvider implements SheetDialogProvider {
    @Override public String id() { return "sheet.popup.dialog.default"; }

    @Override
    public <T> Optional<T> show(SheetDialogRequest<T> request) {
        SheetDialogActivity<T> activity = new SheetDialogActivity<>(request);
        if (!request.modal()) { activity.open(); return Optional.empty(); }
        try { return activity.showResult(); } finally { activity.dispose(); }
    }
}
