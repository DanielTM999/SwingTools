package dtm.stools.component.panels.editor.code.ghost;

import dtm.stools.component.panels.editor.code.provider.CodeEditorProvider;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@FunctionalInterface
public interface GhostTextProvider extends CodeEditorProvider {

    String getGhostText(GhostTextContext context);

    default CompletableFuture<String> getGhostTextAsync(GhostTextContext context, Executor executor) {
        return CompletableFuture.supplyAsync(() -> getGhostText(context), executor);
    }
}
