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

    default GhostTextSuggestion getGhostSuggestion(GhostTextContext context) {
        return GhostTextSuggestion.of(getGhostText(context));
    }

    default CompletableFuture<GhostTextSuggestion> getGhostSuggestionAsync(GhostTextContext context,
                                                                         Executor executor) {
        return CompletableFuture.supplyAsync(() -> getGhostSuggestion(context), executor);
    }
}
