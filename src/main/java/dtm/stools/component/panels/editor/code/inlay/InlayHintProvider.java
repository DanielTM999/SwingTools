package dtm.stools.component.panels.editor.code.inlay;

import dtm.stools.component.panels.editor.code.provider.CodeEditorProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@FunctionalInterface
public interface InlayHintProvider extends CodeEditorProvider {

    List<InlayHint> getInlayHints(InlayHintContext context);

    default CompletableFuture<List<InlayHint>> getInlayHintsAsync(InlayHintContext context, Executor executor) {
        return CompletableFuture.supplyAsync(() -> getInlayHints(context), executor);
    }
}
